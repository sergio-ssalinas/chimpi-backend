package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Fila del reporte de ventas, con sus lineas (sirve tambien para armar el ticket). */
public record ReporteVentaResponse(
        UUID id,
        LocalDateTime fecha,
        String clienteNombre,
        String cajeroNombre,
        String metodoPago,
        String estado,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal total,
        String promocionNombre,
        String numeroFactura,
        List<Linea> items
) {
    public record Linea(
            String tipo,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            BigDecimal descuento
    ) {}
}
