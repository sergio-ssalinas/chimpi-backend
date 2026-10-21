package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CompraResponse(
        UUID id,
        UUID negocioId,
        UUID proveedorId,
        String proveedorNombre,
        LocalDate fecha,
        String numeroFactura,
        BigDecimal total,
        List<CompraItemResponse> items
) {
}
