package com.lavabarber.api.dto.backoffice;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record DisponibilidadResponse(UUID id, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin) {
}
