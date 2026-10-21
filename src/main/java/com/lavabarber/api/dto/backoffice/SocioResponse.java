package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SocioResponse(
        UUID id,
        UUID negocioId,
        String nombre,
        BigDecimal porcentajeParticipacion,
        String telefono,
        String email,
        LocalDate fechaIngreso,
        boolean activo
) {
}
