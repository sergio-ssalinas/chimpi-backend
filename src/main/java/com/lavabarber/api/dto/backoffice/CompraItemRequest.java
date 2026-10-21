package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record CompraItemRequest(
        @NotNull UUID productoId,
        @NotNull @Positive BigDecimal cantidad,
        @NotNull @PositiveOrZero BigDecimal precioUnitario
) {
}
