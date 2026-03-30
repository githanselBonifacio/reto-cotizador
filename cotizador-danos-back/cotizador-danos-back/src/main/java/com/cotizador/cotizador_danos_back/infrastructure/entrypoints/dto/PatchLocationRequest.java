package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto;

import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record PatchLocationRequest(
        @NotNull Long version,
        String nombreUbicacion,
        String direccion,
        String codigoPostal,
        String giro,
        String claveIncendio,
        BigDecimal buildingValue,
        BigDecimal contentsValue,
        List<String> garantias,
        List<String> alertasBloqueantes,
        Ubicacion.EstadoValidacion estadoValidacion
) {
}
