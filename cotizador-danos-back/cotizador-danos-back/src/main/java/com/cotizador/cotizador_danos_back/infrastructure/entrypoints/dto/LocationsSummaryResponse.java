package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import java.math.BigDecimal;

public record LocationsSummaryResponse(
        Integer totalLocations,
        Integer validLocations,
        Integer warningLocations,
        Integer totalBlockingAlerts,
        BigDecimal totalPrimaNeta,
        BigDecimal totalPrimaComercial
) {
}
