package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CajaResponse(
        UUID id,
        UUID negocioId,
        UUID usuarioInternoId,
        String usuarioNombre,
        LocalDateTime fechaApertura,
        BigDecimal montoInicial,
        String estado,
        LocalDateTime fechaCierre,
        BigDecimal montoFinalDeclarado,
        BigDecimal montoFinalEsperado,
        BigDecimal diferencia,
        String observaciones
) {
}
