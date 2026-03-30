package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import java.util.Map;

public record LocationsLayoutResponse(
        Map<String, Object> configuracionLayout,
        Long version
) {
}
