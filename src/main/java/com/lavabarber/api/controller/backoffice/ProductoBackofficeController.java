package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.*;
import com.lavabarber.api.entity.*;
import com.lavabarber.api.entity.enums.TipoMovimientoStock;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.*;
import com.lavabarber.api.security.UsuarioInternoDetails;
import com.lavabarber.api.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/backoffice/productos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductoBackofficeController {

    private final ProductoRepository productoRepository;
    private final NegocioRepository negocioRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final UsuarioInternoRepository usuarioInternoRepository;
    private final StockService stockService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','CAJERO')")
    public List<ProductoResponse> listar(@RequestParam UUID negocioId) {
        return productoRepository.findByNegocioId(negocioId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','CAJERO')")
    public ProductoResponse buscarPorCodigoBarras(@RequestParam String codigoBarras) {
        return productoRepository.findByCodigoBarras(codigoBarras)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No hay ningun producto con ese codigo de barras"));
    }

    @PostMapping
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        if (request.codigoBarras() != null && !request.codigoBarras().isBlank()
                && productoRepository.existsByCodigoBarras(request.codigoBarras())) {
            throw new BusinessException("Ya existe un producto con ese codigo de barras");
        }
        Producto producto = new Producto();
        aplicarDatos(producto, request);
        return toResponse(productoRepository.save(producto));
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));

        boolean cambioCodigoBarras = request.codigoBarras() != null && !request.codigoBarras().equals(producto.getCodigoBarras());
        if (cambioCodigoBarras && productoRepository.existsByCodigoBarras(request.codigoBarras())) {
            throw new BusinessException("Ya existe un producto con ese codigo de barras");
        }

        aplicarDatos(producto, request);
        return toResponse(productoRepository.save(producto));
    }

    /** Soft delete: se desactiva, no se borra (las compras/movimientos historicos lo referencian). */
    @DeleteMapping("/{id}")
    public ProductoResponse desactivar(@PathVariable UUID id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
        producto.setActivo(false);
        return toResponse(productoRepository.save(producto));
    }

    @GetMapping("/{id}/movimientos")
    public List<MovimientoStockResponse> movimientos(@PathVariable UUID id) {
        if (!productoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado: " + id);
        }
        return movimientoStockRepository.findByProductoIdOrderByFechaDesc(id).stream()
                .map(m -> new MovimientoStockResponse(
                        m.getId(), m.getTipo().name(), m.getCantidad(), m.getStockResultante(), m.getFecha(),
                        m.getMotivo(), m.getUsuarioInterno() != null ? m.getUsuarioInterno().getNombre() : null))
                .toList();
    }

    @PostMapping("/{id}/ajustar-stock")
    public ProductoResponse ajustarStock(@PathVariable UUID id, @Valid @RequestBody AjusteStockRequest request,
                                          Authentication authentication) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));

        UUID usuarioId = ((UsuarioInternoDetails) authentication.getPrincipal()).getUsuarioInternoId();
        UsuarioInterno usuario = usuarioInternoRepository.getReferenceById(usuarioId);

        var tipo = request.cantidad().signum() >= 0 ? TipoMovimientoStock.AJUSTE_POSITIVO : TipoMovimientoStock.AJUSTE_NEGATIVO;
        stockService.registrarMovimiento(producto, tipo, request.cantidad(), request.motivo(), usuario);

        return toResponse(producto);
    }

    private void aplicarDatos(Producto producto, ProductoRequest request) {
        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));

        producto.setNegocio(negocio);
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setCodigoBarras(request.codigoBarras() != null && request.codigoBarras().isBlank() ? null : request.codigoBarras());
        producto.setUnidadMedida(request.unidadMedida());
        producto.setPrecioVenta(request.precioVenta());
        producto.setTasaIva(request.tasaIva());
        producto.setStockMinimo(request.stockMinimo() != null ? request.stockMinimo() : java.math.BigDecimal.ZERO);

        if (request.categoriaId() != null) {
            producto.setCategoria(categoriaProductoRepository.findById(request.categoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada: " + request.categoriaId())));
        } else {
            producto.setCategoria(null);
        }

        if (request.activo() != null) {
            producto.setActivo(request.activo());
        } else if (producto.getId() == null) {
            producto.setActivo(true);
        }

        // stockActual NUNCA se toca aca: solo StockService puede cambiarlo (via ajustar-stock o una compra).
        if (producto.getStockActual() == null) {
            producto.setStockActual(java.math.BigDecimal.ZERO);
        }
    }

    private ProductoResponse toResponse(Producto p) {
        CategoriaProductoResponse categoria = p.getCategoria() != null
                ? new CategoriaProductoResponse(p.getCategoria().getId(), p.getCategoria().getNombre())
                : null;

        return new ProductoResponse(
                p.getId(), p.getNegocio().getId(), categoria, p.getNombre(), p.getDescripcion(), p.getCodigoBarras(),
                p.getUnidadMedida(), p.getPrecioVenta(), p.getTasaIva(), p.getStockActual(), p.getStockMinimo(),
                p.isActivo(), p.isStockBajo());
    }
}
