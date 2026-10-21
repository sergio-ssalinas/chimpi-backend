package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.PromocionRequest;
import com.lavabarber.api.dto.backoffice.PromocionResponse;
import com.lavabarber.api.service.PromocionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/backoffice/promociones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PromocionBackofficeController {

    private final PromocionService promocionService;

    @GetMapping
    public List<PromocionResponse> listar(@RequestParam UUID negocioId) {
        return promocionService.listar(negocioId);
    }

    /** Para el POS: las que se pueden usar hoy (generales + las del cliente elegido). Lo ven ADMIN, EMPLEADO y CAJERO. */
    @GetMapping("/vigentes")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','CAJERO')")
    public List<PromocionResponse> vigentes(@RequestParam UUID negocioId,
                                            @RequestParam(required = false) UUID clienteId) {
        return promocionService.listarVigentes(negocioId, clienteId);
    }

    @PostMapping
    public PromocionResponse crear(@Valid @RequestBody PromocionRequest request) {
        return promocionService.crear(request);
    }

    @PutMapping("/{id}")
    public PromocionResponse actualizar(@PathVariable UUID id, @Valid @RequestBody PromocionRequest request) {
        return promocionService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public PromocionResponse desactivar(@PathVariable UUID id) {
        return promocionService.desactivar(id);
    }
}
