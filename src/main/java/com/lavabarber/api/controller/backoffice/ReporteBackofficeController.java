package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.*;
import com.lavabarber.api.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Reportes: solo ADMIN. Fechas en formato ISO (yyyy-MM-dd), ambas inclusive. */
@RestController
@RequestMapping("/api/backoffice/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReporteBackofficeController {

    private final ReporteService reporteService;

    @GetMapping("/resumen")
    public ReporteResumenResponse resumen(@RequestParam UUID negocioId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.resumen(negocioId, desde, hasta);
    }

    @GetMapping("/ventas")
    public List<ReporteVentaResponse> ventas(@RequestParam UUID negocioId,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.ventasDetalle(negocioId, desde, hasta);
    }

    @GetMapping("/compras")
    public List<ReporteCompraResponse> compras(@RequestParam UUID negocioId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.compras(negocioId, desde, hasta);
    }

    @GetMapping("/pagos")
    public List<ReportePagoResponse> pagos(@RequestParam UUID negocioId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.pagos(negocioId, desde, hasta);
    }
}
