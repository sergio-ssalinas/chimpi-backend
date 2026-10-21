package com.lavabarber.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * "Indice ciego" para buscar clientes por cedula sin guardarla en claro.
 *
 * La cedula se guarda cifrada con AES-GCM y un IV aleatorio (DocumentoIdentidadConverter), asi que
 * el mismo numero produce un texto distinto cada vez y no se puede buscar con un WHERE. Para poder
 * buscarla, ademas se guarda este HMAC-SHA256 del numero normalizado: igual entrada, igual hash,
 * y sin la clave no se puede volver al numero. Solo sirve para coincidencia EXACTA.
 *
 * Usa app.encryption-key (con otra derivacion que la del cifrado). Si cambia la clave hay que
 * recalcular los hashes de los clientes existentes.
 */
@Component
public class DocumentoHasher {

    @Value("${app.encryption-key:cambia-esta-clave-en-produccion-1234}")
    private String claveConfigurada;

    /** Deja solo letras y numeros en mayusculas: "4.123.456" y "4123456" dan el mismo resultado. */
    public static String normalizar(String documento) {
        if (documento == null) return null;
        String limpio = documento.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
        return limpio.isEmpty() ? null : limpio;
    }

    /** Hash hexadecimal (64 caracteres) o null si no hay documento. */
    public String hash(String documento) {
        String normalizado = normalizar(documento);
        if (normalizado == null) return null;
        try {
            byte[] clave = MessageDigest.getInstance("SHA-256")
                    .digest(("hash-documento:" + claveConfigurada).getBytes(StandardCharsets.UTF_8));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(normalizado.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash del documento", e);
        }
    }
}
