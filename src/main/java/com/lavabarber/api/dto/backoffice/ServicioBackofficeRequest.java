package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.TasaIva;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ServicioBackofficeRequest(
        @NotNull UUID negocioId,
        @NotNull UUID rubroId,
        @NotBlank String nombre,
        @NotNull @Positive Integer duracionMinutos,
        BigDecimal precio,
        /** Si no se manda, queda en DIEZ (la tasa general). */
        TasaIva tasaIva,
        /** IDs de servicios atomicos que componen este servicio si es un combo (Mixto). Vacio o null si es atomico. */
        List<UUID> componentesIds
) {
}
