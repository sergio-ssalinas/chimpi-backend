package com.lavabarber.api.dto.backoffice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FacturaResponse(
        UUID id,
        UUID ventaId,
        UUID negocioId,
        String numeroFactura,
        LocalDateTime fechaEmision,
        String clienteRuc,
        String clienteNombre,
        String condicionVenta,
        BigDecimal baseGravada10,
        BigDecimal iva10,
        BigDecimal baseGravada5,
        BigDecimal iva5,
        BigDecimal exenta,
        BigDecimal total,
        String estado,
        String cdc,
        /**
         * true mientras el negocio no tenga RUC y timbrado cargados en Configuracion: en ese caso el
         * PDF lleva la leyenda "Comprobante interno". Con ambos cargados la leyenda desaparece.
         */
        boolean esModoLocal,
        List<FacturaLineaResponse> lineas,
        String emisorNombre,
        String emisorRuc,
        String emisorDireccion,
        String emisorTelefono,
        String timbradoNumero
) {
}
