package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.RolInterno;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Cuenta de acceso al backoffice (personal interno: administradores,
 * empleados y cajeros). Distinta de Cliente (quien reserva) y de
 * Empleado (el recurso agendable: el barbero/lavador en si). Un
 * UsuarioInterno con rol EMPLEADO puede vincularse a un Empleado para
 * saber "cual es su propia agenda" cuando se necesite (fase 2).
 */
@Entity
@Table(name = "usuarios_internos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioInterno {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolInterno rol;

    @Builder.Default
    private boolean activo = true;

    /** Solo tiene sentido para rol EMPLEADO. Null para ADMIN y CAJERO. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;
}
