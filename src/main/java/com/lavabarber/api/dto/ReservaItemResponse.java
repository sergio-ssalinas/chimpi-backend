package com.lavabarber.api.dto;

import java.time.LocalTime;
import java.util.UUID;

public record ReservaItemResponse(
        UUID id,
        UUID servicioId,
        String servicioNombre,
        UUID empleadoId,
        String empleadoNombre,
        LocalTime horaInicio,
        LocalTime horaFin
) {
}
