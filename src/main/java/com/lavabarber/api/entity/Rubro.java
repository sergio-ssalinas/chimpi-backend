package com.lavabarber.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Clasificacion de un servicio: Barberia, Lavadero, Mixto, o cualquier
 * rubro nuevo que se agregue en el futuro sin tocar codigo.
 */
@Entity
@Table(name = "rubros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rubro {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String nombre;
}
