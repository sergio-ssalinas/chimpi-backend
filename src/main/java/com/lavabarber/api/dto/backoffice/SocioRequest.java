package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SocioRequest(
        @NotNull UUID negocioId,
        @NotBlank String nombre,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal porcentajeParticipacion,
        String telefono,
        String email,
        LocalDate fechaIngreso,
        Boolean activo
) {
}
