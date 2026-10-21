package com.lavabarber.api.dto.backoffice;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AvanzarEtapaRequest(@NotNull UUID etapaId) {
}
