package com.lavabarber.api.service;

import com.lavabarber.api.entity.MovimientoStock;
import com.lavabarber.api.entity.Producto;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.entity.enums.TipoMovimientoStock;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.repository.MovimientoStockRepository;
import com.lavabarber.api.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unico punto de entrada para cambiar el stock de un producto. Nunca se
 * toca Producto.stockActual directamente desde otro lado del codigo —
 * asi cada cambio queda con su MovimientoStock correspondiente, sin
 * excepciones, y el historial siempre es confiable.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoStockRepository;

    @Transactional
    public MovimientoStock registrarMovimiento(Producto producto, TipoMovimientoStock tipo, BigDecimal cantidadAbsoluta,
                                                String motivo, UsuarioInterno usuario) {
        BigDecimal delta = esEntrada(tipo) ? cantidadAbsoluta.abs() : cantidadAbsoluta.abs().negate();
        BigDecimal nuevoStock = producto.getStockActual().add(delta);

        if (nuevoStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("No hay suficiente stock de " + producto.getNombre()
                    + " (actual: " + producto.getStockActual() + ", se intento restar: " + cantidadAbsoluta + ")");
        }

        producto.setStockActual(nuevoStock);
        productoRepository.save(producto);

        MovimientoStock movimiento = MovimientoStock.builder()
                .producto(producto)
                .tipo(tipo)
                .cantidad(delta)
                .stockResultante(nuevoStock)
                .fecha(LocalDateTime.now())
                .motivo(motivo)
                .usuarioInterno(usuario)
                .build();

        return movimientoStockRepository.save(movimiento);
    }

    private boolean esEntrada(TipoMovimientoStock tipo) {
        return tipo == TipoMovimientoStock.ENTRADA_COMPRA || tipo == TipoMovimientoStock.AJUSTE_POSITIVO;
    }
}
