package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EtapaServicioRequest(
        @NotNull Integer orden,
        @NotBlank String nombre,
        @NotNull Integer porcentajeHasta
) {
}
