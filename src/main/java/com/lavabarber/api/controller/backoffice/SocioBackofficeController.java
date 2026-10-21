package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.SocioRequest;
import com.lavabarber.api.dto.backoffice.SocioResponse;
import com.lavabarber.api.service.SocioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/backoffice/socios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SocioBackofficeController {

    private final SocioService socioService;

    @GetMapping
    public List<SocioResponse> listar(@RequestParam UUID negocioId) {
        return socioService.listar(negocioId);
    }

    @PostMapping
    public SocioResponse crear(@Valid @RequestBody SocioRequest request) {
        return socioService.crear(request);
    }

    @PutMapping("/{id}")
    public SocioResponse actualizar(@PathVariable UUID id, @Valid @RequestBody SocioRequest request) {
        return socioService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public SocioResponse desactivar(@PathVariable UUID id) {
        return socioService.desactivar(id);
    }
}
