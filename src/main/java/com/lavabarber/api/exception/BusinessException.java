package com.lavabarber.api.exception;

/** Excepcion para errores de reglas de negocio (ej: horario no disponible). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
