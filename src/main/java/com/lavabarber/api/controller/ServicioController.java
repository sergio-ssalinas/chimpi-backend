package com.lavabarber.api.controller;

import com.lavabarber.api.dto.RubroResponse;
import com.lavabarber.api.dto.ServicioResponse;
import com.lavabarber.api.entity.Servicio;
import com.lavabarber.api.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioRepository servicioRepository;

    @GetMapping
    public List<ServicioResponse> listarPorNegocio(@RequestParam UUID negocioId) {
        return servicioRepository.findByNegocioId(negocioId).stream().map(this::toResponse).toList();
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
