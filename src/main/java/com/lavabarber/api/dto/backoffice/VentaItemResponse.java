package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.TipoItemVenta;

import java.math.BigDecimal;
import java.util.UUID;

public record VentaItemResponse(
        UUID id,
        TipoItemVenta tipo,
        UUID productoId,
        UUID servicioId,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        BigDecimal descuento
) {
}
