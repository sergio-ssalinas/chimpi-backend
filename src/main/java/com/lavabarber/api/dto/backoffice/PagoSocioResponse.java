package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PagoSocioResponse(
        UUID id,
        UUID socioId,
        String socioNombre,
        BigDecimal monto,
        LocalDate fecha,
        String concepto,
        String metodoPago,
        String comprobanteNumero,
        String usuarioNombre
) {
}
