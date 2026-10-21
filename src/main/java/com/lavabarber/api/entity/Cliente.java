package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.Sexo;
import com.lavabarber.api.security.DocumentoIdentidadConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    /** Hash BCrypt de la contrasena. Nunca se guarda en texto plano. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    private String direccion;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    /**
     * Numero de cedula, guardado encriptado (AES-256) via DocumentoIdentidadConverter.
     * Opcional en el registro inicial; se puede completar despues.
     */
    @Convert(converter = DocumentoIdentidadConverter.class)
    @Column(name = "documento_identidad")
    private String documentoIdentidad;

    /** Opcional: un cliente cargado en mostrador puede no dejar telefono. Unico cuando existe. */
    @Column(unique = true)
    private String telefono;

    /** RUC para facturar (ej: "1234567-8"). Texto plano: es un dato fiscal que va impreso en la factura. */
    @Column(length = 20)
    private String ruc;

    /**
     * HMAC-SHA256 de la cedula normalizada. Permite buscar por cedula (exacta) aunque
     * la cedula este cifrada con salt aleatorio. Ver DocumentoHasher.
     */
    @Column(name = "documento_hash", length = 64)
    private String documentoHash;

    /** Baja logica: un cliente inactivo no aparece en buscadores ni puede iniciar sesion en la app. */
    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    private String email;
}
