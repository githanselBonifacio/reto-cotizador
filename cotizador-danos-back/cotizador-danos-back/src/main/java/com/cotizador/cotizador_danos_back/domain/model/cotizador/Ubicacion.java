package com.cotizador.cotizador_danos_back.domain.model.cotizador;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion {
    private Integer indice;
    private String nombreUbicacion;
    private String direccion;
    private String codigoPostal;
    private String giro;
    private String claveIncendio;
    private BigDecimal buildingValue;
    private BigDecimal contentsValue;
    private List<String> garantias;
    private List<String> alertasBloqueantes;
    private EstadoValidacion estadoValidacion;

    public enum EstadoValidacion {
        INCOMPLETE, VALID, PENDIENTE, VALIDADA, RECHAZADA
    }
}
