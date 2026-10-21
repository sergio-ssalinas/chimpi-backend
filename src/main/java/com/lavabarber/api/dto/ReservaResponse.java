package com.lavabarber.api.dto;

import com.lavabarber.api.entity.enums.EstadoReserva;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReservaResponse(
        UUID id,
        UUID clienteId,
        UUID negocioId,
        LocalDate fecha,
        EstadoReserva estado,
        List<ReservaItemResponse> items
) {
}
