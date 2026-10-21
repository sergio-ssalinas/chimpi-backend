package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.MetodoPago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PagoSocioRequest(
        @NotNull UUID socioId,
        @NotNull @Positive BigDecimal monto,
        @NotNull LocalDate fecha,
        @NotBlank String concepto,
        @NotNull MetodoPago metodoPago,
        String comprobanteNumero
) {
}
