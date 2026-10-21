package com.lavabarber.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * El clienteId ya NO viaja en el body: se toma del JWT autenticado
 * (ver ReservaController), para que un cliente no pueda crear reservas
 * a nombre de otro.
 */
public record ReservaRequest(
        @NotNull java.util.UUID negocioId,
        @NotNull LocalDate fecha,
        @NotEmpty @Valid List<ReservaItemRequest> items
) {
}
