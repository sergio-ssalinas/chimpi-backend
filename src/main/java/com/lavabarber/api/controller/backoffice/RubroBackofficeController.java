package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.RubroResponse;
import com.lavabarber.api.dto.backoffice.RubroRequest;
import com.lavabarber.api.entity.Rubro;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.RubroRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/backoffice/rubros")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RubroBackofficeController {

    private final RubroRepository rubroRepository;

    @GetMapping
    public List<RubroResponse> listar() {
        return rubroRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    public RubroResponse crear(@Valid @RequestBody RubroRequest request) {
        Rubro rubro = Rubro.builder().nombre(request.nombre()).build();
        return toResponse(rubroRepository.save(rubro));
    }

    @PutMapping("/{id}")
    public RubroResponse actualizar(@PathVariable UUID id, @Valid @RequestBody RubroRequest request) {
        Rubro rubro = rubroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rubro no encontrado: " + id));
        rubro.setNombre(request.nombre());
        return toResponse(rubroRepository.save(rubro));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {
        if (!rubroRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rubro no encontrado: " + id);
        }
        try {
            rubroRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("No se puede eliminar: hay servicios que usan este rubro");
        }
    }

    private RubroResponse toResponse(Rubro r) {
        return new RubroResponse(r.getId(), r.getNombre());
    }
}
