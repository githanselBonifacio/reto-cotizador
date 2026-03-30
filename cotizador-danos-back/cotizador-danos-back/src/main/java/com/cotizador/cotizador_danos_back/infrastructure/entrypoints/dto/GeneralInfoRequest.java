package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion;
import jakarta.validation.constraints.NotNull;

public record GeneralInfoRequest(
        @NotNull Long version,
        Cotizacion.DatosAsegurado datosAsegurado,
        String codigoAgente
) {
}
