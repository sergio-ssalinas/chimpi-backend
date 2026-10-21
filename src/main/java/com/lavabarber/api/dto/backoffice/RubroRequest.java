package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.NotBlank;

public record RubroRequest(@NotBlank String nombre) {
}
