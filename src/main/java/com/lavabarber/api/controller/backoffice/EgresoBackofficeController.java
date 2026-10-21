package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.EgresoRequest;
import com.lavabarber.api.dto.backoffice.EgresoResponse;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.repository.UsuarioInternoRepository;
import com.lavabarber.api.security.UsuarioInternoDetails;
import com.lavabarber.api.service.EgresoService;
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
 * Un EMPLEADO puede registrar un egreso solo si sale de SU propia caja
 * abierta. Cualquier egreso sin caja y el listado completo quedan
 * reservados a ADMIN — ver EgresoService.
 */
@RestController
@RequestMapping("/api/backoffice/egresos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
public class EgresoBackofficeController {

    private final EgresoService egresoService;
    private final UsuarioInternoRepository usuarioInternoRepository;

    @PostMapping
    public ResponseEntity<EgresoResponse> registrar(@Valid @RequestBody EgresoRequest request, Authentication authentication) {
        UsuarioInternoDetails d = (UsuarioInternoDetails) authentication.getPrincipal();
        UsuarioInterno usuario = usuarioInternoRepository.getReferenceById(d.getUsuarioInternoId());
        EgresoResponse creado = egresoService.registrar(request, usuario, d.getRol().equals("ADMIN"));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<EgresoResponse> listar(@RequestParam UUID negocioId) {
        return egresoService.listar(negocioId);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public EgresoResponse anular(@PathVariable UUID id) {
        return egresoService.anular(id);
    }
}
