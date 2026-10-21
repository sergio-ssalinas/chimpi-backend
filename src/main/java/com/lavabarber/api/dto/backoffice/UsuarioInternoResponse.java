package com.lavabarber.api.dto.backoffice;

import java.util.UUID;

public record UsuarioInternoResponse(
        UUID id,
        String username,
        String nombre,
        String apellido,
        String rol,
        boolean activo,
        UUID empleadoId,
        String empleadoNombre
) {
}
