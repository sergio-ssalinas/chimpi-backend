package com.lavabarber.api.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID clienteId,
        String username,
        String nombre
) {
}
