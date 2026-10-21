package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.CategoriaEgresoRequest;
import com.lavabarber.api.dto.backoffice.CategoriaEgresoResponse;
import com.lavabarber.api.entity.CategoriaEgreso;
import com.lavabarber.api.entity.Negocio;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.CategoriaEgresoRepository;
import com.lavabarber.api.repository.NegocioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/backoffice/categorias-egreso")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaEgresoBackofficeController {

    private final CategoriaEgresoRepository categoriaEgresoRepository;
    private final NegocioRepository negocioRepository;

    @GetMapping
    public List<CategoriaEgresoResponse> listar(@RequestParam UUID negocioId) {
        return categoriaEgresoRepository.findByNegocioId(negocioId).stream().map(this::toResponse).toList();
    }

    @PostMapping
    public CategoriaEgresoResponse crear(@Valid @RequestBody CategoriaEgresoRequest request) {
        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));
        CategoriaEgreso categoria = CategoriaEgreso.builder().negocio(negocio).nombre(request.nombre()).build();
        return toResponse(categoriaEgresoRepository.save(categoria));
    }

    @PutMapping("/{id}")
    public CategoriaEgresoResponse actualizar(@PathVariable UUID id, @Valid @RequestBody CategoriaEgresoRequest request) {
        CategoriaEgreso categoria = categoriaEgresoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada: " + id));
        categoria.setNombre(request.nombre());
        return toResponse(categoriaEgresoRepository.save(categoria));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {
        if (!categoriaEgresoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Categoria no encontrada: " + id);
        }
        try {
            categoriaEgresoRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("No se puede eliminar: hay egresos que usan esta categoria");
        }
    }

    private CategoriaEgresoResponse toResponse(CategoriaEgreso c) {
        return new CategoriaEgresoResponse(c.getId(), c.getNombre());
    }
}
