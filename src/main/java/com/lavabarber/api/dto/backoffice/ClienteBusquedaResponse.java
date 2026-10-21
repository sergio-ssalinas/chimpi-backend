package com.lavabarber.api.dto.backoffice;

import java.util.UUID;

/** Datos minimos de un cliente para elegirlo en el POS. Sin cedula, email ni direccion. */
public record ClienteBusquedaResponse(UUID id, String nombre, String apellido, String telefono, String ruc) {
}
