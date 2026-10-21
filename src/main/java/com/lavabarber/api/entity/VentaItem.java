package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.TipoItemVenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "venta_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoItemVenta tipo;

    /** Exactamente uno de estos dos segun 'tipo'. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    /** Snapshot del nombre al momento de la venta (por si el catalogo cambia despues). */
    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    /** Subtotal BRUTO (cantidad x precio), antes del descuento. */
    @Column(nullable = false)
    private BigDecimal subtotal;

    /** Descuento de promocion aplicado a esta linea. El neto es subtotal - descuento. */
    @Builder.Default
    @Column(nullable = false)
    private BigDecimal descuento = BigDecimal.ZERO;
}
