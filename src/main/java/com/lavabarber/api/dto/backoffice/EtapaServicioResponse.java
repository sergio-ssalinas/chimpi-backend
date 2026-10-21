package com.lavabarber.api.dto.backoffice;

import java.util.UUID;

public record EtapaServicioResponse(UUID id, Integer orden, String nombre, Integer porcentajeHasta) {
}
