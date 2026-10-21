package com.lavabarber.api.entity.enums;

/**
 * Estado operativo real de un ReservaItem, marcado a mano por el empleado.
 * Mientras este en PENDIENTE, el progreso que ve el cliente se sigue
 * calculando automaticamente por tiempo transcurrido (ver ProgresoService).
 * En cuanto pasa a cualquier otro estado, ese calculo manual manda.
 */
public enum EstadoReservaItem {
    PENDIENTE,
    CONFIRMADO,
    EN_PROGRESO,
    FINALIZADO,
    NO_SHOW
}
