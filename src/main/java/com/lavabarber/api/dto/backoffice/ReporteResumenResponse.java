package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen del periodo [desde, hasta] (ambos inclusive). Solo cuentan ventas
 * COMPLETADAS y egresos ACTIVOS; lo anulado se informa aparte (ventasAnuladas).
 * resultadoOperativo = ventasNeto - comprasTotal - egresosTotal. Los pagos a
 * socios NO restan del resultado: son reparto de utilidades, se muestran aparte.
 */
public record ReporteResumenResponse(
        LocalDate desde,
        LocalDate hasta,
        int ventasCantidad,
        int ventasAnuladas,
        BigDecimal ventasBruto,
        BigDecimal ventasDescuento,
        BigDecimal ventasNeto,
        BigDecimal ticketPromedio,
        BigDecimal comprasTotal,
        BigDecimal egresosTotal,
        BigDecimal pagosSociosTotal,
        BigDecimal resultadoOperativo,
        List<PorMetodo> recaudacionPorMetodo,
        List<PorDia> ventasPorDia,
        List<PorTipo> ventasPorTipo,
        List<TopItem> topItems
) {
    public record PorMetodo(String metodoPago, int cantidad, BigDecimal total) {}

    public record PorDia(LocalDate fecha, int cantidad, BigDecimal total) {}

    /** tipo = PRODUCTO | SERVICIO; cantidad = unidades vendidas; total = neto (ya sin descuento). */
    public record PorTipo(String tipo, BigDecimal cantidad, BigDecimal total) {}

    public record TopItem(String descripcion, String tipo, BigDecimal cantidad, BigDecimal total) {}
}
