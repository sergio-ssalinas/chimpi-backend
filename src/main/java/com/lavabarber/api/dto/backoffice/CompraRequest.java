package com.lavabarber.api.dto.backoffice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CompraRequest(
        @NotNull UUID negocioId,
        @NotNull UUID proveedorId,
        @NotNull LocalDate fecha,
        String numeroFactura,
        @NotEmpty @Valid List<CompraItemRequest> items
) {
}
