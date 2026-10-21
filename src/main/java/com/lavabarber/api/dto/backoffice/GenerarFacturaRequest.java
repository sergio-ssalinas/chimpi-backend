package com.lavabarber.api.dto.backoffice;

import com.lavabarber.api.entity.enums.CondicionVenta;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Todo es opcional salvo la condicion de venta. Resolucion de los datos del cliente:
 * - clienteNombre / clienteRuc escritos a mano ganan sobre todo lo demas.
 * - Si faltan, se toman del cliente elegido (clienteId) o, si no se elige, del cliente de la venta:
 *   RUC y, si no tiene, cedula.
 * - consumidorFinal = true ignora al cliente y factura como "Consumidor Final" sin documento.
 */
public record GenerarFacturaRequest(
        UUID clienteId,
        String clienteRuc,
        String clienteNombre,
        Boolean consumidorFinal,
        @NotNull CondicionVenta condicionVenta
) {
}
