package com.cotizador.cotizador_danos_back.domain.model.cotizador;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cotizacion {
    private String numeroFolio;
    private EstadoCotizacion estadoCotizacion;
    private DatosAsegurado datosAsegurado;
    private DatosConduccion datosConduccion;
    private List<Ubicacion> locations;
    private Map<String, Object> configuracionLayout;
    private List<String> opcionesCobertura;
    private BigDecimal primaNeta;
    private BigDecimal primaComercial;
    private List<PrimaPorUbicacion> primasPorUbicacion;
    private Long version;
    private LocalDateTime fechaUltimaActualizacion;

    public enum EstadoCotizacion {
        PENDING, PENDIENTE, CALCULATED, CALCULADO, ERROR
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatosAsegurado {
        private String nombre;
        private String rfc;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatosConduccion {
        private String codigoAgente;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrimaPorUbicacion {
        private Integer indice;
        private String nombreUbicacion;
        private BigDecimal incendio;
        private BigDecimal cat;
        private BigDecimal fhm;
        private BigDecimal primaNeta;
        private List<String> alertasBloqueantes;
    }
}
