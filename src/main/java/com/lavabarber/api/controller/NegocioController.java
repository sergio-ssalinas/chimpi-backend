package com.lavabarber.api.controller;

import com.lavabarber.api.dto.NegocioResponse;
import com.lavabarber.api.entity.Negocio;
import com.lavabarber.api.repository.NegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/negocios")
@RequiredArgsConstructor
public class NegocioController {

    private final NegocioRepository negocioRepository;

    @GetMapping
    public List<NegocioResponse> listar() {
        return negocioRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    public NegocioResponse crear(@RequestBody Negocio negocio) {
        return toResponse(negocioRepository.save(negocio));
    }

    private NegocioResponse toResponse(Negocio n) {
        return new NegocioResponse(n.getId(), n.getNombre(), n.getDireccion(), n.getTelefono());
    }
}
