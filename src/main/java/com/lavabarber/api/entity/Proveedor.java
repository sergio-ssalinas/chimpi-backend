package com.lavabarber.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(nullable = false)
    private String nombre;

    /** RUC paraguayo del proveedor. Opcional (hay proveedores informales chicos). */
    private String ruc;

    private String telefono;
    private String email;
    private String direccion;

    @Builder.Default
    private boolean activo = true;
}
