package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateCoverageOptionsRequest(
        @NotNull Long version,
        @NotNull List<String> opcionesCobertura
) {
}
