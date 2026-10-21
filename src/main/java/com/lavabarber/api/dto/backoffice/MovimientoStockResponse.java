package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MovimientoStockResponse(
        UUID id,
        String tipo,
        BigDecimal cantidad,
        BigDecimal stockResultante,
        LocalDateTime fecha,
        String motivo,
        String usuarioNombre
) {
}
