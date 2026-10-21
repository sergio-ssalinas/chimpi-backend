package com.lavabarber.api.dto;

import java.util.UUID;

public record AuthInternoResponse(
        String token,
        UUID usuarioInternoId,
        String username,
        String nombre,
        String apellido,
        String rol,
        /** Empleado vinculado al usuario (null si no tiene). El frontend lo usa para filtrar "sus" turnos. */
        UUID empleadoId
) {
}
