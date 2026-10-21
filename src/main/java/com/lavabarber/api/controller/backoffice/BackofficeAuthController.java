package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.AuthInternoResponse;
import com.lavabarber.api.dto.LoginInternoRequest;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.repository.UsuarioInternoRepository;
import com.lavabarber.api.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backoffice/auth")
@RequiredArgsConstructor
public class BackofficeAuthController {

    private final UsuarioInternoRepository usuarioInternoRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public AuthInternoResponse login(@Valid @RequestBody LoginInternoRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (Exception e) {
            throw new BadCredentialsException("Usuario o contrasena incorrectos");
        }

        UsuarioInterno usuario = usuarioInternoRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contrasena incorrectos"));

        if (!usuario.isActivo()) {
            throw new BadCredentialsException("Este usuario esta desactivado");
        }

        String token = jwtService.generarTokenInterno(usuario.getId(), usuario.getUsername(), usuario.getRol().name());
        return new AuthInternoResponse(
                token, usuario.getId(), usuario.getUsername(), usuario.getNombre(), usuario.getApellido(), usuario.getRol().name(),
                usuario.getEmpleado() != null ? usuario.getEmpleado().getId() : null);
    }
}
