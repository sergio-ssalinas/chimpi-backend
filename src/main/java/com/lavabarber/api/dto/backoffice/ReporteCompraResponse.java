package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReporteCompraResponse(
        UUID id,
        LocalDate fecha,
        String proveedorNombre,
        String numeroFactura,
        int cantidadItems,
        BigDecimal total
) {
}
