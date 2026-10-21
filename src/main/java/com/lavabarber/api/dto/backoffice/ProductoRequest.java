package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.TasaIva;
import com.lavabarber.api.entity.enums.UnidadMedida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductoRequest(
        @NotNull UUID negocioId,
        UUID categoriaId,
        @NotBlank String nombre,
        String descripcion,
        /** Opcional. Si se manda, debe ser unico. */
        String codigoBarras,
        @NotNull UnidadMedida unidadMedida,
        @NotNull @PositiveOrZero BigDecimal precioVenta,
        @NotNull TasaIva tasaIva,
        @PositiveOrZero BigDecimal stockMinimo,
        Boolean activo
) {
}
