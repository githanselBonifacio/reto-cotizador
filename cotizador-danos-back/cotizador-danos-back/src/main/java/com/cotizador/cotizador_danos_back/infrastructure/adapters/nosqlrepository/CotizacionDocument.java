package com.cotizador.cotizador_danos_back.infrastructure.adapters.nosqlrepository;

import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion.DatosAsegurado;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion.DatosConduccion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion.PrimaPorUbicacion;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quotes")
public class CotizacionDocument {
    @Id
    private String numeroFolio;
    private String estadoCotizacion;
    private DatosAsegurado datosAsegurado;
    private DatosConduccion datosConduccion;
    private List<Ubicacion> locations;
    private Map<String, Object> configuracionLayout;
    private List<String> opcionesCobertura;
    private BigDecimal primaNeta;
    private BigDecimal primaComercial;
    private List<PrimaPorUbicacion> primasPorUbicacion;
    @Version
    private Long version;
    private LocalDateTime fechaUltimaActualizacion;
}
