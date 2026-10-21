package com.lavabarber.api.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encripta el numero de documento (cedula) antes de guardarlo en la base,
 * y lo desencripta al leerlo. Usa AES-256-GCM. La clave se deriva de
 * app.encryption-key (variable de entorno APP_ENCRYPTION_KEY en produccion).
 *
 * IMPORTANTE: si perdes la clave, los documentos guardados quedan
 * irrecuperables (no hay forma de "resetear" un dato encriptado).
 */
@Converter
@Component
public class DocumentoIdentidadConverter implements AttributeConverter<String, String> {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAM_TAG_GCM = 128;
    private static final int TAM_IV = 12;

    @Value("${app.encryption-key:cambia-esta-clave-en-produccion-1234}")
    private String claveConfigurada;

    private SecretKeySpec obtenerClave() {
        try {
            // Deriva una clave AES-256 valida a partir de la clave configurada,
            // sin importar su longitud original.
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] claveBytes = sha256.digest(claveConfigurada.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(claveBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo derivar la clave de encriptacion", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String documentoPlano) {
        if (documentoPlano == null || documentoPlano.isBlank()) return null;
        try {
            byte[] iv = new byte[TAM_IV];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, obtenerClave(), new GCMParameterSpec(TAM_TAG_GCM, iv));
            byte[] cifrado = cipher.doFinal(documentoPlano.getBytes(StandardCharsets.UTF_8));

            // Se guarda IV + texto cifrado juntos, codificado en base64.
            byte[] resultado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(cifrado, 0, resultado, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("Error al encriptar el documento de identidad", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String valorEnBase) {
        if (valorEnBase == null || valorEnBase.isBlank()) return null;
        try {
            byte[] datos = Base64.getDecoder().decode(valorEnBase);
            byte[] iv = new byte[TAM_IV];
            System.arraycopy(datos, 0, iv, 0, TAM_IV);
            byte[] cifrado = new byte[datos.length - TAM_IV];
            System.arraycopy(datos, TAM_IV, cifrado, 0, cifrado.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, obtenerClave(), new GCMParameterSpec(TAM_TAG_GCM, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error al desencriptar el documento de identidad", e);
        }
    }
}
