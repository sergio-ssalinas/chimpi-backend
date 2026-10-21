package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta/edicion de un cliente desde el backoffice. Solo el nombre es obligatorio: para una empresa
 * se carga la razon social en "nombre" y se deja el apellido vacio.
 */
public record ClienteBackofficeRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 120) String apellido,
        @Size(max = 40) String telefono,
        /** Numero de cedula (se guarda cifrado). */
        @Size(max = 30) String documentoIdentidad,
        /** Ej: "1234567-8". */
        @Size(max = 20) String ruc,
        @Email @Size(max = 160) String email,
        @Size(max = 255) String direccion
) {
}
