package com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto;

import java.math.BigDecimal;

public record TechnicalTariffs(
        BigDecimal fireFactor,
        BigDecimal catFactor,
        BigDecimal fhmFactor,
        String fireKey,
        String catastropheZone
) {
}