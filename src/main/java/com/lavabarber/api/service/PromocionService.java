package com.lavabarber.api.service;

import com.lavabarber.api.dto.backoffice.PromocionRequest;
import com.lavabarber.api.dto.backoffice.PromocionResponse;
import com.lavabarber.api.entity.*;
import com.lavabarber.api.entity.enums.AlcancePromocion;
import com.lavabarber.api.entity.enums.TipoPromocion;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Mantenimiento de promociones y calculo del descuento de una venta.
 * El descuento se calcula SIEMPRE en el backend, item por item, y queda
 * guardado en VentaItem.descuento: asi la factura puede discriminar el
 * IVA exacto de cada linea sin prorratear.
 */
@Service
@RequiredArgsConstructor
public class PromocionService {

    private final PromocionRepository promocionRepository;
    private final NegocioRepository negocioRepository;
    private final ProductoRepository productoRepository;
    private final ServicioRepository servicioRepository;
    private final ClienteRepository clienteRepository;

    // ---------- mantenimiento ----------

    @Transactional(readOnly = true)
    public List<PromocionResponse> listar(UUID negocioId) {
        return promocionRepository.findByNegocioId(negocioId).stream().map(this::toResponse).toList();
    }

    /**
     * Las que se pueden usar hoy: las generales y, si se indica clienteId,
     * tambien las exclusivas de ese cliente. Las de otros clientes nunca salen.
     */
    @Transactional(readOnly = true)
    public List<PromocionResponse> listarVigentes(UUID negocioId, UUID clienteId) {
        return promocionRepository.findByNegocioId(negocioId).stream()
                .filter(this::estaVigente)
                .filter(p -> p.getCliente() == null
                        || (clienteId != null && p.getCliente().getId().equals(clienteId)))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PromocionResponse crear(PromocionRequest request) {
        Promocion promocion = new Promocion();
        aplicarDatos(promocion, request);
        return toResponse(promocionRepository.save(promocion));
    }

    @Transactional
    public PromocionResponse actualizar(UUID id, PromocionRequest request) {
        Promocion promocion = obtener(id);
        aplicarDatos(promocion, request);
        return toResponse(promocionRepository.save(promocion));
    }

    /** Soft delete: las ventas historicas la referencian. */
    @Transactional
    public PromocionResponse desactivar(UUID id) {
        Promocion promocion = obtener(id);
        promocion.setActiva(false);
        return toResponse(promocionRepository.save(promocion));
    }

    public Promocion obtener(UUID id) {
        return promocionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promocion no encontrada: " + id));
    }

    /** Devuelve la promocion solo si se puede usar hoy en ese negocio (y para ese cliente, si es exclusiva). */
    public Promocion obtenerAplicable(UUID id, UUID negocioId, UUID clienteId) {
        Promocion p = obtener(id);
        if (!p.getNegocio().getId().equals(negocioId)) {
            throw new BusinessException("La promocion no pertenece a este negocio");
        }
        if (!estaVigente(p)) {
            throw new BusinessException("La promocion \"" + p.getNombre() + "\" no esta vigente");
        }
        if (p.getCliente() != null && !p.getCliente().getId().equals(clienteId)) {
            throw new BusinessException("La promocion \"" + p.getNombre() + "\" es exclusiva de un cliente: elegi ese cliente en la venta");
        }
        return p;
    }

    private boolean estaVigente(Promocion p) {
        LocalDate hoy = LocalDate.now();
        return p.isActiva()
                && (p.getFechaDesde() == null || !hoy.isBefore(p.getFechaDesde()))
                && (p.getFechaHasta() == null || !hoy.isAfter(p.getFechaHasta()));
    }

    private void aplicarDatos(Promocion p, PromocionRequest r) {
        Negocio negocio = negocioRepository.findById(r.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + r.negocioId()));

        if (r.fechaDesde() != null && r.fechaHasta() != null && r.fechaHasta().isBefore(r.fechaDesde())) {
            throw new BusinessException("La fecha 'hasta' no puede ser anterior a 'desde'");
        }

        BigDecimal valor = r.valor();
        switch (r.tipo()) {
            case PORCENTAJE -> {
                if (valor == null || valor.signum() <= 0 || valor.compareTo(new BigDecimal("100")) > 0) {
                    throw new BusinessException("El porcentaje debe ser mayor a 0 y hasta 100");
                }
            }
            case MONTO_FIJO -> {
                if (valor == null || valor.signum() <= 0) {
                    throw new BusinessException("El monto del descuento debe ser mayor a 0");
                }
            }
            case DOS_POR_UNO -> {
                valor = null;
                if (r.alcance() != AlcancePromocion.PRODUCTO) {
                    throw new BusinessException("El 2x1 solo se puede aplicar a un producto");
                }
            }
        }

        Producto producto = null;
        Servicio servicio = null;
        if (r.alcance() == AlcancePromocion.PRODUCTO) {
            if (r.productoId() == null) throw new BusinessException("Elegi el producto de la promocion");
            producto = productoRepository.findById(r.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + r.productoId()));
        } else if (r.alcance() == AlcancePromocion.SERVICIO) {
            if (r.servicioId() == null) throw new BusinessException("Elegi el servicio de la promocion");
            servicio = servicioRepository.findById(r.servicioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + r.servicioId()));
        }

        Cliente cliente = null;
        if (r.clienteId() != null) {
            cliente = clienteRepository.findById(r.clienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + r.clienteId()));
        }

        p.setNegocio(negocio);
        p.setNombre(r.nombre().trim());
        p.setTipo(r.tipo());
        p.setAlcance(r.alcance());
        p.setValor(valor);
        p.setProducto(producto);
        p.setServicio(servicio);
        p.setCliente(cliente);
        p.setFechaDesde(r.fechaDesde());
        p.setFechaHasta(r.fechaHasta());
        if (r.activa() != null) {
            p.setActiva(r.activa());
        } else if (p.getId() == null) {
            p.setActiva(true);
        }
    }

    // ---------- calculo ----------

    /**
     * Calcula el descuento de cada item al que aplica la promocion, lo deja
     * en item.descuento y devuelve el total descontado. Tira BusinessException
     * si la promocion no aplica a nada del carrito o no genera descuento.
     */
    public BigDecimal aplicarPromocion(Promocion p, List<VentaItem> items) {
        List<VentaItem> aplicables = items.stream().filter(i -> aplicaA(p, i)).toList();
        if (aplicables.isEmpty()) {
            throw new BusinessException("La promocion \"" + p.getNombre() + "\" no aplica a los items de esta venta");
        }
        BigDecimal base = aplicables.stream().map(VentaItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        switch (p.getTipo()) {
            case PORCENTAJE -> {
                for (VentaItem item : aplicables) {
                    item.setDescuento(item.getSubtotal().multiply(p.getValor())
                            .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP));
                }
            }
            case MONTO_FIJO -> {
                BigDecimal total = p.getValor().min(base);
                BigDecimal repartido = BigDecimal.ZERO;
                for (int i = 0; i < aplicables.size(); i++) {
                    VentaItem item = aplicables.get(i);
                    BigDecimal parte = (i == aplicables.size() - 1)
                            ? total.subtract(repartido)
                            : total.multiply(item.getSubtotal()).divide(base, 0, RoundingMode.HALF_UP);
                    item.setDescuento(parte.min(item.getSubtotal()));
                    repartido = repartido.add(item.getDescuento());
                }
            }
            case DOS_POR_UNO -> {
                for (VentaItem item : aplicables) {
                    BigDecimal pares = item.getCantidad().divideToIntegralValue(BigDecimal.valueOf(2));
                    item.setDescuento(pares.multiply(item.getPrecioUnitario()));
                }
            }
        }

        BigDecimal totalDescuento = aplicables.stream().map(VentaItem::getDescuento).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDescuento.signum() <= 0) {
            throw new BusinessException(p.getTipo() == TipoPromocion.DOS_POR_UNO
                    ? "El 2x1 necesita al menos 2 unidades del producto"
                    : "La promocion no genera descuento en esta venta");
        }
        return totalDescuento;
    }

    private boolean aplicaA(Promocion p, VentaItem item) {
        return switch (p.getAlcance()) {
            case TODA_LA_VENTA -> true;
            case PRODUCTO -> item.getProducto() != null && item.getProducto().getId().equals(p.getProducto().getId());
            case SERVICIO -> item.getServicio() != null && item.getServicio().getId().equals(p.getServicio().getId());
        };
    }

    private PromocionResponse toResponse(Promocion p) {
        return new PromocionResponse(
                p.getId(), p.getNegocio().getId(), p.getNombre(), p.getTipo(), p.getAlcance(), p.getValor(),
                p.getProducto() != null ? p.getProducto().getId() : null,
                p.getProducto() != null ? p.getProducto().getNombre() : null,
                p.getServicio() != null ? p.getServicio().getId() : null,
                p.getServicio() != null ? p.getServicio().getNombre() : null,
                p.getCliente() != null ? p.getCliente().getId() : null,
                p.getCliente() != null ? p.getCliente().getNombre() + " " + p.getCliente().getApellido() : null,
                p.getFechaDesde(), p.getFechaHasta(), p.isActiva(), estaVigente(p));
    }
}
