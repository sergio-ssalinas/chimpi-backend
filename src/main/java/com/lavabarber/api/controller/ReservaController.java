package com.lavabarber.api.controller;

import com.lavabarber.api.dto.ReservaRequest;
import com.lavabarber.api.dto.ReservaResponse;
import com.lavabarber.api.security.ClienteUserDetails;
import com.lavabarber.api.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    private UUID clienteIdDe(Authentication authentication) {
        return ((ClienteUserDetails) authentication.getPrincipal()).getClienteId();
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest request, Authentication authentication) {
        ReservaResponse creada = reservaService.crearReserva(clienteIdDe(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable UUID id, Authentication authentication) {
        return reservaService.obtenerReserva(clienteIdDe(authentication), id);
    }

    /** Las reservas del cliente autenticado (ya no se recibe el clienteId por URL). */
    @GetMapping("/mis-reservas")
    public List<ReservaResponse> misReservas(Authentication authentication) {
        return reservaService.reservasDeCliente(clienteIdDe(authentication));
    }

    @PostMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable UUID id, Authentication authentication) {
        return reservaService.cancelarReserva(clienteIdDe(authentication), id);
    }
}
