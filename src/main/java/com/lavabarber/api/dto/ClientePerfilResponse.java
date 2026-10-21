package com.lavabarber.api.dto;

import com.lavabarber.api.entity.enums.Sexo;

import java.time.LocalDate;
import java.util.UUID;

/** Perfil del cliente logueado. No incluye el documento de identidad completo por defecto. */
public record ClientePerfilResponse(
        UUID id,
        String username,
        String nombre,
        String apellido,
        String direccion,
        LocalDate fechaNacimiento,
        Sexo sexo,
        boolean tieneDocumentoCargado,
        String telefono,
        String email
) {
}
