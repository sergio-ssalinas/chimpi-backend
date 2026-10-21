package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.ReservaResponse;
import com.lavabarber.api.dto.backoffice.AvanzarEtapaRequest;
import com.lavabarber.api.dto.backoffice.ReservaBackofficeRequest;
import com.lavabarber.api.dto.backoffice.ReasignarTurnoRequest;
import com.lavabarber.api.dto.backoffice.ReservaItemOperativoResponse;
import com.lavabarber.api.security.UsuarioInternoDetails;
import com.lavabarber.api.service.ReservaOperativaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Operacion del dia a dia. A diferencia de los demas controllers de
 * backoffice (solo ADMIN), estos endpoints los usan tanto ADMIN como
 * EMPLEADO — cada EMPLEADO solo puede operar sus propios turnos, validado
 * en ReservaOperativaService.
 */
@RestController
@RequestMapping("/api/backoffice/reservas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
public class ReservaOperativaController {

    private final ReservaOperativaService reservaOperativaService;

    private UsuarioInternoDetails detallesDe(Authentication authentication) {
        return (UsuarioInternoDetails) authentication.getPrincipal();
    }

    /**
     * Turnos del dia para un negocio. Si quien pregunta es EMPLEADO, se le
     * fuerza el filtro a su propio empleadoId (aunque no lo mande, o mande
     * otro) para que no pueda ver la agenda de otro empleado por esta via.
     */
    @GetMapping
    public List<ReservaItemOperativoResponse> delDia(
            @RequestParam UUID negocioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) UUID empleadoId,
            Authentication authentication) {
        UsuarioInternoDetails detalles = detallesDe(authentication);
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();

        UUID filtroEmpleado = detalles.getRol().equals("EMPLEADO") ? detalles.getEmpleadoId() : empleadoId;
        return reservaOperativaService.listarDelDia(negocioId, fechaConsulta, filtroEmpleado);
    }

    /** Agenda un turno para un cliente (ver ReservaOperativaService.crearParaCliente). */
    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaBackofficeRequest request, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        ReservaResponse creada = reservaOperativaService.crearParaCliente(request, d.getEmpleadoId(), d.getRol().equals("ADMIN"));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PostMapping("/items/{itemId}/confirmar")
    public ReservaItemOperativoResponse confirmar(@PathVariable UUID itemId, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        return reservaOperativaService.confirmar(itemId, d.getEmpleadoId(), d.getRol().equals("ADMIN"));
    }

    @PutMapping("/items/{itemId}/etapa")
    public ReservaItemOperativoResponse avanzarEtapa(
            @PathVariable UUID itemId, @Valid @RequestBody AvanzarEtapaRequest request, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        return reservaOperativaService.avanzarEtapa(itemId, request.etapaId(), d.getEmpleadoId(), d.getRol().equals("ADMIN"));
    }

    @PostMapping("/items/{itemId}/finalizar")
    public ReservaItemOperativoResponse finalizar(@PathVariable UUID itemId, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        return reservaOperativaService.finalizar(itemId, d.getEmpleadoId(), d.getRol().equals("ADMIN"));
    }

    @PostMapping("/items/{itemId}/marcar-ausente")
    public ReservaItemOperativoResponse marcarAusente(@PathVariable UUID itemId, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        return reservaOperativaService.marcarAusente(itemId, d.getEmpleadoId(), d.getRol().equals("ADMIN"));
    }

    @PutMapping("/items/{itemId}/reasignar")
    public ReservaItemOperativoResponse reasignar(
            @PathVariable UUID itemId, @Valid @RequestBody ReasignarTurnoRequest request, Authentication authentication) {
        UsuarioInternoDetails d = detallesDe(authentication);
        return reservaOperativaService.reasignar(
                itemId, request.nuevoEmpleadoId(), request.nuevaHoraInicio(), d.getEmpleadoId(), d.getRol().equals("ADMIN"));
    }
}
