package com.lavabarber.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Ej: Insumos, Sueldos, Alquiler, Servicios publicos, Impuestos, Marketing, Otros. Configurable, no un enum fijo. */
@Entity
@Table(name = "categorias_egreso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaEgreso {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(nullable = false)
    private String nombre;
}
