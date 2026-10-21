package com.lavabarber.api.dto.backoffice;

public record ConfiguracionFacturacionRequest(
        String ruc,
        String timbradoNumero,
        String puntoExpedicion
) {
}
