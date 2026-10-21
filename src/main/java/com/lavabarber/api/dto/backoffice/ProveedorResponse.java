package com.lavabarber.api.dto.backoffice;

import java.util.UUID;

public record ProveedorResponse(
        UUID id,
        UUID negocioId,
        String nombre,
        String ruc,
        String telefono,
        String email,
        String direccion,
        boolean activo
) {
}
