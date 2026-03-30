package com.cotizador.cotizador_danos_back.domain.usecase;

import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.LocationsSummary;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

public interface CotizacionUseCase {
    Mono<Cotizacion> initializeQuote();
    Mono<Cotizacion> getQuote(String folio);
    Mono<Cotizacion> updateGeneralInfo(String folio, Long expectedVersion, Cotizacion.DatosAsegurado datosAsegurado, String codigoAgente);
    Mono<Cotizacion> updateLocations(String folio, Long expectedVersion, List<Ubicacion> locations);
    Mono<Cotizacion> patchLocation(String folio, Long expectedVersion, Integer indice, Ubicacion patch);
    Mono<Map<String, Object>> getLocationsLayout(String folio);
    Mono<Cotizacion> updateLocationsLayout(String folio, Long expectedVersion, Map<String, Object> layout);
    Mono<LocationsSummary> getLocationsSummary(String folio);
    Mono<List<String>> getCoverageOptions(String folio);
    Mono<Cotizacion> updateCoverageOptions(String folio, Long expectedVersion, List<String> coverageOptions);
    Mono<Cotizacion> calculateQuote(String folio, Long expectedVersion, Map<String, BigDecimal> calculationParameters);
}
