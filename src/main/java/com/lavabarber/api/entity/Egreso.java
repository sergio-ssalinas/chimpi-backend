package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.EstadoEgreso;
import com.lavabarber.api.entity.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cualquier pago que hace la empresa: insumos, sueldos, alquiler,
 * servicios publicos, impuestos, etc. Si se paga en EFECTIVO y esta
 * atado a una Caja (cajaId no nulo), entra al arqueo de esa caja —
 * resta del efectivo esperado al cerrarla. Si no esta atado a una caja
 * (ej: pago por transferencia de un alquiler), no afecta ningun arqueo.
 */
@Entity
@Table(name = "egresos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Egreso {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private CategoriaEgreso categoria;

    @Column(nullable = false)
    private String concepto;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    /** Solo tiene sentido si metodoPago = EFECTIVO y sale de una caja abierta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id")
    private Caja caja;

    @Column(name = "comprobante_numero")
    private String comprobanteNumero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_interno_id")
    private UsuarioInterno usuarioInterno;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EstadoEgreso estado = EstadoEgreso.ACTIVO;
}
