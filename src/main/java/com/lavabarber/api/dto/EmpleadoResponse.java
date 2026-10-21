package com.lavabarber.api.dto;

import java.util.UUID;

public record EmpleadoResponse(UUID id, String nombre, String fotoUrl) {
}
