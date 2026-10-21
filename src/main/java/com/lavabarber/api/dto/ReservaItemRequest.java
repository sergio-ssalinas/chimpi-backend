package com.lavabarber.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record ReservaItemRequest(
        @NotNull UUID servicioId,
        @NotNull UUID empleadoId,
        @NotNull LocalTime horaInicio
) {
}
