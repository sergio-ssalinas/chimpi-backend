package com.lavabarber.api.dto.backoffice;

import java.util.List;
import java.util.UUID;

public record EmpleadoBackofficeResponse(
        UUID id,
        UUID negocioId,
        String nombre,
        String fotoUrl,
        boolean activo,
        List<ServicioResumenResponse> servicios
) {
}
