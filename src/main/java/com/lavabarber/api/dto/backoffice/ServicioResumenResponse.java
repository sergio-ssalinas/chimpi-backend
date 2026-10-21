package com.lavabarber.api.dto.backoffice;

import java.util.UUID;

/** Version liviana de Servicio, sin anidar componentes ni relaciones (para listas dentro de otros DTOs). */
public record ServicioResumenResponse(UUID id, String nombre, boolean combo) {
}
