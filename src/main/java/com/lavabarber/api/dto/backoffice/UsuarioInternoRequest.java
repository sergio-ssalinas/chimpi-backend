package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.RolInterno;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UsuarioInternoRequest(
        @NotBlank String username,
        /** Requerida al crear. Al editar, si viene null/vacia no se cambia la contrasena actual. */
        String password,
        @NotBlank String nombre,
        @NotBlank String apellido,
        @NotNull RolInterno rol,
        Boolean activo,
        /** Solo tiene sentido si rol = EMPLEADO. */
        UUID empleadoId
) {
}
