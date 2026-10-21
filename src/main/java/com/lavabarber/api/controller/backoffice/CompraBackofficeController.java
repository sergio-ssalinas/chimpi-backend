package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.CompraRequest;
import com.lavabarber.api.dto.backoffice.CompraResponse;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.repository.UsuarioInternoRepository;
import com.lavabarber.api.security.UsuarioInternoDetails;
import com.lavabarber.api.service.CompraService;
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
@RequestMapping("/api/backoffice/compras")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CompraBackofficeController {

    private final CompraService compraService;
    private final UsuarioInternoRepository usuarioInternoRepository;

    @GetMapping
    public List<CompraResponse> listar(@RequestParam UUID negocioId) {
        return compraService.listar(negocioId);
    }

    @PostMapping
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody CompraRequest request, Authentication authentication) {
        UUID usuarioId = ((UsuarioInternoDetails) authentication.getPrincipal()).getUsuarioInternoId();
        UsuarioInterno usuario = usuarioInternoRepository.getReferenceById(usuarioId);
        CompraResponse creada = compraService.registrar(request, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}
