package com.lavabarber.api.entity;

import com.lavabarber.api.entity.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Pago de utilidades (u otro concepto) a un socio puntual. */
@Entity
@Table(name = "pagos_socio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoSocio {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "socio_id")
    private Socio socio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    /** Ej: "Utilidades agosto 2026", "Adelanto", etc. */
    @Column(nullable = false)
    private String concepto;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "comprobante_numero")
    private String comprobanteNumero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_interno_id")
    private UsuarioInterno usuarioInterno;
}
