package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.TasaIva;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "servicios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubro_id")
    private Rubro rubro;

    @Column(nullable = false)
    private String nombre;

    /** Duracion estimada del servicio, usada para calcular horarios disponibles. */
    @Column(name = "duracion_min", nullable = false)
    private Integer duracionMinutos;

    private BigDecimal precio;

    /** Para poder discriminar IVA en una factura que mezcle productos y servicios. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "tasa_iva", nullable = false)
    private TasaIva tasaIva = TasaIva.DIEZ;

    @Builder.Default
    @ManyToMany(mappedBy = "servicios")
    private Set<Empleado> empleados = new HashSet<>();

    /**
     * Servicios atomicos que componen este servicio cuando es un combo
     * (rubro Mixto), ej: "Combo VIP" = Corte + Lavado. Si esta vacio,
     * el servicio es atomico y se reserva con un solo empleado/horario
     * como siempre. Si tiene componentes, CADA componente necesita su
     * propio empleado y se reservan todos a la misma hora de inicio.
     */
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "servicio_componentes",
            joinColumns = @JoinColumn(name = "servicio_id"),
            inverseJoinColumns = @JoinColumn(name = "componente_id")
    )
    private Set<Servicio> componentes = new HashSet<>();

    public boolean esCombo() {
        return componentes != null && !componentes.isEmpty();
    }
}
