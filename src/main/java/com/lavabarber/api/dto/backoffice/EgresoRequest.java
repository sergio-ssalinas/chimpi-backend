package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.MetodoPago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EgresoRequest(
        @NotNull UUID negocioId,
        @NotNull UUID categoriaId,
        @NotBlank String concepto,
        @NotNull @Positive BigDecimal monto,
        @NotNull LocalDate fecha,
        @NotNull MetodoPago metodoPago,
        UUID proveedorId,
        UUID empleadoId,
        UUID cajaId,
        String comprobanteNumero
) {
}
