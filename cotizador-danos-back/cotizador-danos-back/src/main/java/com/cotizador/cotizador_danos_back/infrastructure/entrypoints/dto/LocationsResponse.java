package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion;
import java.util.List;

public record LocationsResponse(
        List<Ubicacion> locations,
        Long version
) {
}
