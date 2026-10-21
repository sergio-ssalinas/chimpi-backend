package com.lavabarber.api.controller;

import com.lavabarber.api.dto.ProgresoReservaItemResponse;
import com.lavabarber.api.security.ClienteUserDetails;
import com.lavabarber.api.service.ProgresoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/progreso")
@RequiredArgsConstructor
public class ProgresoController {

    private final ProgresoService progresoService;

    private UUID clienteIdDe(Authentication authentication) {
        return ((ClienteUserDetails) authentication.getPrincipal()).getClienteId();
    }

    /** Progreso de los servicios "en curso hoy" del cliente logueado. */
    @GetMapping("/activo")
    public List<ProgresoReservaItemResponse> activo(Authentication authentication) {
        return progresoService.progresoActivoDeCliente(clienteIdDe(authentication));
    }

    /** Progreso detallado de una reserva puntual (debe ser del cliente logueado). */
    @GetMapping("/reserva/{reservaId}")
    public List<ProgresoReservaItemResponse> deReserva(@PathVariable UUID reservaId, Authentication authentication) {
        return progresoService.progresoDeReserva(clienteIdDe(authentication), reservaId);
    }
}
