package com.lavabarber.api.dto.backoffice;

/** Documento a imprimir en la factura de un cliente: su RUC o, si no tiene, su cedula. Puede ser null. */
public record DocumentoFacturaResponse(String documento) {
}
