package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.*;
import com.lavabarber.api.entity.Disponibilidad;
import com.lavabarber.api.entity.Empleado;
import com.lavabarber.api.entity.Negocio;
import com.lavabarber.api.entity.Servicio;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/backoffice/empleados")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmpleadoBackofficeController {

    private final EmpleadoRepository empleadoRepository;
    private final NegocioRepository negocioRepository;
    private final ServicioRepository servicioRepository;
    private final DisponibilidadRepository disponibilidadRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public List<EmpleadoBackofficeResponse> listar(@RequestParam UUID negocioId) {
        return empleadoRepository.findByNegocioId(negocioId).stream().map(this::toResponse).toList();
    }

    @PostMapping
    public EmpleadoBackofficeResponse crear(@Valid @RequestBody EmpleadoBackofficeRequest request) {
        Empleado empleado = new Empleado();
        empleado.setActivo(true);
        empleado.setDisponibilidad(new HashSet<>()); // @Builder.Default no aplica fuera del builder
        aplicarDatos(empleado, request);
        return toResponse(empleadoRepository.save(empleado));
    }

    @PutMapping("/{id}")
    public EmpleadoBackofficeResponse actualizar(@PathVariable UUID id, @Valid @RequestBody EmpleadoBackofficeRequest request) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + id));
        aplicarDatos(empleado, request);
        return toResponse(empleadoRepository.save(empleado));
    }

    /**
     * No se borra el empleado (rompe el historial de reservas ya hechas):
     * se desactiva, para que deje de aparecer como opcion al reservar.
     */
    @DeleteMapping("/{id}")
    public EmpleadoBackofficeResponse desactivar(@PathVariable UUID id) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + id));
        empleado.setActivo(false);
        return toResponse(empleadoRepository.save(empleado));
    }

    @GetMapping("/{id}/disponibilidad")
    public List<DisponibilidadResponse> listarDisponibilidad(@PathVariable UUID id) {
        if (!empleadoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Empleado no encontrado: " + id);
        }
        return disponibilidadRepository.findByEmpleadoIdOrderByDiaSemanaAscHoraInicioAsc(id).stream()
                .map(d -> new DisponibilidadResponse(d.getId(), d.getDiaSemana(), d.getHoraInicio(), d.getHoraFin()))
                .toList();
    }

    @PostMapping("/{id}/disponibilidad")
    public DisponibilidadResponse agregarDisponibilidad(@PathVariable UUID id, @Valid @RequestBody DisponibilidadRequest request) {
        Empleado empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + id));

        if (!request.horaInicio().isBefore(request.horaFin())) {
            throw new BusinessException("La hora de inicio debe ser anterior a la hora de fin");
        }

        Disponibilidad disponibilidad = Disponibilidad.builder()
                .empleado(empleado)
                .diaSemana(request.diaSemana())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .build();
        disponibilidad = disponibilidadRepository.save(disponibilidad);
        return new DisponibilidadResponse(disponibilidad.getId(), disponibilidad.getDiaSemana(), disponibilidad.getHoraInicio(), disponibilidad.getHoraFin());
    }

    @DeleteMapping("/disponibilidad/{disponibilidadId}")
    public void eliminarDisponibilidad(@PathVariable UUID disponibilidadId) {
        if (!disponibilidadRepository.existsById(disponibilidadId)) {
            throw new ResourceNotFoundException("Disponibilidad no encontrada: " + disponibilidadId);
        }
        disponibilidadRepository.deleteById(disponibilidadId);
    }

    private void aplicarDatos(Empleado empleado, EmpleadoBackofficeRequest request) {
        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));

        empleado.setNegocio(negocio);
        empleado.setNombre(request.nombre());
        empleado.setFotoUrl(request.fotoUrl());
        if (request.activo() != null) {
            empleado.setActivo(request.activo());
        }

        Set<Servicio> servicios = new HashSet<>();
        if (request.serviciosIds() != null && !request.serviciosIds().isEmpty()) {
            servicios.addAll(servicioRepository.findAllById(request.serviciosIds()));
        }
        empleado.setServicios(servicios);
    }

    private EmpleadoBackofficeResponse toResponse(Empleado e) {
        List<ServicioResumenResponse> servicios = e.getServicios().stream()
                .map(s -> new ServicioResumenResponse(s.getId(), s.getNombre(), s.esCombo()))
                .toList();
        return new EmpleadoBackofficeResponse(e.getId(), e.getNegocio().getId(), e.getNombre(), e.getFotoUrl(), e.isActivo(), servicios);
    }
}
