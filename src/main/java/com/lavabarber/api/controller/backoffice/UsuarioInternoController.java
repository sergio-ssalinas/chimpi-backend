package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.UsuarioInternoRequest;
import com.lavabarber.api.dto.backoffice.UsuarioInternoResponse;
import com.lavabarber.api.entity.Empleado;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.entity.enums.RolInterno;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.EmpleadoRepository;
import com.lavabarber.api.repository.UsuarioInternoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Alta/gestion de las cuentas de acceso al backoffice (administradores y, a futuro, empleados). Solo un ADMIN puede tocar esto. */
@RestController
@RequestMapping("/api/backoffice/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioInternoController {

    private final UsuarioInternoRepository usuarioInternoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public List<UsuarioInternoResponse> listar() {
        return usuarioInternoRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    public UsuarioInternoResponse crear(@Valid @RequestBody UsuarioInternoRequest request) {
        if (usuarioInternoRepository.existsByUsername(request.username())) {
            throw new BusinessException("Ese nombre de usuario ya esta en uso");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("La contrasena es obligatoria al crear un usuario");
        }

        UsuarioInterno usuario = new UsuarioInterno();
        usuario.setUsername(request.username());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setActivo(true);
        aplicarDatos(usuario, request);

        return toResponse(usuarioInternoRepository.save(usuario));
    }

    @PutMapping("/{id}")
    public UsuarioInternoResponse actualizar(@PathVariable UUID id, @Valid @RequestBody UsuarioInternoRequest request) {
        UsuarioInterno usuario = usuarioInternoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));

        if (!usuario.getUsername().equals(request.username()) && usuarioInternoRepository.existsByUsername(request.username())) {
            throw new BusinessException("Ese nombre de usuario ya esta en uso");
        }
        usuario.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        aplicarDatos(usuario, request);

        return toResponse(usuarioInternoRepository.save(usuario));
    }

    /** No se borra (evita perder trazabilidad de quien hizo que): se desactiva. */
    @DeleteMapping("/{id}")
    public UsuarioInternoResponse desactivar(@PathVariable UUID id) {
        UsuarioInterno usuario = usuarioInternoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        usuario.setActivo(false);
        return toResponse(usuarioInternoRepository.save(usuario));
    }

    private void aplicarDatos(UsuarioInterno usuario, UsuarioInternoRequest request) {
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setRol(request.rol());
        if (request.activo() != null) {
            usuario.setActivo(request.activo());
        }

        if (request.rol() == RolInterno.EMPLEADO && request.empleadoId() != null) {
            Empleado empleado = empleadoRepository.findById(request.empleadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + request.empleadoId()));
            usuario.setEmpleado(empleado);
        } else {
            usuario.setEmpleado(null);
        }
    }

    private UsuarioInternoResponse toResponse(UsuarioInterno u) {
        return new UsuarioInternoResponse(
                u.getId(),
                u.getUsername(),
                u.getNombre(),
                u.getApellido(),
                u.getRol().name(),
                u.isActivo(),
                u.getEmpleado() != null ? u.getEmpleado().getId() : null,
                u.getEmpleado() != null ? u.getEmpleado().getNombre() : null);
    }
}
