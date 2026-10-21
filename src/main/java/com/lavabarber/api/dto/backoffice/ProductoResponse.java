package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.TasaIva;
import com.lavabarber.api.entity.enums.UnidadMedida;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductoResponse(
        UUID id,
        UUID negocioId,
        CategoriaProductoResponse categoria,
        String nombre,
        String descripcion,
        String codigoBarras,
        UnidadMedida unidadMedida,
        BigDecimal precioVenta,
        TasaIva tasaIva,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        boolean activo,
        boolean stockBajo
) {
}
