package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record EmpleadoBackofficeRequest(
        @NotNull UUID negocioId,
        @NotBlank String nombre,
        String fotoUrl,
        Boolean activo,
        List<UUID> serviciosIds
) {
}
