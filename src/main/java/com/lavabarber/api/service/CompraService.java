package com.lavabarber.api.service;

import com.lavabarber.api.dto.backoffice.*;
import com.lavabarber.api.entity.*;
import com.lavabarber.api.entity.enums.TipoMovimientoStock;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registra una compra a proveedor: crea la Compra con sus items, y por
 * cada item genera la entrada de stock correspondiente via StockService.
 * Todo en una sola transaccion — si algo falla, no queda ni la compra ni
 * el stock a medio actualizar.
 */
@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final NegocioRepository negocioRepository;
    private final StockService stockService;

    @Transactional
    public CompraResponse registrar(CompraRequest request, UsuarioInterno usuario) {
        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));
        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + request.proveedorId()));

        Compra compra = Compra.builder()
                .negocio(negocio)
                .proveedor(proveedor)
                .fecha(request.fecha())
                .numeroFactura(request.numeroFactura())
                .total(BigDecimal.ZERO)
                .build();

        List<CompraItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CompraItemRequest itemReq : request.items()) {
            Producto producto = productoRepository.findById(itemReq.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + itemReq.productoId()));

            BigDecimal subtotal = itemReq.precioUnitario().multiply(itemReq.cantidad());
            items.add(CompraItem.builder()
                    .compra(compra)
                    .producto(producto)
                    .cantidad(itemReq.cantidad())
                    .precioUnitario(itemReq.precioUnitario())
                    .subtotal(subtotal)
                    .build());
            total = total.add(subtotal);

            stockService.registrarMovimiento(
                    producto, TipoMovimientoStock.ENTRADA_COMPRA, itemReq.cantidad(),
                    "Compra a " + proveedor.getNombre() + (request.numeroFactura() != null ? " (fact. " + request.numeroFactura() + ")" : ""),
                    usuario);
        }

        compra.setItems(items);
        compra.setTotal(total);
        Compra guardada = compraRepository.save(compra);

        return toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listar(UUID negocioId) {
        return compraRepository.findByNegocioIdOrderByFechaDesc(negocioId).stream().map(this::toResponse).toList();
    }

    private CompraResponse toResponse(Compra c) {
        List<CompraItemResponse> items = c.getItems().stream()
                .map(i -> new CompraItemResponse(i.getId(), i.getProducto().getId(), i.getProducto().getNombre(),
                        i.getCantidad(), i.getPrecioUnitario(), i.getSubtotal()))
                .toList();

        return new CompraResponse(
                c.getId(), c.getNegocio().getId(), c.getProveedor().getId(), c.getProveedor().getNombre(),
                c.getFecha(), c.getNumeroFactura(), c.getTotal(), items);
    }
}
