package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.VentaRequest;
import com.lavabarber.api.dto.backoffice.VentaResponse;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.repository.UsuarioInternoRepository;
import com.lavabarber.api.security.UsuarioInternoDetails;
import com.lavabarber.api.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/backoffice/ventas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','CAJERO')")
public class VentaBackofficeController {

    private final VentaService ventaService;
    private final UsuarioInternoRepository usuarioInternoRepository;

    private UsuarioInternoDetails detallesDe(Authentication authentication) {
        return (UsuarioInternoDetails) authentication.getPrincipal();
    }

    @PostMapping
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        UsuarioInterno usuario = usuarioInternoRepository.getReferenceById(d.getUsuarioInternoId());
        VentaResponse creada = ventaService.registrar(request, usuario, d.getRol().equals("ADMIN"));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /**
     * Por cajaId: cualquiera puede ver las ventas de SU propia caja (o
     * cualquiera si es ADMIN). Por negocioId (todas las ventas del
     * negocio, cualquier caja): solo ADMIN.
     */
    @GetMapping
    public List<VentaResponse> listar(@RequestParam(required = false) UUID cajaId,
                                       @RequestParam(required = false) UUID negocioId,
                                       Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        boolean esAdmin = d.getRol().equals("ADMIN");

        if (cajaId != null) {
            return ventaService.listarDeCaja(cajaId, d.getUsuarioInternoId(), esAdmin);
        }
        if (esAdmin && negocioId != null) {
            return ventaService.listarDeNegocio(negocioId);
        }
        throw new BusinessException("Se requiere cajaId, o negocioId si sos ADMIN");
    }

    @PostMapping("/{id}/anular")
    public VentaResponse anular(@PathVariable UUID id, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        UsuarioInterno usuario = usuarioInternoRepository.getReferenceById(d.getUsuarioInternoId());
        return ventaService.anular(id, usuario, d.getRol().equals("ADMIN"));
    }
}
