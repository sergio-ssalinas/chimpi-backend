package com.lavabarber.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Genera y valida JWT tanto para clientes (app de reservas) como para
 * personal interno (backoffice). Se distinguen por el claim "tipo"
 * ("CLIENTE" o "INTERNO") — ver JwtAuthenticationFilter, que decide
 * contra que UserDetailsService autenticar segun ese claim.
 */
@Component
public class JwtService {

    @Value("${app.jwt-secret:cambia-este-secreto-en-produccion-por-uno-largo-y-aleatorio}")
    private String secretoConfigurado;

    @Value("${app.jwt-expiration-horas:720}") // 30 dias por defecto
    private long horasExpiracion;

    private SecretKey obtenerClave() {
        try {
            // HMAC-SHA256 necesita al menos 256 bits; se deriva de la clave configurada
            // para que funcione sin importar su longitud original.
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] claveBytes = sha256.digest(secretoConfigurado.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(claveBytes);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo derivar la clave JWT", e);
        }
    }

    /** Token para un cliente (app de reservas). */
    public String generarTokenCliente(UUID clienteId, String username) {
        return generarToken(username, Map.of(
                "tipo", "CLIENTE",
                "clienteId", clienteId.toString()));
    }

    /** Token para personal interno (backoffice). El rol viaja en el claim "rol". */
    public String generarTokenInterno(UUID usuarioInternoId, String username, String rol) {
        return generarToken(username, Map.of(
                "tipo", "INTERNO",
                "usuarioInternoId", usuarioInternoId.toString(),
                "rol", rol));
    }

    private String generarToken(String subject, Map<String, Object> claims) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + horasExpiracion * 3600_000);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(obtenerClave())
                .compact();
    }

    public Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerUsername(String token) {
        return parsearClaims(token).getSubject();
    }

    /** "CLIENTE" o "INTERNO". Los tokens de cliente emitidos antes de este cambio no tienen este claim: se tratan como CLIENTE. */
    public String extraerTipo(String token) {
        String tipo = parsearClaims(token).get("tipo", String.class);
        return tipo != null ? tipo : "CLIENTE";
    }

    public UUID extraerClienteId(String token) {
        return UUID.fromString(parsearClaims(token).get("clienteId", String.class));
    }

    public UUID extraerUsuarioInternoId(String token) {
        return UUID.fromString(parsearClaims(token).get("usuarioInternoId", String.class));
    }

    public boolean esValido(String token) {
        try {
            Claims claims = parsearClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
