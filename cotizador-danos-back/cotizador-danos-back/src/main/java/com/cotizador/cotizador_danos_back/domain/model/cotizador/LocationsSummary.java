package com.cotizador.cotizador_danos_back.domain.model.cotizador;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationsSummary {
    private Integer totalLocations;
    private Integer validLocations;
    private Integer warningLocations;
    private Integer totalBlockingAlerts;
    private BigDecimal totalPrimaNeta;
    private BigDecimal totalPrimaComercial;
}
