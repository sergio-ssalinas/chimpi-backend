package com.lavabarber.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Una etapa dentro del progreso de un servicio (ej: "Secado" para Lavado,
 * o "Corte" para Barberia). El orden y el porcentajeHasta definen en que
 * tramo del tiempo reservado cae cada etapa.
 */
@Entity
@Table(name = "etapas_servicio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapaServicio {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private String nombre;

    /** Porcentaje acumulado (0-100) en el que ESTA etapa termina. */
    @Column(name = "porcentaje_hasta", nullable = false)
    private Integer porcentajeHasta;
}
