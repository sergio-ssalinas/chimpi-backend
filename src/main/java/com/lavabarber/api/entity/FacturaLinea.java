package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.TasaIva;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Renglon impreso en la factura. Es una foto de lo vendido: un combo se desglosa aca en sus
 * componentes (cada uno con su precio de lista y su parte del descuento) aunque en la venta
 * figure como un solo item al precio del combo. Si despues se edita el combo o el catalogo,
 * la factura emitida no cambia.
 */
@Entity
@Table(name = "factura_lineas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaLinea {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id")
    private Factura factura;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal cantidad;

    /** Precio de lista por unidad (sin descuento). */
    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    /** Descuento total del renglon: parte del descuento del combo + parte de la promocion. */
    @Builder.Default
    @Column(nullable = false)
    private BigDecimal descuento = BigDecimal.ZERO;

    /** Importe NETO del renglon (lo realmente cobrado: cantidad x precio - descuento). Incluye IVA. */
    @Column(nullable = false)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(name = "tasa_iva", nullable = false)
    private TasaIva tasaIva;
}
