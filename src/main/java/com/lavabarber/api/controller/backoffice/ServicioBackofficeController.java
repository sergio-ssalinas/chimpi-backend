package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.RubroResponse;
import com.lavabarber.api.dto.ServicioResponse;
import com.lavabarber.api.dto.backoffice.EtapaServicioRequest;
import com.lavabarber.api.dto.backoffice.EtapaServicioResponse;
import com.lavabarber.api.dto.backoffice.ServicioBackofficeRequest;
import com.lavabarber.api.entity.EtapaServicio;
import com.lavabarber.api.entity.Negocio;
import com.lavabarber.api.entity.Rubro;
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
@RequestMapping("/api/backoffice/servicios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ServicioBackofficeController {

    private final ServicioRepository servicioRepository;
    private final NegocioRepository negocioRepository;
    private final RubroRepository rubroRepository;
    private final EtapaServicioRepository etapaServicioRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','CAJERO')")
    public List<ServicioResponse> listar(@RequestParam UUID negocioId) {
        return servicioRepository.findByNegocioId(negocioId).stream().map(this::toResponse).toList();
    }

    @PostMapping
    public ServicioResponse crear(@Valid @RequestBody ServicioBackofficeRequest request) {
        Servicio servicio = new Servicio();
        servicio.setEmpleados(new HashSet<>()); // @Builder.Default no aplica fuera del builder
        aplicarDatos(servicio, request);
        return toResponse(servicioRepository.save(servicio));
    }

    @PutMapping("/{id}")
    public ServicioResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ServicioBackofficeRequest request) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + id));
        aplicarDatos(servicio, request);
        return toResponse(servicioRepository.save(servicio));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {
        if (!servicioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Servicio no encontrado: " + id);
        }
        try {
            servicioRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("No se puede eliminar: el servicio tiene reservas, empleados o combos asociados");
        }
    }

    @GetMapping("/{id}/etapas")
    public List<EtapaServicioResponse> listarEtapas(@PathVariable UUID id) {
        return etapaServicioRepository.findByServicioIdOrderByOrdenAsc(id).stream()
                .map(e -> new EtapaServicioResponse(e.getId(), e.getOrden(), e.getNombre(), e.getPorcentajeHasta()))
                .toList();
    }

    /** Reemplaza TODAS las etapas del servicio por la lista enviada (borra y vuelve a crear, mas simple que hacer un diff). */
    @PutMapping("/{id}/etapas")
    public List<EtapaServicioResponse> reemplazarEtapas(@PathVariable UUID id, @Valid @RequestBody List<EtapaServicioRequest> etapas) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado: " + id));

        List<EtapaServicio> actuales = etapaServicioRepository.findByServicioIdOrderByOrdenAsc(id);
        etapaServicioRepository.deleteAll(actuales);

        List<EtapaServicio> nuevas = etapas.stream()
                .map(e -> EtapaServicio.builder()
                        .servicio(servicio)
                        .orden(e.orden())
                        .nombre(e.nombre())
                        .porcentajeHasta(e.porcentajeHasta())
                        .build())
                .toList();
        etapaServicioRepository.saveAll(nuevas);

        return nuevas.stream()
                .map(e -> new EtapaServicioResponse(e.getId(), e.getOrden(), e.getNombre(), e.getPorcentajeHasta()))
                .toList();
    }

    private void aplicarDatos(Servicio servicio, ServicioBackofficeRequest request) {
        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));
        Rubro rubro = rubroRepository.findById(request.rubroId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubro no encontrado: " + request.rubroId()));

        servicio.setNegocio(negocio);
        servicio.setRubro(rubro);
        servicio.setNombre(request.nombre());
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setPrecio(request.precio());
        servicio.setTasaIva(request.tasaIva() != null ? request.tasaIva() : com.lavabarber.api.entity.enums.TasaIva.DIEZ);

        Set<Servicio> componentes = new HashSet<>();
        if (request.componentesIds() != null && !request.componentesIds().isEmpty()) {
            if (request.componentesIds().contains(servicio.getId())) {
                throw new BusinessException("Un servicio no puede ser componente de si mismo");
            }
            componentes.addAll(servicioRepository.findAllById(request.componentesIds()));
        }
        servicio.setComponentes(componentes);
    }

    private ServicioResponse toResponse(Servicio s) {
        return new ServicioResponse(
                s.getId(),
                s.getNegocio().getId(),
                new RubroResponse(s.getRubro().getId(), s.getRubro().getNombre()),
                s.getNombre(),
                s.getDuracionMinutos(),
                s.getPrecio(),
                s.getTasaIva(),
                s.esCombo(),
                s.getComponentes().stream().map(this::toResponse).toList());
    }
}
