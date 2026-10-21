package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record VentaRequest(
        @NotNull UUID cajaId,
        /** Opcional: venta de mostrador sin cliente identificado. */
        UUID clienteId,
        @NotNull MetodoPago metodoPago,
        /** Opcional: promocion vigente elegida por el cajero. El descuento lo calcula el backend. */
        UUID promocionId,
        @NotEmpty @Valid List<VentaItemRequest> items
) {
}
