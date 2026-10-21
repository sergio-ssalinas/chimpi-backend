package com.lavabarber.api.controller;

import com.lavabarber.api.dto.EmpleadoResponse;
import com.lavabarber.api.entity.Empleado;
import com.lavabarber.api.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoRepository empleadoRepository;

    @GetMapping
    public List<EmpleadoResponse> listarPorNegocio(@RequestParam UUID negocioId) {
        return empleadoRepository.findByNegocioId(negocioId).stream().map(this::toResponse).toList();
    }

    private EmpleadoResponse toResponse(Empleado e) {
        return new EmpleadoResponse(e.getId(), e.getNombre(), e.getFotoUrl());
    }
}
