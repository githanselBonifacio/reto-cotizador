package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import java.time.LocalDateTime;

public record QuoteStateResponse(
        String numeroFolio,
        String estadoCotizacion,
        Long version,
        LocalDateTime fechaUltimaActualizacion
) {
}
