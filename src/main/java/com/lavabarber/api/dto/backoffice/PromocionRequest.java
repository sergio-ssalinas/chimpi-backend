package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.AlcancePromocion;
import com.lavabarber.api.entity.enums.TipoPromocion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PromocionRequest(
        @NotNull UUID negocioId,
        @NotBlank String nombre,
        @NotNull TipoPromocion tipo,
        @NotNull AlcancePromocion alcance,
        /** Requerido para PORCENTAJE y MONTO_FIJO; se ignora en DOS_POR_UNO. */
        BigDecimal valor,
        UUID productoId,
        UUID servicioId,
        /** Opcional: promocion exclusiva de este cliente. */
        UUID clienteId,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Boolean activa
) {
}
