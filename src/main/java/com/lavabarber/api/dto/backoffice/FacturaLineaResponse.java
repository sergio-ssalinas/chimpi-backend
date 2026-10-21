package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;

/** Renglon de factura. precioUnitario es de lista; importe es el neto cobrado (cantidad x precio - descuento). */
public record FacturaLineaResponse(
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal importe,
        /** DIEZ | CINCO | EXENTA */
        String tasaIva
) {
}
