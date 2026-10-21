package com.lavabarber.api.dto;

import java.time.LocalTime;
import java.util.List;

/**
 * Una hora de inicio en la que TODOS los componentes del combo tienen
 * al menos un empleado libre en simultaneo (ej: Juan libre para el corte
 * Y Pedro libre para el lavado, ambos a las 10:00).
 */
public record HorarioComboResponse(
        LocalTime horaInicio,
        List<HorarioComboComponenteResponse> componentes
) {
}
