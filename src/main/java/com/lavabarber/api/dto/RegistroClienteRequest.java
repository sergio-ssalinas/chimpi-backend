package com.lavabarber.api.dto;

import com.lavabarber.api.entity.enums.Sexo;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegistroClienteRequest(
        @NotBlank @Size(min = 4, max = 30, message = "debe tener entre 4 y 30 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "solo se permiten letras, numeros, punto y guion bajo")
        String username,

        @NotBlank @Size(min = 8, message = "debe tener al menos 8 caracteres")
        String password,

        @NotBlank String nombre,
        @NotBlank String apellido,
        String direccion,
        LocalDate fechaNacimiento,
        Sexo sexo,

        /** Numero de cedula. Opcional en el registro inicial (ver README para el porque). */
        String documentoIdentidad,

        @NotBlank String telefono,
        @Email String email
) {
}
