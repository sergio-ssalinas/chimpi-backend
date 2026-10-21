package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.AlcancePromocion;
import com.lavabarber.api.entity.enums.TipoPromocion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PromocionResponse(
        UUID id,
        UUID negocioId,
        String nombre,
        TipoPromocion tipo,
        AlcancePromocion alcance,
        BigDecimal valor,
        UUID productoId,
        String productoNombre,
        UUID servicioId,
        String servicioNombre,
        UUID clienteId,
        String clienteNombre,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        boolean activa,
        /** activa Y dentro de fechaDesde/fechaHasta hoy. */
        boolean vigente
) {
}
