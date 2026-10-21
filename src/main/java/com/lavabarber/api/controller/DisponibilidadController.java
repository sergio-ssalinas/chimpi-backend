package com.lavabarber.api.controller;

import com.lavabarber.api.dto.HorarioDisponibleResponse;
import com.lavabarber.api.dto.HorarioComboResponse;
import com.lavabarber.api.service.DisponibilidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    /**
     * GET /api/disponibilidad?servicioId=...&fecha=2026-08-10&empleadoId=... (opcional)
     * Si no se manda empleadoId, devuelve los horarios libres de TODOS los
     * empleados que realizan ese servicio (para que el cliente elija).
     */
    @GetMapping
    public List<HorarioDisponibleResponse> horariosDisponibles(
            @RequestParam UUID servicioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) UUID empleadoId) {
        return disponibilidadService.horariosDisponibles(servicioId, fecha, empleadoId);
    }

    /**
     * Para servicios Mixto (combo): devuelve las horas donde TODOS los
     * componentes tienen empleado libre en simultaneo. Es obligatorio
     * usar este endpoint (no el de arriba) para servicios combo.
     */
    @GetMapping("/combo")
    public List<HorarioComboResponse> horariosCombo(
            @RequestParam UUID servicioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return disponibilidadService.horariosComboDisponibles(servicioId, fecha);
    }
}
