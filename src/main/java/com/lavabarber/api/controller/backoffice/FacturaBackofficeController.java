package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.FacturaResponse;
import com.lavabarber.api.dto.backoffice.GenerarFacturaRequest;
import com.lavabarber.api.security.UsuarioInternoDetails;
import com.lavabarber.api.service.FacturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Facturacion (ver Factura/EstadoFactura). En el dia a dia factura el mismo cajero que vendio:
 * ADMIN puede facturar y consultar cualquier venta; EMPLEADO y CAJERO solo las de su propia caja
 * (se valida en FacturaService, devuelve 403 si no es suya).
 */
@RestController
@RequestMapping("/api/backoffice/facturas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','CAJERO')")
public class FacturaBackofficeController {

    private final FacturaService facturaService;

    @PostMapping("/venta/{ventaId}")
    public ResponseEntity<FacturaResponse> generar(@PathVariable UUID ventaId, @Valid @RequestBody GenerarFacturaRequest request,
                                                   Authentication authentication) {
        UsuarioInternoDetails d = (UsuarioInternoDetails) authentication.getPrincipal();
        FacturaResponse creada = facturaService.generar(ventaId, request, d.getUsuarioInternoId(), d.getRol().equals("ADMIN"));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/venta/{ventaId}")
    public FacturaResponse porVenta(@PathVariable UUID ventaId, Authentication authentication) {
        UsuarioInternoDetails d = (UsuarioInternoDetails) authentication.getPrincipal();
        return facturaService.obtenerPorVenta(ventaId, d.getUsuarioInternoId(), d.getRol().equals("ADMIN"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<FacturaResponse> listar(@RequestParam UUID negocioId) {
        return facturaService.listar(negocioId);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public FacturaResponse anular(@PathVariable UUID id) {
        return facturaService.anular(id);
    }
}
