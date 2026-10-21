package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProveedorRequest(
        @NotNull UUID negocioId,
        @NotBlank String nombre,
        String ruc,
        String telefono,
        String email,
        String direccion,
        Boolean activo
) {
}
