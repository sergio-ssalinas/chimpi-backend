package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VentaResponse(
        UUID id,
        UUID cajaId,
        UUID clienteId,
        String clienteNombre,
        /** RUC cargado del cliente (null si no tiene). La cedula NO viaja aca. */
        String clienteRuc,
        String usuarioNombre,
        LocalDateTime fecha,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal total,
        String promocionNombre,
        String metodoPago,
        String estado,
        List<VentaItemResponse> items
) {
}
