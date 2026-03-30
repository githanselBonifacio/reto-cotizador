package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record LocationRequest(
        @NotNull Integer index,
        @JsonProperty(value = "locationName", defaultValue = "")
        String locationName,
        @JsonProperty(value = "address", defaultValue = "")
        String address,
        @JsonProperty(value = "zipCode", defaultValue = "")
        String zipCode,
        @JsonProperty(value = "state", defaultValue = "")
        String state,
        @JsonProperty(value = "municipality", defaultValue = "")
        String municipality,
        @JsonProperty(value = "neighborhood", defaultValue = "")
        String neighborhood,
        @JsonProperty(value = "city", defaultValue = "")
        String city,
        @JsonProperty(value = "constructionType", defaultValue = "")
        String constructionType,
        Integer level,
        Integer constructionYear,
        @JsonProperty(value = "giro", defaultValue = "")
        String giro,
        @JsonProperty(value = "fireKey", defaultValue = "")
        String fireKey,
        List<String> coverages,
        @JsonProperty(value = "insuredAmount", defaultValue = "0")
        BigDecimal insuredAmount,
        @JsonProperty(value = "contentsValue", defaultValue = "0")
        BigDecimal contentsValue
) {
}