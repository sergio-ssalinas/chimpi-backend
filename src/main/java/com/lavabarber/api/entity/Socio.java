package com.lavabarber.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "socios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Socio {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(nullable = false)
    private String nombre;

    /** Porcentaje de 0 a 100. La suma de los socios activos no puede pasar de 100. */
    @Column(name = "porcentaje_participacion", nullable = false)
    private BigDecimal porcentajeParticipacion;

    private String telefono;
    private String email;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Builder.Default
    private boolean activo = true;
}
