package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record AbrirCajaRequest(
        @NotNull UUID negocioId,
        @NotNull @PositiveOrZero BigDecimal montoInicial
) {
}
