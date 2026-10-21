package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.ProveedorRequest;
import com.lavabarber.api.dto.backoffice.ProveedorResponse;
import com.lavabarber.api.entity.Negocio;
import com.lavabarber.api.entity.Proveedor;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.NegocioRepository;
import com.lavabarber.api.repository.ProveedorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/backoffice/proveedores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProveedorBackofficeController {

    private final ProveedorRepository proveedorRepository;
    private final NegocioRepository negocioRepository;

    @GetMapping
    public List<ProveedorResponse> listar(@RequestParam UUID negocioId) {
        return proveedorRepository.findByNegocioId(negocioId).stream().map(this::toResponse).toList();
    }

    @PostMapping
    public ProveedorResponse crear(@Valid @RequestBody ProveedorRequest request) {
        Proveedor proveedor = new Proveedor();
        aplicarDatos(proveedor, request);
        return toResponse(proveedorRepository.save(proveedor));
    }

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ProveedorRequest request) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
        aplicarDatos(proveedor, request);
        return toResponse(proveedorRepository.save(proveedor));
    }

    /** Soft delete: se desactiva, no se borra (las compras historicas lo referencian). */
    @DeleteMapping("/{id}")
    public ProveedorResponse desactivar(@PathVariable UUID id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
        proveedor.setActivo(false);
        return toResponse(proveedorRepository.save(proveedor));
    }

    private void aplicarDatos(Proveedor proveedor, ProveedorRequest request) {
        Negocio negocio = negocioRepository.findById(request.negocioId())
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado: " + request.negocioId()));
        proveedor.setNegocio(negocio);
        proveedor.setNombre(request.nombre());
        proveedor.setRuc(request.ruc());
        proveedor.setTelefono(request.telefono());
        proveedor.setEmail(request.email());
        proveedor.setDireccion(request.direccion());
        if (request.activo() != null) {
            proveedor.setActivo(request.activo());
        } else if (proveedor.getId() == null) {
            proveedor.setActivo(true);
        }
    }

    private ProveedorResponse toResponse(Proveedor p) {
        return new ProveedorResponse(p.getId(), p.getNegocio().getId(), p.getNombre(), p.getRuc(), p.getTelefono(), p.getEmail(), p.getDireccion(), p.isActivo());
    }
}
