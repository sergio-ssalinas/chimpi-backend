package com.lavabarber.api.service;

import com.lavabarber.api.dto.backoffice.SocioRequest;
import com.lavabarber.api.dto.backoffice.SocioResponse;
import com.lavabarber.api.entity.Negocio;
import com.lavabarber.api.entity.Socio;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.NegocioRepository;
import com.lavabarber.api.repository.SocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocioService {

    private final SocioRepository socioRepository;
    private final NegocioRepository negocioRepository;

    @Transactional
    public SocioResponse crear(SocioRequest request) {
        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));

        validarPorcentaje(request.negocioId(), request.porcentajeParticipacion(), null);

        Socio socio = Socio.builder()
                .negocio(negocio).nombre(request.nombre())
                .porcentajeParticipacion(request.porcentajeParticipacion())
                .telefono(request.telefono()).email(request.email())
                .fechaIngreso(request.fechaIngreso())
                .activo(request.activo() == null || request.activo())
                .build();

        return toResponse(socioRepository.save(socio));
    }

    @Transactional
    public SocioResponse actualizar(UUID id, SocioRequest request) {
        Socio socio = socioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio no encontrado: " + id));

        boolean quedaActivo = request.activo() == null || request.activo();
        if (quedaActivo) {
            validarPorcentaje(request.negocioId(), request.porcentajeParticipacion(), id);
        }

        socio.setNombre(request.nombre());
        socio.setPorcentajeParticipacion(request.porcentajeParticipacion());
        socio.setTelefono(request.telefono());
        socio.setEmail(request.email());
        socio.setFechaIngreso(request.fechaIngreso());
        socio.setActivo(quedaActivo);

        return toResponse(socioRepository.save(socio));
    }

    /** Soft delete: se desactiva, no se borra (los pagos historicos lo referencian). */
    @Transactional
    public SocioResponse desactivar(UUID id) {
        Socio socio = socioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio no encontrado: " + id));
        socio.setActivo(false);
        return toResponse(socioRepository.save(socio));
    }

    @Transactional(readOnly = true)
    public List<SocioResponse> listar(UUID negocioId) {
        return socioRepository.findByNegocioId(negocioId).stream().map(this::toResponse).toList();
    }

    private void validarPorcentaje(UUID negocioId, BigDecimal porcentajeNuevo, UUID socioAExcluir) {
        UUID excluir = socioAExcluir != null ? socioAExcluir : UUID.randomUUID();
        BigDecimal sumaResto = socioRepository.sumaPorcentajesActivos(negocioId, excluir);
        BigDecimal total = sumaResto.add(porcentajeNuevo);

        if (total.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(
                    "La suma de participaciones activas quedaria en " + total + "% — no puede pasar de 100%");
        }
    }

    private SocioResponse toResponse(Socio s) {
        return new SocioResponse(
                s.getId(), s.getNegocio().getId(), s.getNombre(), s.getPorcentajeParticipacion(),
                s.getTelefono(), s.getEmail(), s.getFechaIngreso(), s.isActivo());
    }
}
