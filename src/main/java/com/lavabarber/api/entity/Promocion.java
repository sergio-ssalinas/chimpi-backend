package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.AlcancePromocion;
import com.lavabarber.api.entity.enums.TipoPromocion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "promociones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promocion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPromocion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlcancePromocion alcance;

    /** Porcentaje o monto segun 'tipo'. Null para DOS_POR_UNO. */
    private BigDecimal valor;

    /** Requerido si alcance = PRODUCTO. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    /** Requerido si alcance = SERVICIO. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    /** Opcional: si esta, la promocion es exclusiva de ese cliente (descuento por cliente). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Ambas opcionales; null = sin limite por ese lado. */
    @Column(name = "fecha_desde")
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta")
    private LocalDate fechaHasta;

    @Builder.Default
    private boolean activa = true;
}
