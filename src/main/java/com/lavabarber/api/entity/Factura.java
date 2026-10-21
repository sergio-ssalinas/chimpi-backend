package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.CondicionVenta;
import com.lavabarber.api.entity.enums.EstadoFactura;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Comprobante generado a partir de una Venta. HOY solo alcanza el estado
 * LOCAL (ver EstadoFactura) — calcula todo correctamente (discriminacion
 * de IVA, numeracion con formato paraguayo) pero NO es un timbrado
 * electronico valido ante la SET hasta que se conecte un proveedor
 * certificado real. El campo cdc (Codigo de Control) queda null hasta
 * ese momento — lo asigna el SIFEN, no nosotros.
 */
@Entity
@Table(name = "facturas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", unique = true)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    /** Formato "001-001-0000001" (puntoExpedicion-numero), generado localmente. */
    @Column(name = "numero_factura", nullable = false, unique = true)
    private String numeroFactura;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    /** Null = consumidor final sin RUC (valido en Paraguay para montos chicos). */
    @Column(name = "cliente_ruc")
    private String clienteRuc;

    @Column(name = "cliente_nombre", nullable = false)
    private String clienteNombre;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "condicion_venta", nullable = false)
    private CondicionVenta condicionVenta = CondicionVenta.CONTADO;

    // Discriminacion de IVA: los precios ya incluyen el IVA (practica habitual
    // en Paraguay), asi que estos son la base gravada calculada "hacia atras".
    @Column(name = "base_gravada_10", nullable = false)
    private BigDecimal baseGravada10;

    @Column(name = "iva_10", nullable = false)
    private BigDecimal iva10;

    @Column(name = "base_gravada_5", nullable = false)
    private BigDecimal baseGravada5;

    @Column(name = "iva_5", nullable = false)
    private BigDecimal iva5;

    @Column(nullable = false)
    private BigDecimal exenta;

    @Column(nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EstadoFactura estado = EstadoFactura.LOCAL;

    /** Lo asigna el SIFEN real cuando haya integracion. Null en modo local. */
    private String cdc;

    /** Renglones impresos (combos desglosados). Foto inmutable de lo facturado. */
    @Builder.Default
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden asc")
    private List<FacturaLinea> lineas = new ArrayList<>();
}
