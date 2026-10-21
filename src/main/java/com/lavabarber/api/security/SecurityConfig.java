package com.lavabarber.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Un solo filtro/esquema de JWT sirve para dos "mundos" de usuarios:
 * Cliente (app de reservas) y UsuarioInterno (backoffice). Cada uno tiene
 * su propio DaoAuthenticationProvider, y el AuthenticationManager se arma
 * a mano con un ProviderManager que prueba ambos en orden.
 *
 * IMPORTANTE: el AuthenticationManager se construye DIRECTO con
 * "new ProviderManager(...)", sin pasar por AuthenticationConfiguration.
 * Delegar en AuthenticationConfiguration.getAuthenticationManager() aca
 * genera un StackOverflowError (el manager termina llamandose a si mismo
 * en loop) cuando ademas hay varios AuthenticationProvider en juego.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize("hasRole('ADMIN')") en los controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final ClienteUserDetailsService clienteUserDetailsService;
    private final UsuarioInternoDetailsService usuarioInternoDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors-allowed-origins:http://localhost:3000,http://localhost:5173}")
    private List<String> origenesPermitidos;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenesPermitidos);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider clienteAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(clienteUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public DaoAuthenticationProvider usuarioInternoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioInternoDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(clienteAuthenticationProvider(), usuarioInternoAuthenticationProvider()));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Publicos: login/registro de clientes, login del backoffice, y
                        // navegar el catalogo (negocios/servicios/empleados/disponibilidad).
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/backoffice/auth/**").permitAll()
                        .requestMatchers("GET", "/api/negocios/**").permitAll()
                        .requestMatchers("GET", "/api/servicios/**").permitAll()
                        .requestMatchers("GET", "/api/empleados/**").permitAll()
                        .requestMatchers("GET", "/api/disponibilidad/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // El resto de /api/backoffice/** exige estar logueado como
                        // personal interno; el rol especifico (ADMIN/EMPLEADO) se
                        // valida por metodo con @PreAuthorize en cada controller.
                        .requestMatchers("/api/backoffice/**").authenticated()
                        // Todo lo demas (reservas, perfil de cliente) requiere JWT valido.
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // necesario para la consola H2
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
