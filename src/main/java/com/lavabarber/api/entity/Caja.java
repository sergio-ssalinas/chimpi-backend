package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.EstadoCaja;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una sesion de caja: la abre un usuario con un monto inicial (fondo), y
 * la cierra el mismo, declarando cuanto conto fisicamente. La diferencia
 * contra lo que el sistema esperaba (fondo + ventas en efectivo de esta
 * caja) es el resultado del arqueo. Multiples cajas pueden estar abiertas
 * en simultaneo (una por cajero/turno) — cada una es independiente.
 */
@Entity
@Table(name = "cajas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caja {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_interno_id")
    private UsuarioInterno usuarioInterno;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "monto_inicial", nullable = false)
    private BigDecimal montoInicial;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /** Lo que el cajero cuenta fisicamente al cerrar. */
    @Column(name = "monto_final_declarado")
    private BigDecimal montoFinalDeclarado;

    /** Calculado por el sistema: montoInicial + ventas en efectivo de esta caja. */
    @Column(name = "monto_final_esperado")
    private BigDecimal montoFinalEsperado;

    /** declarado - esperado. Positivo = sobrante, negativo = faltante. */
    private BigDecimal diferencia;

    private String observaciones;
}
