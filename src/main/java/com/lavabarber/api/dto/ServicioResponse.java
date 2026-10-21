package com.lavabarber.api.dto;

import com.lavabarber.api.entity.enums.TasaIva;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ServicioResponse(
        UUID id,
        UUID negocioId,
        RubroResponse rubro,
        String nombre,
        Integer duracionMinutos,
        BigDecimal precio,
        TasaIva tasaIva,
        boolean combo,
        List<ServicioResponse> componentes
) {
}
