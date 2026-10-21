package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EgresoResponse(
        UUID id,
        UUID negocioId,
        String categoriaNombre,
        String concepto,
        BigDecimal monto,
        LocalDate fecha,
        String metodoPago,
        String proveedorNombre,
        String empleadoNombre,
        UUID cajaId,
        String comprobanteNumero,
        String usuarioNombre,
        String estado
) {
}
