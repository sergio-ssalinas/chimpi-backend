package com.lavabarber.api.dto;

import java.util.UUID;

public record NegocioResponse(UUID id, String nombre, String direccion, String telefono) {
}
