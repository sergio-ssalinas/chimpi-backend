package com.lavabarber.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "empleados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empleado {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(nullable = false)
    private String nombre;

    private String fotoUrl;

    @Builder.Default
    private boolean activo = true;

    /** Servicios que este empleado puede realizar (ej: solo cortes, o cortes + lavado). */
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "empleado_servicios",
            joinColumns = @JoinColumn(name = "empleado_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id")
    )
    private Set<Servicio> servicios = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Disponibilidad> disponibilidad = new HashSet<>();
}
