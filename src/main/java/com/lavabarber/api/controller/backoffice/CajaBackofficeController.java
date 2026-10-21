package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.AbrirCajaRequest;
import com.lavabarber.api.dto.backoffice.CajaResponse;
import com.lavabarber.api.dto.backoffice.CerrarCajaRequest;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.entity.enums.EstadoCaja;
import com.lavabarber.api.repository.UsuarioInternoRepository;
import com.lavabarber.api.security.UsuarioInternoDetails;
import com.lavabarber.api.service.CajaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Multiples cajas pueden estar abiertas a la vez (una por cajero/turno).
 * Un ADMIN o EMPLEADO puede abrir/cerrar/operar la suya propia; solo un
 * ADMIN puede listar o ver el detalle de la caja de otra persona.
 */
@RestController
@RequestMapping("/api/backoffice/caja")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','CAJERO')")
public class CajaBackofficeController {

    private final CajaService cajaService;
    private final UsuarioInternoRepository usuarioInternoRepository;

    private UsuarioInternoDetails detallesDe(Authentication authentication) {
        return (UsuarioInternoDetails) authentication.getPrincipal();
    }

    @PostMapping("/abrir")
    public CajaResponse abrir(@Valid @RequestBody AbrirCajaRequest request, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        UsuarioInterno usuario = usuarioInternoRepository.getReferenceById(d.getUsuarioInternoId());
        return cajaService.abrir(request, usuario);
    }

    /** La caja abierta del usuario logueado (404 si no tiene ninguna). */
    @GetMapping("/actual")
    public CajaResponse actual(Authentication authentication) {
        return cajaService.obtenerActual(detallesDe(authentication).getUsuarioInternoId());
    }

    @PostMapping("/{id}/cerrar")
    public CajaResponse cerrar(@PathVariable UUID id, @Valid @RequestBody CerrarCajaRequest request, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        return cajaService.cerrar(id, request, d.getUsuarioInternoId(), d.getRol().equals("ADMIN"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CajaResponse> listar(@RequestParam UUID negocioId, @RequestParam(required = false) String estado) {
        EstadoCaja filtro = estado != null ? EstadoCaja.valueOf(estado) : null;
        return cajaService.listar(negocioId, filtro);
    }

    @GetMapping("/{id}")
    public CajaResponse obtener(@PathVariable UUID id, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        return cajaService.obtenerPorId(id, d.getUsuarioInternoId(), d.getRol().equals("ADMIN"));
    }
}
