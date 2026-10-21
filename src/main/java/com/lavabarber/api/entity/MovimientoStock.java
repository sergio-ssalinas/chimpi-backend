package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.TipoMovimientoStock;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Historial de cada cambio de stock (entrada por compra, salida por venta,
 * ajuste manual). Nunca se edita ni se borra: es el registro auditable de
 * por que el stock de un producto quedo en tal numero.
 */
@Entity
@Table(name = "movimientos_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoStock {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimientoStock tipo;

    /** Con signo: positivo si sumo stock, negativo si resto. */
    @Column(nullable = false)
    private BigDecimal cantidad;

    /** Snapshot del stock resultante DESPUES de este movimiento (para no tener que recalcular el historico). */
    @Column(name = "stock_resultante", nullable = false)
    private BigDecimal stockResultante;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Motivo (ajustes manuales) o referencia (ej: "Compra a Proveedor X"). */
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_interno_id")
    private UsuarioInterno usuarioInterno;
}
