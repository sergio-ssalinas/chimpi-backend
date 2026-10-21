package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.TipoItemVenta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * El precio NO viaja en el request: lo calcula el backend desde el
 * catalogo (Producto.precioVenta o Servicio.precio) al momento de la
 * venta, para que el cajero no pueda tipear un precio distinto.
 */
public record VentaItemRequest(
        @NotNull TipoItemVenta tipo,
        /** Requerido si tipo = PRODUCTO. */
        UUID productoId,
        /** Requerido si tipo = SERVICIO. */
        UUID servicioId,
        @NotNull @Positive BigDecimal cantidad
) {
}
