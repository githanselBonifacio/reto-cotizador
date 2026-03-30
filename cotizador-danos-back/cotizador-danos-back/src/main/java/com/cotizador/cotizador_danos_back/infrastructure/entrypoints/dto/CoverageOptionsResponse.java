package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import java.util.List;

public record CoverageOptionsResponse(
        List<String> opcionesCobertura,
        Long version
) {
}
