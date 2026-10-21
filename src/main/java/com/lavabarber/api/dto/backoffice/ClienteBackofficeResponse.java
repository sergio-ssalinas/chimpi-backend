package com.lavabarber.api.dto.backoffice;

import java.util.UUID;

public record ClienteBackofficeResponse(
        UUID id,
        String nombre,
        String apellido,
        String telefono,
        String documentoIdentidad,
        String ruc,
        String email,
        String direccion,
        boolean activo
) {
}
