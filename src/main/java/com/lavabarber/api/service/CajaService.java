package com.lavabarber.api.service;

import com.lavabarber.api.dto.backoffice.AbrirCajaRequest;
import com.lavabarber.api.dto.backoffice.CajaResponse;
import com.lavabarber.api.dto.backoffice.CerrarCajaRequest;
import com.lavabarber.api.entity.Caja;
import com.lavabarber.api.entity.Negocio;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.entity.enums.EstadoCaja;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.CajaRepository;
import com.lavabarber.api.repository.EgresoRepository;
import com.lavabarber.api.repository.NegocioRepository;
import com.lavabarber.api.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CajaService {

    private final CajaRepository cajaRepository;
    private final VentaRepository ventaRepository;
    private final EgresoRepository egresoRepository;
    private final NegocioRepository negocioRepository;

    @Transactional
    public CajaResponse abrir(AbrirCajaRequest request, UsuarioInterno usuario) {
        cajaRepository.findByUsuarioInternoIdAndEstado(usuario.getId(), EstadoCaja.ABIERTA)
                .ifPresent(c -> {
                    throw new BusinessException("Ya tenes una caja abierta. Cerrala antes de abrir otra.");
                });

        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));

        Caja caja = Caja.builder()
                .negocio(negocio)
                .usuarioInterno(usuario)
                .fechaApertura(LocalDateTime.now())
                .montoInicial(request.montoInicial())
                .estado(EstadoCaja.ABIERTA)
                .build();

        return toResponse(cajaRepository.save(caja));
    }

    @Transactional(readOnly = true)
    public CajaResponse obtenerActual(UUID usuarioInternoId) {
        Caja caja = cajaRepository.findByUsuarioInternoIdAndEstado(usuarioInternoId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ResourceNotFoundException("No tenes una caja abierta"));
        return toResponse(caja);
    }

    @Transactional
    public CajaResponse cerrar(UUID cajaId, CerrarCajaRequest request, UUID usuarioSolicitanteId, boolean esAdmin) {
        Caja caja = obtenerConPermiso(cajaId, usuarioSolicitanteId, esAdmin);

        if (caja.getEstado() == EstadoCaja.CERRADA) {
            throw new BusinessException("Esta caja ya esta cerrada");
        }

        BigDecimal totalEfectivoVentas = ventaRepository.totalEfectivoDeCaja(cajaId);
        BigDecimal totalEfectivoEgresos = egresoRepository.totalEfectivoDeCaja(cajaId);
        BigDecimal esperado = caja.getMontoInicial().add(totalEfectivoVentas).subtract(totalEfectivoEgresos);
        BigDecimal diferencia = request.montoFinalDeclarado().subtract(esperado);

        caja.setEstado(EstadoCaja.CERRADA);
        caja.setFechaCierre(LocalDateTime.now());
        caja.setMontoFinalDeclarado(request.montoFinalDeclarado());
        caja.setMontoFinalEsperado(esperado);
        caja.setDiferencia(diferencia);
        caja.setObservaciones(request.observaciones());

        return toResponse(cajaRepository.save(caja));
    }

    @Transactional(readOnly = true)
    public List<CajaResponse> listar(UUID negocioId, EstadoCaja estadoFiltro) {
        List<Caja> cajas = estadoFiltro != null
                ? cajaRepository.findByNegocioIdAndEstadoOrderByFechaAperturaDesc(negocioId, estadoFiltro)
                : cajaRepository.findByNegocioIdOrderByFechaAperturaDesc(negocioId);
        return cajas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CajaResponse obtenerPorId(UUID cajaId, UUID usuarioSolicitanteId, boolean esAdmin) {
        return toResponse(obtenerConPermiso(cajaId, usuarioSolicitanteId, esAdmin));
    }

    private Caja obtenerConPermiso(UUID cajaId, UUID usuarioSolicitanteId, boolean esAdmin) {
        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada: " + cajaId));
        if (!esAdmin && !caja.getUsuarioInterno().getId().equals(usuarioSolicitanteId)) {
            throw new AccessDeniedException("No tenes permiso sobre esta caja");
        }
        return caja;
    }

    private CajaResponse toResponse(Caja c) {
        return new CajaResponse(
                c.getId(), c.getNegocio().getId(), c.getUsuarioInterno().getId(),
                c.getUsuarioInterno().getNombre() + " " + c.getUsuarioInterno().getApellido(),
                c.getFechaApertura(), c.getMontoInicial(), c.getEstado().name(), c.getFechaCierre(),
                c.getMontoFinalDeclarado(), c.getMontoFinalEsperado(), c.getDiferencia(), c.getObservaciones());
    }
}
