package com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto;

public record ZipCodeValidationResult(
        boolean valid,
        String state,
        String municipality,
        String city,
        String neighborhood,
        String catastropheZone
) {
}