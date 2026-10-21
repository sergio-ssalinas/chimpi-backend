package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** cantidad puede ser negativa (ej: -2 por rotura/perdida) o positiva (ej: se encontro stock de mas al inventariar). */
public record AjusteStockRequest(
        @NotNull BigDecimal cantidad,
        @NotBlank String motivo
) {
}
