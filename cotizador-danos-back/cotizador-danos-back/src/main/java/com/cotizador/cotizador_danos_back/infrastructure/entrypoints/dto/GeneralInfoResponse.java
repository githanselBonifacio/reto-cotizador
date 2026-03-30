package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion;

public record GeneralInfoResponse(
        Cotizacion.DatosAsegurado datosAsegurado,
        String codigoAgente,
        Long version
) {
}
