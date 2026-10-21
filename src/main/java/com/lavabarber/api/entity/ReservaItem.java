package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.EstadoReservaItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Una linea dentro de una Reserva: un servicio puntual, con un empleado
 * y un horario. Varios items en la misma Reserva permiten combinar
 * servicios (ej: lavadero + barberia) en simultaneo o en horarios distintos.
 */
@Entity
@Table(name = "reserva_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /** Estado operativo real, marcado a mano por el empleado (fase 2). */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EstadoReservaItem estado = EstadoReservaItem.PENDIENTE;

    /** Etapa actual marcada a mano (ej: "Secado"). Null si no se avanzo ninguna a mano todavia. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_actual_id")
    private EtapaServicio etapaActual;

    /** Momento real en que el empleado confirmo la presencia del cliente (no el horario reservado). */
    @Column(name = "hora_inicio_real")
    private LocalDateTime horaInicioReal;

    /** Momento real en que el empleado marco el servicio como finalizado. */
    @Column(name = "hora_fin_real")
    private LocalDateTime horaFinReal;
}
