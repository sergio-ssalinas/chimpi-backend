package com.lavabarber.api.service;

import com.lavabarber.api.dto.backoffice.PagoSocioRequest;
import com.lavabarber.api.dto.backoffice.PagoSocioResponse;
import com.lavabarber.api.entity.PagoSocio;
import com.lavabarber.api.entity.Socio;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.PagoSocioRepository;
import com.lavabarber.api.repository.SocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoSocioService {

    private final PagoSocioRepository pagoSocioRepository;
    private final SocioRepository socioRepository;

    @Transactional
    public PagoSocioResponse registrar(PagoSocioRequest request, UsuarioInterno usuario) {
        Socio socio = socioRepository.findById(request.socioId())
                .orElseThrow(() -> new ResourceNotFoundException("Socio no encontrado: " + request.socioId()));

        PagoSocio pago = PagoSocio.builder()
                .socio(socio).negocio(socio.getNegocio())
                .monto(request.monto()).fecha(request.fecha()).concepto(request.concepto())
                .metodoPago(request.metodoPago()).comprobanteNumero(request.comprobanteNumero())
                .usuarioInterno(usuario)
                .build();

        return toResponse(pagoSocioRepository.save(pago));
    }

    @Transactional(readOnly = true)
    public List<PagoSocioResponse> listarDeSocio(UUID socioId) {
        return pagoSocioRepository.findBySocioIdOrderByFechaDesc(socioId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PagoSocioResponse> listarDeNegocio(UUID negocioId) {
        return pagoSocioRepository.findByNegocioIdOrderByFechaDesc(negocioId).stream().map(this::toResponse).toList();
    }

    private PagoSocioResponse toResponse(PagoSocio p) {
        return new PagoSocioResponse(
                p.getId(), p.getSocio().getId(), p.getSocio().getNombre(), p.getMonto(), p.getFecha(), p.getConcepto(),
                p.getMetodoPago().name(), p.getComprobanteNumero(),
                p.getUsuarioInterno().getNombre() + " " + p.getUsuarioInterno().getApellido());
    }
}
