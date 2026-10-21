package com.lavabarber.api.entity.enums;

/**
 * LOCAL es el unico estado alcanzable hoy: el comprobante se genera y
 * numera correctamente, pero no se transmite a ningun lado (no hay
 * integracion real con SIFEN todavia). ENVIADA/APROBADA/RECHAZADA quedan
 * preparados para cuando se conecte un proveedor certificado (e-Kuatia
 * via Web Services, o un proveedor como AdoqIA/EDICOM/FactAPI).
 */
public enum EstadoFactura {
    LOCAL,
    ENVIADA,
    APROBADA,
    RECHAZADA,
    ANULADA
}
