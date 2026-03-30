package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateLocationsLayoutRequest(
        @NotNull Long version,
        @NotNull Map<String, Object> configuracionLayout
) {
}
