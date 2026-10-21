package com.lavabarber.api.dto;

import java.time.LocalTime;
import java.util.UUID;

/** Un slot de horario libre para un empleado y servicio puntual. */
public record HorarioDisponibleResponse(
        UUID empleadoId,
        String empleadoNombre,
        LocalTime horaInicio,
        LocalTime horaFin
) {
}
