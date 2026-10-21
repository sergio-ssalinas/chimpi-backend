package com.lavabarber.api.service;

import com.lavabarber.api.dto.backoffice.VentaItemRequest;
import com.lavabarber.api.dto.backoffice.VentaItemResponse;
import com.lavabarber.api.dto.backoffice.VentaRequest;
import com.lavabarber.api.dto.backoffice.VentaResponse;
import com.lavabarber.api.entity.*;
import com.lavabarber.api.entity.enums.EstadoCaja;
import com.lavabarber.api.entity.enums.EstadoVenta;
import com.lavabarber.api.entity.enums.TipoItemVenta;
import com.lavabarber.api.entity.enums.TipoMovimientoStock;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registra una venta que puede mezclar productos y servicios. El precio de
 * cada linea SIEMPRE sale del catalogo (Producto.precioVenta o
 * Servicio.precio) en el momento de la venta — el cajero no puede
 * mandarlo por su cuenta. Cada producto vendido genera una salida de
 * stock via StockService; anular una venta la restaura.
 */
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final CajaRepository cajaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ServicioRepository servicioRepository;
    private final StockService stockService;
    private final PromocionService promocionService;

    @Transactional
    public VentaResponse registrar(VentaRequest request, UsuarioInterno usuario, boolean esAdmin) {
        Caja caja = cajaRepository.findById(request.cajaId())
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada: " + request.cajaId()));

        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new BusinessException("Esa caja no esta abierta");
        }
        if (!esAdmin && !caja.getUsuarioInterno().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("No podes vender en una caja que no es tuya");
        }

        Cliente cliente = null;
        if (request.clienteId() != null) {
            cliente = clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.clienteId()));
        }

        Venta venta = Venta.builder()
                .negocio(caja.getNegocio())
                .caja(caja)
                .cliente(cliente)
                .usuarioInterno(usuario)
                .fecha(LocalDateTime.now())
                .metodoPago(request.metodoPago())
                .estado(EstadoVenta.COMPLETADA)
                .subtotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();

        List<VentaItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (VentaItemRequest itemReq : request.items()) {
            VentaItem item = itemReq.tipo() == TipoItemVenta.PRODUCTO
                    ? construirItemProducto(venta, itemReq, usuario)
                    : construirItemServicio(venta, itemReq);
            items.add(item);
            total = total.add(item.getSubtotal());
        }

        BigDecimal descuento = BigDecimal.ZERO;
        if (request.promocionId() != null) {
            Promocion promocion = promocionService.obtenerAplicable(
                    request.promocionId(), caja.getNegocio().getId(), cliente != null ? cliente.getId() : null);
            descuento = promocionService.aplicarPromocion(promocion, items);
            venta.setPromocion(promocion);
            venta.setPromocionNombre(promocion.getNombre());
        }

        venta.setItems(items);
        venta.setSubtotal(total);
        venta.setDescuento(descuento);
        venta.setTotal(total.subtract(descuento));

        return toResponse(ventaRepository.save(venta));
    }

    private VentaItem construirItemProducto(Venta venta, VentaItemRequest itemReq, UsuarioInterno usuario) {
        if (itemReq.productoId() == null) {
            throw new BusinessException("Falta el producto en un item de tipo PRODUCTO");
        }
        Producto producto = productoRepository.findById(itemReq.productoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + itemReq.productoId()));

        BigDecimal subtotal = producto.getPrecioVenta().multiply(itemReq.cantidad());

        // Se descuenta el stock ANTES de armar la venta: si no hay suficiente,
        // StockService tira BusinessException y toda la transaccion se revierte.
        stockService.registrarMovimiento(producto, TipoMovimientoStock.SALIDA_VENTA, itemReq.cantidad(), "Venta", usuario);

        return VentaItem.builder()
                .venta(venta).tipo(TipoItemVenta.PRODUCTO).producto(producto)
                .descripcion(producto.getNombre())
                .cantidad(itemReq.cantidad())
                .precioUnitario(producto.getPrecioVenta())
                .subtotal(subtotal)
                .build();
    }

    private VentaItem construirItemServicio(Venta venta, VentaItemRequest itemReq) {
        if (itemReq.servicioId() == null) {
            throw new BusinessException("Falta el servicio en un item de tipo SERVICIO");
        }
        Servicio servicio = servicioRepository.findById(itemReq.servicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + itemReq.servicioId()));

        BigDecimal precio = servicio.getPrecio() != null ? servicio.getPrecio() : BigDecimal.ZERO;
        BigDecimal subtotal = precio.multiply(itemReq.cantidad());

        return VentaItem.builder()
                .venta(venta).tipo(TipoItemVenta.SERVICIO).servicio(servicio)
                .descripcion(servicio.getNombre())
                .cantidad(itemReq.cantidad())
                .precioUnitario(precio)
                .subtotal(subtotal)
                .build();
    }

    @Transactional
    public VentaResponse anular(UUID ventaId, UsuarioInterno usuario, boolean esAdmin) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + ventaId));

        if (!esAdmin && !venta.getUsuarioInterno().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("No podes anular una venta que no es tuya");
        }
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new BusinessException("Esta venta ya esta anulada");
        }
        if (venta.getCaja().getEstado() == EstadoCaja.CERRADA) {
            throw new BusinessException("No se puede anular una venta de una caja ya cerrada");
        }

        for (VentaItem item : venta.getItems()) {
            if (item.getTipo() == TipoItemVenta.PRODUCTO && item.getProducto() != null) {
                stockService.registrarMovimiento(
                        item.getProducto(), TipoMovimientoStock.AJUSTE_POSITIVO, item.getCantidad(),
                        "Anulacion de venta", usuario);
            }
        }

        venta.setEstado(EstadoVenta.ANULADA);
        return toResponse(ventaRepository.save(venta));
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarDeCaja(UUID cajaId, UUID usuarioSolicitanteId, boolean esAdmin) {
        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada: " + cajaId));
        if (!esAdmin && !caja.getUsuarioInterno().getId().equals(usuarioSolicitanteId)) {
            throw new AccessDeniedException("No tenes permiso sobre esta caja");
        }
        return ventaRepository.findByCajaIdOrderByFechaDesc(cajaId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarDeNegocio(UUID negocioId) {
        return ventaRepository.findByNegocioIdOrderByFechaDesc(negocioId).stream().map(this::toResponse).toList();
    }

    private VentaResponse toResponse(Venta v) {
        List<VentaItemResponse> items = v.getItems().stream()
                .map(i -> new VentaItemResponse(
                        i.getId(), i.getTipo(),
                        i.getProducto() != null ? i.getProducto().getId() : null,
                        i.getServicio() != null ? i.getServicio().getId() : null,
                        i.getDescripcion(), i.getCantidad(), i.getPrecioUnitario(), i.getSubtotal(), i.getDescuento()))
                .toList();

        return new VentaResponse(
                v.getId(), v.getCaja().getId(),
                v.getCliente() != null ? v.getCliente().getId() : null,
                v.getCliente() != null ? (v.getCliente().getNombre() + " " + v.getCliente().getApellido()).trim() : null,
                v.getCliente() != null ? v.getCliente().getRuc() : null,
                v.getUsuarioInterno().getNombre() + " " + v.getUsuarioInterno().getApellido(),
                v.getFecha(), v.getSubtotal(), v.getDescuento(), v.getTotal(), v.getPromocionNombre(),
                v.getMetodoPago().name(), v.getEstado().name(), items);
    }
}
