package com.lavabarber.api.controller;

import com.lavabarber.api.dto.*;
import com.lavabarber.api.entity.Cliente;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.repository.ClienteRepository;
import com.lavabarber.api.security.ClienteUserDetails;
import com.lavabarber.api.security.DocumentoHasher;
import com.lavabarber.api.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final DocumentoHasher documentoHasher;

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroClienteRequest request) {
        if (clienteRepository.existsByUsername(request.username())) {
            throw new BusinessException("Ese nombre de usuario ya esta en uso");
        }
        if (clienteRepository.findByTelefono(request.telefono()).isPresent()) {
            throw new BusinessException("Ya existe una cuenta con ese telefono");
        }

        Cliente cliente = Cliente.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nombre(request.nombre())
                .apellido(request.apellido())
                .direccion(request.direccion())
                .fechaNacimiento(request.fechaNacimiento())
                .sexo(request.sexo())
                .documentoIdentidad(request.documentoIdentidad())
                .documentoHash(documentoHasher.hash(request.documentoIdentidad()))
                .telefono(request.telefono())
                .email(request.email())
                .build();

        cliente = clienteRepository.save(cliente);

        String token = jwtService.generarTokenCliente(cliente.getId(), cliente.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, cliente.getId(), cliente.getUsername(), cliente.getNombre()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (Exception e) {
            throw new BadCredentialsException("Usuario o contrasena incorrectos");
        }

        Cliente cliente = clienteRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contrasena incorrectos"));

        String token = jwtService.generarTokenCliente(cliente.getId(), cliente.getUsername());
        return new AuthResponse(token, cliente.getId(), cliente.getUsername(), cliente.getNombre());
    }

    @GetMapping("/me")
    public ClientePerfilResponse perfil(Authentication authentication) {
        ClienteUserDetails principal = (ClienteUserDetails) authentication.getPrincipal();
        Cliente cliente = clienteRepository.findById(principal.getClienteId())
                .orElseThrow(() -> new BadCredentialsException("Cliente no encontrado"));

        return new ClientePerfilResponse(
                cliente.getId(),
                cliente.getUsername(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getDireccion(),
                cliente.getFechaNacimiento(),
                cliente.getSexo(),
                cliente.getDocumentoIdentidad() != null,
                cliente.getTelefono(),
                cliente.getEmail());
    }
}
