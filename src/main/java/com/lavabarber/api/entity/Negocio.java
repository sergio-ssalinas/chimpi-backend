package com.lavabarber.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "negocios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Negocio {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    private String direccion;

    private String telefono;

    /** RUC del negocio. Null hasta que se complete el tramite ante la SET. */
    private String ruc;

    /**
     * Configuracion de facturacion "modo local" (ver Factura). Quedan null
     * hasta que se cargue un timbrado real. puntoExpedicion en formato
     * "001-001"; proximoNumeroFactura arranca en 1 y se auto-incrementa
     * con cada factura generada.
     */
    @Column(name = "timbrado_numero")
    private String timbradoNumero;

    @Column(name = "punto_expedicion")
    private String puntoExpedicion;

    @Builder.Default
    @Column(name = "proximo_numero_factura")
    private Integer proximoNumeroFactura = 1;
}
