package com.cotizador.cotizador_danos_back.infrastructure.entrypoints;

import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion;
import com.cotizador.cotizador_danos_back.domain.usecase.CotizacionUseCase;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.CalculateQuoteRequest;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.CoverageOptionsResponse;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.GeneralInfoRequest;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.GeneralInfoResponse;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.LocationRequest;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.LocationsLayoutResponse;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.LocationsResponse;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.LocationsSummaryResponse;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.PatchLocationRequest;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.QuoteStateResponse;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.UpdateCoverageOptionsRequest;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.UpdateLocationsLayoutRequest;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.dto.UpdateLocationsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Quotes", description = "Reactive operations for property damage quotes")
public class CotizacionController {
    private final CotizacionUseCase cotizacionUseCase;

    @Operation(summary = "Create a folio", description = "Requests a new folio from Core OHS and initializes the quote in MongoDB")
    @PostMapping("/folios")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Cotizacion> createFolio() {
        return cotizacionUseCase.initializeQuote();
    }

    @Operation(summary = "Get general info", description = "Returns insured and agent information for a quote")
    @GetMapping("/quotes/{folio}/general-info")
    public Mono<GeneralInfoResponse> getGeneralInfo(@Parameter(description = "Quote folio") @PathVariable String folio) {
        return cotizacionUseCase.getQuote(folio)
                .map(quote -> new GeneralInfoResponse(
                        quote.getDatosAsegurado(),
                        quote.getDatosConduccion() != null ? quote.getDatosConduccion().getCodigoAgente() : null,
                        quote.getVersion()
                ));
    }

    @Operation(summary = "Update general info", description = "Updates insured and agent information for a quote")
    @PutMapping("/quotes/{folio}/general-info")
    public Mono<Cotizacion> updateGeneralInfo(@Parameter(description = "Quote folio") @PathVariable String folio,
                                              @Valid @RequestBody GeneralInfoRequest request) {
        return cotizacionUseCase.updateGeneralInfo(folio, request.version(), request.datosAsegurado(), request.codigoAgente());
    }

    @Operation(summary = "Get locations", description = "Returns all locations for a quote")
    @GetMapping("/quotes/{folio}/locations")
    public Mono<LocationsResponse> getLocations(@Parameter(description = "Quote folio") @PathVariable String folio) {
        return cotizacionUseCase.getQuote(folio)
                .map(quote -> new LocationsResponse(quote.getLocations(), quote.getVersion()));
    }

    @Operation(summary = "Get locations layout", description = "Retrieves the UI layout configuration for locations")
    @GetMapping("/quotes/{folio}/locations/layout")
    public Mono<LocationsLayoutResponse> getLocationsLayout(@Parameter(description = "Quote folio") @PathVariable String folio) {
        return Mono.zip(cotizacionUseCase.getLocationsLayout(folio), cotizacionUseCase.getQuote(folio))
                .map(tuple -> new LocationsLayoutResponse(tuple.getT1(), tuple.getT2().getVersion()));
    }

    @Operation(summary = "Update locations layout", description = "Updates the UI layout configuration for locations")
    @PutMapping("/quotes/{folio}/locations/layout")
    public Mono<Cotizacion> updateLocationsLayout(@Parameter(description = "Quote folio") @PathVariable String folio,
                                                  @Valid @RequestBody UpdateLocationsLayoutRequest request) {
        return cotizacionUseCase.updateLocationsLayout(folio, request.version(), request.configuracionLayout());
    }

    @Operation(summary = "Get locations summary", description = "Returns financial and validation summary for all locations")
    @GetMapping({"/quotes/{folio}/locations/summary", "/quotes/{folio}/summary"})
    public Mono<LocationsSummaryResponse> getLocationsSummary(@Parameter(description = "Quote folio") @PathVariable String folio) {
        return cotizacionUseCase.getLocationsSummary(folio)
                .map(summary -> new LocationsSummaryResponse(
                        summary.getTotalLocations(),
                        summary.getValidLocations(),
                        summary.getWarningLocations(),
                        summary.getTotalBlockingAlerts(),
                        summary.getTotalPrimaNeta(),
                        summary.getTotalPrimaComercial()
                ));
    }

    @Operation(summary = "Replace locations", description = "Replaces the entire list of locations for a quote")
    @PutMapping("/quotes/{folio}/locations")
    public Mono<Cotizacion> putLocations(@Parameter(description = "Quote folio") @PathVariable String folio,
                                         @Valid @RequestBody UpdateLocationsRequest request) {
        return cotizacionUseCase.updateLocations(folio, request.version(), mapLocations(request.locations()));
    }

    @Operation(summary = "Patch location", description = "Partially updates one location by index")
    @PatchMapping("/quotes/{folio}/locations/{index}")
    public Mono<Cotizacion> patchLocation(@Parameter(description = "Quote folio") @PathVariable String folio,
                                          @Parameter(description = "Location index") @PathVariable Integer index,
                                          @Valid @RequestBody PatchLocationRequest request) {
        return cotizacionUseCase.patchLocation(folio, request.version(), index, mapPatchLocation(index, request));
    }

    @Operation(summary = "Calculate quote", description = "Triggers the quote calculation engine and persists the results")
    @PostMapping("/quotes/{folio}/calculate")
    public Mono<Cotizacion> calculateQuote(@Parameter(description = "Quote folio") @PathVariable String folio,
                                           @Valid @RequestBody CalculateQuoteRequest request) {
        return cotizacionUseCase.calculateQuote(folio, request.version(), request.calculationParameters());
    }

    @Operation(summary = "Get coverage options", description = "Retrieves available insurance coverages for the quote")
    @GetMapping("/quotes/{folio}/coverage-options")
    public Mono<CoverageOptionsResponse> getCoverageOptions(@Parameter(description = "Quote folio") @PathVariable String folio) {
        return Mono.zip(cotizacionUseCase.getCoverageOptions(folio), cotizacionUseCase.getQuote(folio))
                .map(tuple -> new CoverageOptionsResponse(tuple.getT1(), tuple.getT2().getVersion()));
    }

    @Operation(summary = "Update coverage options", description = "Updates selected insurance coverages for the quote")
    @PutMapping("/quotes/{folio}/coverage-options")
    public Mono<Cotizacion> updateCoverageOptions(@Parameter(description = "Quote folio") @PathVariable String folio,
                                                  @Valid @RequestBody UpdateCoverageOptionsRequest request) {
        return cotizacionUseCase.updateCoverageOptions(folio, request.version(), request.opcionesCobertura());
    }

    @Operation(summary = "Get quote state", description = "Returns the current progress state of the quote")
    @GetMapping("/quotes/{folio}/state")
    public Mono<QuoteStateResponse> getQuoteState(@Parameter(description = "Quote folio") @PathVariable String folio) {
        return cotizacionUseCase.getQuote(folio)
                .map(quote -> new QuoteStateResponse(
                        quote.getNumeroFolio(),
                        quote.getEstadoCotizacion() != null ? quote.getEstadoCotizacion().name() : null,
                        quote.getVersion(),
                        quote.getFechaUltimaActualizacion()
                ));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Void> handleOptimisticLockingFailureException(OptimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    private List<Ubicacion> mapLocations(List<LocationRequest> locations) {
        return locations.stream()
                .map(location -> Ubicacion.builder()
                        .indice(location.index())
                        .nombreUbicacion(location.locationName())
                        .direccion(location.address())
                        .codigoPostal(location.zipCode())
                        .giro(location.giro())
                        .claveIncendio(location.fireKey())
                        .buildingValue(location.insuredAmount())
                        .contentsValue(location.contentsValue())
                        .garantias(location.coverages())
                        .build())
                .toList();
    }

    private Ubicacion mapPatchLocation(Integer indice, PatchLocationRequest request) {
        return Ubicacion.builder()
                .indice(indice)
                .nombreUbicacion(request.nombreUbicacion())
                .direccion(request.direccion())
                .codigoPostal(request.codigoPostal())
                .giro(request.giro())
                .claveIncendio(request.claveIncendio())
                .buildingValue(request.buildingValue())
                .contentsValue(request.contentsValue())
                .garantias(request.garantias())
                .alertasBloqueantes(request.alertasBloqueantes())
                .estadoValidacion(request.estadoValidacion())
                .build();
    }
}
