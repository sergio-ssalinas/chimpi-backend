package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.TasaIva;
import com.lavabarber.api.entity.enums.UnidadMedida;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaProducto categoria;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    /** Opcional: no todos los productos necesitan codigo de barras (ej: insumos internos). */
    @Column(name = "codigo_barras", unique = true)
    private String codigoBarras;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", nullable = false)
    private UnidadMedida unidadMedida;

    @Column(name = "precio_venta", nullable = false)
    private BigDecimal precioVenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tasa_iva", nullable = false)
    private TasaIva tasaIva;

    /**
     * BigDecimal (no entero) porque hay unidades fraccionables (kilos, litros).
     * Se actualiza SOLO a traves de StockService, nunca directo, para que
     * cada cambio quede en el historial de MovimientoStock.
     */
    @Builder.Default
    @Column(name = "stock_actual", nullable = false)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "stock_minimo", nullable = false)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Builder.Default
    private boolean activo = true;

    public boolean isStockBajo() {
        return stockActual.compareTo(stockMinimo) <= 0;
    }
}
