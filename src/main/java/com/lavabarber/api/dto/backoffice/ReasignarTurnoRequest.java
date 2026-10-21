package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record ReasignarTurnoRequest(
        @NotNull UUID nuevoEmpleadoId,
        @NotNull LocalTime nuevaHoraInicio
) {
}
