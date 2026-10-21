package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.dto.ReservaItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Turno agendado por el personal en nombre de un cliente. A diferencia de la app, aca el cliente
 * viaja en el body (lo elige quien agenda); la validacion de horarios y choques es la misma.
 */
public record ReservaBackofficeRequest(
        @NotNull UUID clienteId,
        @NotNull UUID negocioId,
        @NotNull LocalDate fecha,
        @NotEmpty @Valid List<ReservaItemRequest> items
) {
}
