package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

public record CalculateQuoteRequest(
        @NotNull Long version,
        @NotNull @JsonProperty("parametros_calculo") Map<String, BigDecimal> calculationParameters
) {
}