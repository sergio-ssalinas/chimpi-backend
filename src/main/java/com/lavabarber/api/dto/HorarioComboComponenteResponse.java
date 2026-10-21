package com.lavabarber.api.dto;

import java.util.List;
import java.util.UUID;

/** Un componente del combo (ej: "Corte de pelo") con los empleados libres a esa hora puntual. */
public record HorarioComboComponenteResponse(
        UUID servicioId,
        String servicioNombre,
        Integer duracionMinutos,
        List<EmpleadoResponse> empleadosDisponibles
) {
}
