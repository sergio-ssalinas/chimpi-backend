package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.PagoSocioRequest;
import com.lavabarber.api.dto.backoffice.PagoSocioResponse;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.repository.UsuarioInternoRepository;
import com.lavabarber.api.security.UsuarioInternoDetails;
import com.lavabarber.api.service.PagoSocioService;
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
@RequestMapping("/api/backoffice/pagos-socio")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PagoSocioBackofficeController {

    private final PagoSocioService pagoSocioService;
    private final UsuarioInternoRepository usuarioInternoRepository;

    @PostMapping
    public ResponseEntity<PagoSocioResponse> registrar(@Valid @RequestBody PagoSocioRequest request, Authentication authentication) {
        UsuarioInternoDetails d = (UsuarioInternoDetails) authentication.getPrincipal();
        UsuarioInterno usuario = usuarioInternoRepository.getReferenceById(d.getUsuarioInternoId());
        PagoSocioResponse creado = pagoSocioService.registrar(request, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public List<PagoSocioResponse> listar(@RequestParam(required = false) UUID socioId, @RequestParam(required = false) UUID negocioId) {
        if (socioId != null) return pagoSocioService.listarDeSocio(socioId);
        if (negocioId != null) return pagoSocioService.listarDeNegocio(negocioId);
        throw new com.lavabarber.api.exception.BusinessException("Se requiere socioId o negocioId");
    }
}
