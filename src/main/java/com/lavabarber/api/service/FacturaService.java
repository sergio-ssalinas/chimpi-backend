package com.lavabarber.api.service;

import com.lavabarber.api.dto.backoffice.FacturaLineaResponse;
import com.lavabarber.api.dto.backoffice.FacturaResponse;
import com.lavabarber.api.dto.backoffice.GenerarFacturaRequest;
import com.lavabarber.api.entity.*;
import com.lavabarber.api.entity.enums.EstadoFactura;
import com.lavabarber.api.entity.enums.EstadoVenta;
import com.lavabarber.api.entity.enums.TasaIva;
import com.lavabarber.api.entity.enums.TipoItemVenta;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.ClienteRepository;
import com.lavabarber.api.repository.FacturaRepository;
import com.lavabarber.api.repository.NegocioRepository;
import com.lavabarber.api.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Genera facturas a partir de una Venta: discrimina IVA (10%/5%/Exenta) segun la tasa de cada
 * producto/servicio vendido, numera con el formato paraguayo (puntoExpedicion-numero) y guarda los
 * renglones como foto inmutable (FacturaLinea). Un combo se cobra al precio del combo, pero en la
 * factura sale desglosado en sus componentes, con el descuento repartido entre ellos.
 *
 * Solo ADMIN puede facturar/consultar cualquier venta; EMPLEADO y CAJERO unicamente las de su propia
 * caja (o las que ellos mismos registraron).
 *
 * Ver EstadoFactura y la clase Factura para el porque del "modo local".
 */
@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final VentaRepository ventaRepository;
    private final NegocioRepository negocioRepository;
    private final ClienteRepository clienteRepository;

    @Transactional
    public FacturaResponse generar(UUID ventaId, GenerarFacturaRequest request, UUID usuarioInternoId, boolean esAdmin) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + ventaId));
        verificarAcceso(venta, usuarioInternoId, esAdmin);

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new BusinessException("No se puede facturar una venta anulada");
        }
        facturaRepository.findByVentaId(ventaId).ifPresent(f -> {
            throw new BusinessException("Esta venta ya tiene una factura generada (" + f.getNumeroFactura() + ")");
        });

        Negocio negocio = venta.getNegocio();

        // Renglones (con los combos desglosados) y, a partir de ellos, la discriminacion de IVA.
        // Los precios ya incluyen el IVA (practica habitual en Paraguay): la base gravada se calcula
        // "hacia atras" sobre el importe neto de cada renglon, ya descontadas las promociones.
        List<FacturaLinea> lineas = construirLineas(venta);

        BigDecimal base10 = BigDecimal.ZERO, iva10 = BigDecimal.ZERO;
        BigDecimal base5 = BigDecimal.ZERO, iva5 = BigDecimal.ZERO;
        BigDecimal exenta = BigDecimal.ZERO;

        for (FacturaLinea linea : lineas) {
            BigDecimal importe = linea.getImporte();
            switch (linea.getTasaIva()) {
                case DIEZ -> {
                    BigDecimal base = importe.divide(new BigDecimal("1.10"), 0, RoundingMode.HALF_UP);
                    base10 = base10.add(base);
                    iva10 = iva10.add(importe.subtract(base));
                }
                case CINCO -> {
                    BigDecimal base = importe.divide(new BigDecimal("1.05"), 0, RoundingMode.HALF_UP);
                    base5 = base5.add(base);
                    iva5 = iva5.add(importe.subtract(base));
                }
                case EXENTA -> exenta = exenta.add(importe);
            }
        }

        // Datos del cliente en la factura (ver GenerarFacturaRequest).
        Cliente cliente = null;
        if (request.clienteId() != null) {
            cliente = clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.clienteId()));
        } else {
            cliente = venta.getCliente();
        }
        boolean consumidorFinal = Boolean.TRUE.equals(request.consumidorFinal());
        boolean usarCliente = cliente != null && !consumidorFinal;

        String clienteNombre;
        if (noVacio(request.clienteNombre())) {
            clienteNombre = request.clienteNombre().trim();
        } else if (usarCliente) {
            clienteNombre = (cliente.getNombre() + " " + cliente.getApellido()).trim();
        } else {
            clienteNombre = "Consumidor Final";
        }

        String clienteRuc;
        if (noVacio(request.clienteRuc())) {
            clienteRuc = ClienteService.normalizarRuc(request.clienteRuc());
        } else if (usarCliente) {
            clienteRuc = ClienteService.documentoParaFactura(cliente);
        } else {
            clienteRuc = null;
        }

        Factura factura = Factura.builder()
                .venta(venta).negocio(negocio)
                .numeroFactura(generarNumeroFactura(negocio))
                .fechaEmision(LocalDateTime.now())
                .clienteRuc(clienteRuc)
                .clienteNombre(clienteNombre)
                .condicionVenta(request.condicionVenta())
                .baseGravada10(base10).iva10(iva10)
                .baseGravada5(base5).iva5(iva5)
                .exenta(exenta)
                .total(venta.getTotal())
                .estado(EstadoFactura.LOCAL)
                .build();

        int orden = 1;
        for (FacturaLinea linea : lineas) {
            linea.setFactura(factura);
            linea.setOrden(orden++);
            factura.getLineas().add(linea);
        }

        return toResponse(facturaRepository.save(factura));
    }

    // ---------------------------------------------------------------- renglones

    private List<FacturaLinea> construirLineas(Venta venta) {
        List<FacturaLinea> lineas = new ArrayList<>();
        for (VentaItem item : venta.getItems()) {
            BigDecimal promo = item.getDescuento() != null ? item.getDescuento() : BigDecimal.ZERO;
            if (item.getTipo() == TipoItemVenta.SERVICIO && item.getServicio().esCombo()) {
                lineas.addAll(desglosarCombo(item, promo));
            } else {
                TasaIva tasa = item.getTipo() == TipoItemVenta.PRODUCTO
                        ? item.getProducto().getTasaIva()
                        : item.getServicio().getTasaIva();
                lineas.add(FacturaLinea.builder()
                        .descripcion(item.getDescripcion())
                        .cantidad(item.getCantidad())
                        .precioUnitario(item.getPrecioUnitario())
                        .descuento(promo)
                        .importe(item.getSubtotal().subtract(promo))
                        .tasaIva(tasa)
                        .build());
            }
        }
        return lineas;
    }

    /**
     * Desglosa un combo en sus servicios componentes sin cambiar lo cobrado: la suma de los importes
     * netos de los renglones es exactamente (precio del combo - descuento de promocion).
     *
     * - Cada componente sale a su precio de lista.
     * - Si la suma de las listas supera el precio del combo, esa diferencia es el "descuento del
     *   combo" y se reparte entre los componentes en proporcion a su precio de lista.
     * - La promocion que haya sobre el combo se reparte con el mismo criterio.
     * - Si el combo cuesta igual o mas que la suma de sus partes, no hay descuento de combo: el precio
     *   cobrado se reparte proporcionalmente entre los componentes.
     * Cada componente lleva su propia tasa de IVA. El redondeo es a guaranies enteros y el ultimo
     * componente absorbe la diferencia, para que los totales cierren al guarani.
     */
    private List<FacturaLinea> desglosarCombo(VentaItem item, BigDecimal promo) {
        Servicio combo = item.getServicio();
        List<Servicio> componentes = combo.getComponentes().stream()
                .sorted(Comparator.comparing(Servicio::getNombre))
                .toList();

        BigDecimal cantidad = item.getCantidad();
        BigDecimal cobradoBruto = item.getSubtotal();

        BigDecimal[] lista = new BigDecimal[componentes.size()];
        BigDecimal sumaLista = BigDecimal.ZERO;
        for (int i = 0; i < componentes.size(); i++) {
            BigDecimal precio = componentes.get(i).getPrecio() != null ? componentes.get(i).getPrecio() : BigDecimal.ZERO;
            lista[i] = precio.multiply(cantidad);
            sumaLista = sumaLista.add(lista[i]);
        }

        BigDecimal descuentoCombo = sumaLista.compareTo(cobradoBruto) > 0 ? sumaLista.subtract(cobradoBruto) : BigDecimal.ZERO;
        boolean hayDescuentoCombo = descuentoCombo.signum() > 0;

        BigDecimal[] brutoPorComponente = hayDescuentoCombo ? lista : repartir(cobradoBruto, lista);
        BigDecimal[] descComboPorComponente = repartir(descuentoCombo, lista);
        BigDecimal[] descPromoPorComponente = repartir(promo, lista);

        List<FacturaLinea> lineas = new ArrayList<>();
        for (int i = 0; i < componentes.size(); i++) {
            Servicio comp = componentes.get(i);
            BigDecimal descuento = descComboPorComponente[i].add(descPromoPorComponente[i]);
            BigDecimal precioUnitario = hayDescuentoCombo && comp.getPrecio() != null
                    ? comp.getPrecio()
                    : brutoPorComponente[i].divide(cantidad, 0, RoundingMode.HALF_UP);
            lineas.add(FacturaLinea.builder()
                    .descripcion(comp.getNombre() + " (" + combo.getNombre() + ")")
                    .cantidad(cantidad)
                    .precioUnitario(precioUnitario)
                    .descuento(descuento)
                    .importe(brutoPorComponente[i].subtract(descuento))
                    .tasaIva(comp.getTasaIva())
                    .build());
        }
        return lineas;
    }

    /**
     * Reparte "total" en partes enteras proporcionales a los pesos. El ultimo recibe el resto, asi la
     * suma da exactamente "total". Si todos los pesos son 0 se reparte en partes iguales.
     */
    private BigDecimal[] repartir(BigDecimal total, BigDecimal[] pesos) {
        int n = pesos.length;
        BigDecimal[] partes = new BigDecimal[n];
        BigDecimal sumaPesos = BigDecimal.ZERO;
        for (BigDecimal p : pesos) sumaPesos = sumaPesos.add(p);
        boolean iguales = sumaPesos.signum() == 0;
        if (iguales) sumaPesos = BigDecimal.valueOf(n);

        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            if (i == n - 1) {
                partes[i] = total.subtract(acumulado);
            } else {
                BigDecimal peso = iguales ? BigDecimal.ONE : pesos[i];
                partes[i] = total.multiply(peso).divide(sumaPesos, 0, RoundingMode.HALF_UP);
                acumulado = acumulado.add(partes[i]);
            }
        }
        return partes;
    }

    // ---------------------------------------------------------------- resto

    private String generarNumeroFactura(Negocio negocio) {
        String puntoExp = negocio.getPuntoExpedicion() != null ? negocio.getPuntoExpedicion() : "001-001";
        int numero = negocio.getProximoNumeroFactura() != null ? negocio.getProximoNumeroFactura() : 1;

        negocio.setProximoNumeroFactura(numero + 1);
        negocioRepository.save(negocio);

        return puntoExp + "-" + String.format("%07d", numero);
    }

    @Transactional
    public FacturaResponse anular(UUID facturaId) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada: " + facturaId));
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new BusinessException("Esta factura ya esta anulada");
        }
        factura.setEstado(EstadoFactura.ANULADA);
        return toResponse(facturaRepository.save(factura));
    }

    @Transactional(readOnly = true)
    public List<FacturaResponse> listar(UUID negocioId) {
        return facturaRepository.findByNegocioIdOrderByFechaEmisionDesc(negocioId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FacturaResponse obtenerPorVenta(UUID ventaId, UUID usuarioInternoId, boolean esAdmin) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + ventaId));
        verificarAcceso(venta, usuarioInternoId, esAdmin);

        return facturaRepository.findByVentaId(ventaId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Esta venta todavia no tiene factura"));
    }

    /** ADMIN accede a todo; los demas solo a ventas de su propia caja o registradas por ellos. */
    private void verificarAcceso(Venta venta, UUID usuarioInternoId, boolean esAdmin) {
        if (esAdmin) return;
        boolean esSuya = venta.getUsuarioInterno().getId().equals(usuarioInternoId)
                || venta.getCaja().getUsuarioInterno().getId().equals(usuarioInternoId);
        if (!esSuya) {
            throw new AccessDeniedException("No tenes permiso sobre esta venta");
        }
    }

    private static boolean noVacio(String s) {
        return s != null && !s.isBlank();
    }

    private FacturaResponse toResponse(Factura f) {
        Negocio n = f.getNegocio();
        // Sin RUC y timbrado reales del negocio, el comprobante es interno y asi se rotula en el PDF.
        boolean sinValidezFiscal = !noVacio(n.getRuc()) || !noVacio(n.getTimbradoNumero());

        List<FacturaLineaResponse> lineas = f.getLineas().stream()
                .map(l -> new FacturaLineaResponse(
                        l.getDescripcion(), l.getCantidad(), l.getPrecioUnitario(), l.getDescuento(),
                        l.getImporte(), l.getTasaIva().name()))
                .toList();

        return new FacturaResponse(
                f.getId(), f.getVenta().getId(), n.getId(), f.getNumeroFactura(), f.getFechaEmision(),
                f.getClienteRuc(), f.getClienteNombre(), f.getCondicionVenta().name(),
                f.getBaseGravada10(), f.getIva10(), f.getBaseGravada5(), f.getIva5(), f.getExenta(), f.getTotal(),
                f.getEstado().name(), f.getCdc(), sinValidezFiscal, lineas,
                n.getNombre(), n.getRuc(), n.getDireccion(), n.getTelefono(), n.getTimbradoNumero());
    }
}
