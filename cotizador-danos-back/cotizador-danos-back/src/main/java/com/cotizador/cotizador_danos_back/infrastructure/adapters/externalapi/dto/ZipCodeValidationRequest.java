package com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZipCodeValidationRequest(
        @JsonProperty("zip_code")
        String zipCode
) {
}
