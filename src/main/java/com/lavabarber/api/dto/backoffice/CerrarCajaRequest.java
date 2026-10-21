package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CerrarCajaRequest(
        @NotNull @PositiveOrZero BigDecimal montoFinalDeclarado,
        String observaciones
) {
}
