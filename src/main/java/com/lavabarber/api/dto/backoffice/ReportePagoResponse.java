package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pago saliente unificado: egresos de la empresa (origen = EGRESO) y pagos de
 * utilidades a socios (origen = SOCIO). estado: ACTIVO o ANULADO (los socios
 * no se anulan, siempre ACTIVO).
 */
public record ReportePagoResponse(
        UUID id,
        String origen,
        LocalDate fecha,
        String categoria,
        String concepto,
        String beneficiario,
        String metodoPago,
        String comprobanteNumero,
        String estado,
        BigDecimal monto
) {
}
