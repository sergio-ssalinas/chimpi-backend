package com.lavabarber.api.dto;

import java.time.LocalTime;
import java.util.UUID;

public record ProgresoReservaItemResponse(
        UUID reservaItemId,
        UUID reservaId,
        String servicioNombre,
        String empleadoNombre,
        LocalTime horaInicio,
        LocalTime horaFin,
        /** NO_INICIADO (todavia no llega la hora), EN_PROGRESO, o FINALIZADO. */
        String estado,
        int porcentaje,
        String etapaActual
) {
}
