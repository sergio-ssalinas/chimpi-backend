package com.lavabarber.api.dto.backoffice;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ReservaItemOperativoResponse(
        UUID itemId,
        UUID reservaId,
        String clienteNombre,
        String clienteTelefono,
        UUID servicioId,
        String servicioNombre,
        UUID empleadoId,
        String empleadoNombre,
        LocalTime horaInicio,
        LocalTime horaFin,
        String estado, // PENDIENTE, CONFIRMADO, EN_PROGRESO, FINALIZADO, NO_SHOW
        String etapaActualNombre,
        List<EtapaServicioResponse> etapasDisponibles
) {
}
