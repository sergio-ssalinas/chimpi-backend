package com.lavabarber.api.dto.backoffice;

import java.util.UUID;

public record NegocioConfigResponse(
        UUID id,
        String nombre,
        String ruc,
        String timbradoNumero,
        String puntoExpedicion,
        Integer proximoNumeroFactura
) {
}
