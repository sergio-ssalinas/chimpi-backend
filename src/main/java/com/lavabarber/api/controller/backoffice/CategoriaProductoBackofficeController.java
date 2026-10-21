package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.CategoriaProductoRequest;
import com.lavabarber.api.dto.backoffice.CategoriaProductoResponse;
import com.lavabarber.api.entity.CategoriaProducto;
import com.lavabarber.api.entity.Negocio;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.CategoriaProductoRepository;
import com.lavabarber.api.repository.NegocioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/backoffice/categorias-producto")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaProductoBackofficeController {

    private final CategoriaProductoRepository categoriaProductoRepository;
    private final NegocioRepository negocioRepository;

    @GetMapping
    public List<CategoriaProductoResponse> listar(@RequestParam UUID negocioId) {
        return categoriaProductoRepository.findByNegocioId(negocioId).stream().map(this::toResponse).toList();
    }

    @PostMapping
    public CategoriaProductoResponse crear(@Valid @RequestBody CategoriaProductoRequest request) {
        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));
        CategoriaProducto categoria = CategoriaProducto.builder().negocio(negocio).nombre(request.nombre()).build();
        return toResponse(categoriaProductoRepository.save(categoria));
    }

    @PutMapping("/{id}")
    public CategoriaProductoResponse actualizar(@PathVariable UUID id, @Valid @RequestBody CategoriaProductoRequest request) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada: " + id));
        categoria.setNombre(request.nombre());
        return toResponse(categoriaProductoRepository.save(categoria));
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {
        if (!categoriaProductoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Categoria no encontrada: " + id);
        }
        try {
            categoriaProductoRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("No se puede eliminar: hay productos que usan esta categoria");
        }
    }

    private CategoriaProductoResponse toResponse(CategoriaProducto c) {
        return new CategoriaProductoResponse(c.getId(), c.getNombre());
    }
}
