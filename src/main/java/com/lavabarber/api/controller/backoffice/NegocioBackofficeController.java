package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.ConfiguracionFacturacionRequest;
import com.lavabarber.api.dto.backoffice.NegocioConfigResponse;
import com.lavabarber.api.entity.Negocio;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.NegocioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/backoffice/negocio")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NegocioBackofficeController {

    private final NegocioRepository negocioRepository;

    @GetMapping("/{id}/facturacion")
    public NegocioConfigResponse verConfiguracion(@PathVariable UUID id) {
        return toResponse(obtener(id));
    }

    @PutMapping("/{id}/facturacion")
    public NegocioConfigResponse actualizarConfiguracion(@PathVariable UUID id, @Valid @RequestBody ConfiguracionFacturacionRequest request) {
        Negocio negocio = obtener(id);
        negocio.setRuc(request.ruc());
        negocio.setTimbradoNumero(request.timbradoNumero());
        negocio.setPuntoExpedicion(request.puntoExpedicion());
        return toResponse(negocioRepository.save(negocio));
    }

    private Negocio obtener(UUID id) {
        return negocioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + id));
    }

    private NegocioConfigResponse toResponse(Negocio n) {
        return new NegocioConfigResponse(n.getId(), n.getNombre(), n.getRuc(), n.getTimbradoNumero(), n.getPuntoExpedicion(), n.getProximoNumeroFactura());
    }
}
