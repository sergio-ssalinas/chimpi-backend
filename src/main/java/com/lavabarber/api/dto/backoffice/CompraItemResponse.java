package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.util.UUID;

public record CompraItemResponse(
        UUID id,
        UUID productoId,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
