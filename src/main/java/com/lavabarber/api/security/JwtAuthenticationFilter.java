package com.lavabarber.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee el header "Authorization: Bearer <token>" en cada request, valida el
 * JWT y autentica contra el UserDetailsService que corresponda segun el
 * claim "tipo" del token (CLIENTE -> ClienteUserDetailsService, INTERNO ->
 * UsuarioInternoDetailsService). Asi un mismo mecanismo de JWT sirve tanto
 * para la app de clientes como para el backoffice.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ClienteUserDetailsService clienteUserDetailsService;
    private final UsuarioInternoDetailsService usuarioInternoDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (jwtService.esValido(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = jwtService.extraerUsername(token);
            String tipo = jwtService.extraerTipo(token);

            UserDetails userDetails = tipo.equals("INTERNO")
                    ? usuarioInternoDetailsService.loadUserByUsername(username)
                    : clienteUserDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
