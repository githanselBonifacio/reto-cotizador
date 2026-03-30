package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateLocationsRequest(
        @NotNull Long version,
        @NotNull List<@Valid LocationRequest> locations
) {
}