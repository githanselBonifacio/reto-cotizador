package com.cotizador.cotizador_danos_back.domain.usecase;

import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.LocationsSummary;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion.EstadoCotizacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion.EstadoValidacion;
import com.cotizador.cotizador_danos_back.domain.exception.QuoteNotFoundException;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.CatalogClient;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.TechnicalTariffs;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.ZipCodeValidationRequest;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.nosqlrepository.CotizacionDocument;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.nosqlrepository.CotizacionRepository;
import com.cotizador.cotizador_danos_back.infrastructure.helpers.mapper.ObjectMapperImp;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CotizacionUseCaseImpl implements CotizacionUseCase {
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final CotizacionRepository cotizacionRepository;
    private final CatalogClient catalogClient;
    private final ObjectMapperImp objectMapper;

    @Override
    public Mono<Cotizacion> initializeQuote() {
        return createNewQuoteDocument()
                .map(this::toDomain);
    }

    @Override
    public Mono<Cotizacion> getQuote(String folio) {
        return getDocument(folio).map(this::toDomain);
    }

    @Override
    public Mono<Cotizacion> updateGeneralInfo(String folio, Long expectedVersion, Cotizacion.DatosAsegurado datosAsegurado, String codigoAgente) {
        return getDocument(folio)
                .flatMap(document -> {
                    document.setVersion(resolveVersion(document.getVersion(), expectedVersion));
                    document.setDatosAsegurado(datosAsegurado);
                    Cotizacion.DatosConduccion datosConduccion = Optional.ofNullable(document.getDatosConduccion())
                            .orElseGet(Cotizacion.DatosConduccion::new);
                    datosConduccion.setCodigoAgente(codigoAgente);
                    document.setDatosConduccion(datosConduccion);
                    document.setFechaUltimaActualizacion(LocalDateTime.now());
                    return cotizacionRepository.save(document);
                })
                .map(this::toDomain);
    }

    @Override
    public Mono<Cotizacion> updateLocations(String folio, Long expectedVersion, List<Ubicacion> locations) {
        List<Ubicacion> normalizedLocations = normalizeLocations(locations);
        return validateLocations(normalizedLocations)
                .flatMap(validatedLocations -> cotizacionRepository.updateLocationsAtomically(
                        folio,
                        expectedVersion,
                        validatedLocations,
                        LocalDateTime.now()
                ))
                .map(this::toDomain);
    }

    @Override
    public Mono<Cotizacion> patchLocation(String folio, Long expectedVersion, Integer indice, Ubicacion patch) {
        return getDocument(folio)
                .flatMap(document -> {
                    List<Ubicacion> current = new ArrayList<>(Optional.ofNullable(document.getLocations()).orElse(List.of()));

                    if (indice == null || indice < 0 || indice >= current.size()) {
                        return Mono.error(new QuoteNotFoundException(folio + " location index " + indice));
                    }

                    Ubicacion existing = current.get(indice);
                    current.set(indice, mergeLocation(existing, patch));

                    return validateLocations(normalizeLocations(current))
                            .flatMap(validatedLocations -> cotizacionRepository.updateLocationsAtomically(
                                    folio,
                                    expectedVersion,
                                    validatedLocations,
                                    LocalDateTime.now()
                            ));
                })
                .map(this::toDomain);
    }

            @Override
            public Mono<Map<String, Object>> getLocationsLayout(String folio) {
            return getDocument(folio)
                .map(document -> Optional.ofNullable(document.getConfiguracionLayout()).orElseGet(Map::of));
            }

            @Override
            public Mono<Cotizacion> updateLocationsLayout(String folio, Long expectedVersion, Map<String, Object> layout) {
            return cotizacionRepository.updateLayoutAtomically(
                    folio,
                    expectedVersion,
                    new HashMap<>(Optional.ofNullable(layout).orElseGet(Map::of)),
                    LocalDateTime.now()
                )
                .map(this::toDomain);
            }

            @Override
            public Mono<LocationsSummary> getLocationsSummary(String folio) {
            return getDocument(folio)
                .map(document -> {
                    List<Ubicacion> locations = Optional.ofNullable(document.getLocations()).orElse(List.of());
                    int totalLocations = locations.size();
                    int validLocations = (int) locations.stream()
                        .filter(location -> location.getEstadoValidacion() == EstadoValidacion.VALID
                                || location.getEstadoValidacion() == EstadoValidacion.VALIDADA)
                        .count();
                    int warningLocations = (int) locations.stream()
                        .filter(location -> location.getEstadoValidacion() == EstadoValidacion.INCOMPLETE
                            || location.getEstadoValidacion() == EstadoValidacion.RECHAZADA
                            || !cleanAlerts(location.getAlertasBloqueantes()).isEmpty())
                        .count();
                    int totalBlockingAlerts = locations.stream()
                        .map(location -> cleanAlerts(location.getAlertasBloqueantes()).size())
                        .reduce(0, Integer::sum);

                    return LocationsSummary.builder()
                        .totalLocations(totalLocations)
                        .validLocations(validLocations)
                        .warningLocations(warningLocations)
                        .totalBlockingAlerts(totalBlockingAlerts)
                        .totalPrimaNeta(scaleMoney(document.getPrimaNeta()))
                        .totalPrimaComercial(scaleMoney(document.getPrimaComercial()))
                        .build();
                });
            }

            @Override
            public Mono<List<String>> getCoverageOptions(String folio) {
            return getDocument(folio)
                .map(document -> Optional.ofNullable(document.getOpcionesCobertura()).orElse(List.of()));
            }

            @Override
            public Mono<Cotizacion> updateCoverageOptions(String folio, Long expectedVersion, List<String> coverageOptions) {
            return cotizacionRepository.updateCoverageOptionsAtomically(
                    folio,
                    expectedVersion,
                    Optional.ofNullable(coverageOptions).orElse(List.of()),
                    LocalDateTime.now()
                )
                .map(this::toDomain);
            }

    @Override
    public Mono<Cotizacion> calculateQuote(String folio, Long expectedVersion, Map<String, BigDecimal> calculationParameters) {
        return getDocument(folio)
                .flatMap(document -> {
                    document.setVersion(resolveVersion(document.getVersion(), expectedVersion));
                    List<Ubicacion> locations = Optional.ofNullable(document.getLocations()).orElse(List.of());

                    return Flux.fromIterable(locations)
                            .flatMapSequential(this::calculateLocation)
                            .collectList()
                            .flatMap(results -> persistCalculation(document, results, calculationParameters));
                });
    }

    private Mono<CotizacionDocument> createNewQuoteDocument() {
        return catalogClient.getFolio()
            .filter(StringUtils::hasText)
            .switchIfEmpty(Mono.error(new IllegalStateException("Catalog service returned an empty folio.")))
            .flatMap(this::createDocumentFromFolio);
    }

    private Mono<CotizacionDocument> createDocumentFromFolio(String folio) {
        return cotizacionRepository.findById(folio)
            .switchIfEmpty(
                cotizacionRepository.save(CotizacionDocument.builder()
                    .numeroFolio(folio)
                    .estadoCotizacion(EstadoCotizacion.PENDING.name())
                    .locations(List.of())
                    .configuracionLayout(new HashMap<>())
                    .opcionesCobertura(List.of())
                    .primaNeta(ZERO)
                    .primaComercial(ZERO)
                    .primasPorUbicacion(List.of())
                    .fechaUltimaActualizacion(LocalDateTime.now())
                    .build())
                    .onErrorResume(throwable ->
                        cotizacionRepository.findById(folio)
                            .switchIfEmpty(Mono.error(throwable))
                    )
            );
    }

        private Mono<Cotizacion> persistCalculation(CotizacionDocument document,
                            List<LocationCalculationResult> results,
                            Map<String, BigDecimal> calculationParameters) {
        BigDecimal totalPrimaNeta = scaleMoney(results.stream()
                .map(LocationCalculationResult::netPremium)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal commercialMultiplier = buildCommercialMultiplier(calculationParameters);
        BigDecimal primaComercial = scaleMoney(totalPrimaNeta.multiply(commercialMultiplier));

        document.setLocations(results.stream().map(LocationCalculationResult::location).toList());
        document.setPrimaNeta(totalPrimaNeta);
        document.setPrimaComercial(primaComercial);
        document.setPrimasPorUbicacion(results.stream().map(LocationCalculationResult::breakdown).toList());
        document.setEstadoCotizacion(EstadoCotizacion.CALCULATED.name());
        document.setFechaUltimaActualizacion(LocalDateTime.now());

        return cotizacionRepository.save(document).map(this::toDomain);
    }

    private Mono<LocationCalculationResult> calculateLocation(Ubicacion location) {
        Ubicacion baseLocation = normalizeLocation(location);

        if (!isLocationCalculable(baseLocation)) {
            return Mono.just(buildWarningResult(baseLocation, "WARNING: location is invalid for premium calculation."));
        }

        return catalogClient.getTariffs(baseLocation.getCodigoPostal(), baseLocation.getClaveIncendio())
            .map(tariffs -> buildCalculatedResult(baseLocation, tariffs))
            .onErrorResume(exception -> Mono.just(buildWarningResult(baseLocation, "WARNING: tariffs are unavailable for this location.")));
    }

    private LocationCalculationResult buildCalculatedResult(Ubicacion location, TechnicalTariffs tariffs) {
        BigDecimal buildingValue = Optional.ofNullable(location.getBuildingValue()).orElse(BigDecimal.ZERO);
        BigDecimal contentsValue = Optional.ofNullable(location.getContentsValue()).orElse(BigDecimal.ZERO);
        BigDecimal fireRate = Optional.ofNullable(tariffs.fireFactor()).orElse(BigDecimal.ZERO);
        BigDecimal catRate = Optional.ofNullable(tariffs.catFactor()).orElse(BigDecimal.ZERO);
        BigDecimal fhmRate = Optional.ofNullable(tariffs.fhmFactor()).orElse(BigDecimal.ZERO);

        BigDecimal incendioPremium = scaleMoney(buildingValue.multiply(fireRate));
        BigDecimal catPremium = scaleMoney(contentsValue.multiply(catRate));
        BigDecimal fhmPremium = scaleMoney(buildingValue.add(contentsValue).multiply(fhmRate));
        BigDecimal netPremium = scaleMoney(incendioPremium.add(catPremium).add(fhmPremium));

        Ubicacion calculatedLocation = location.toBuilder()
                .claveIncendio(firstNonBlank(tariffs.fireKey(), location.getClaveIncendio()))
                .estadoValidacion(EstadoValidacion.VALID)
            .alertasBloqueantes(cleanAlerts(location.getAlertasBloqueantes()))
                .build();

        Cotizacion.PrimaPorUbicacion breakdown = Cotizacion.PrimaPorUbicacion.builder()
            .indice(calculatedLocation.getIndice())
            .nombreUbicacion(calculatedLocation.getNombreUbicacion())
            .incendio(incendioPremium)
            .cat(catPremium)
            .fhm(fhmPremium)
            .primaNeta(netPremium)
            .alertasBloqueantes(calculatedLocation.getAlertasBloqueantes())
            .build();

        return new LocationCalculationResult(calculatedLocation, netPremium, breakdown);
    }

    private LocationCalculationResult buildWarningResult(Ubicacion location, String warningMessage) {
        List<String> blockingAlerts = new ArrayList<>(cleanAlerts(location.getAlertasBloqueantes()));
        blockingAlerts.add(warningMessage);

        Ubicacion warningLocation = location.toBuilder()
            .alertasBloqueantes(blockingAlerts)
                .estadoValidacion(EstadoValidacion.INCOMPLETE)
                .build();

        Cotizacion.PrimaPorUbicacion breakdown = Cotizacion.PrimaPorUbicacion.builder()
            .indice(warningLocation.getIndice())
            .nombreUbicacion(warningLocation.getNombreUbicacion())
            .primaNeta(ZERO)
            .alertasBloqueantes(warningLocation.getAlertasBloqueantes())
            .build();

        return new LocationCalculationResult(warningLocation, ZERO, breakdown);
    }

    private List<Ubicacion> normalizeLocations(List<Ubicacion> locations) {
        return Optional.ofNullable(locations).orElse(List.of()).stream().map(this::normalizeLocation).toList();
    }

    private Ubicacion normalizeLocation(Ubicacion location) {
        if (location == null) {
            return Ubicacion.builder()
                .estadoValidacion(EstadoValidacion.INCOMPLETE)
                    .alertasBloqueantes(List.of())
                    .build();
        }

        return location.toBuilder()
                .alertasBloqueantes(cleanAlerts(location.getAlertasBloqueantes()))
            .estadoValidacion(Objects.requireNonNullElse(location.getEstadoValidacion(), EstadoValidacion.INCOMPLETE))
                .build();
    }

    private Ubicacion mergeLocation(Ubicacion current, Ubicacion patch) {
        if (patch == null) {
            return current;
        }

        return current.toBuilder()
                .nombreUbicacion(firstNonBlank(patch.getNombreUbicacion(), current.getNombreUbicacion()))
                .direccion(firstNonBlank(patch.getDireccion(), current.getDireccion()))
                .codigoPostal(firstNonBlank(patch.getCodigoPostal(), current.getCodigoPostal()))
                .giro(firstNonBlank(patch.getGiro(), current.getGiro()))
                .claveIncendio(firstNonBlank(patch.getClaveIncendio(), current.getClaveIncendio()))
                .buildingValue(patch.getBuildingValue() != null ? patch.getBuildingValue() : current.getBuildingValue())
                .contentsValue(patch.getContentsValue() != null ? patch.getContentsValue() : current.getContentsValue())
                .garantias(patch.getGarantias() != null ? patch.getGarantias() : current.getGarantias())
                .alertasBloqueantes(patch.getAlertasBloqueantes() != null ? patch.getAlertasBloqueantes() : current.getAlertasBloqueantes())
                .estadoValidacion(patch.getEstadoValidacion() != null ? patch.getEstadoValidacion() : current.getEstadoValidacion())
                .build();
    }

    private boolean isLocationCalculable(Ubicacion location) {
        return StringUtils.hasText(location.getCodigoPostal())
                && StringUtils.hasText(location.getClaveIncendio())
                && location.getBuildingValue() != null
                && location.getContentsValue() != null
                && (location.getEstadoValidacion() == EstadoValidacion.VALID
                    || location.getEstadoValidacion() == EstadoValidacion.VALIDADA);
    }

    private Mono<List<Ubicacion>> validateLocations(List<Ubicacion> locations) {
        return Flux.fromIterable(locations)
                .index()
                .flatMapSequential(tuple -> {
                    int index = tuple.getT1().intValue();
                    Ubicacion location = tuple.getT2();
                    return validateLocation(location, index);
                })
                .collectList();
    }

    private Mono<Ubicacion> validateLocation(Ubicacion location, int index) {
        Ubicacion normalized = normalizeLocation(location).toBuilder()
                .indice(location.getIndice() != null ? location.getIndice() : index)
                .build();

        List<String> alerts = new ArrayList<>();
        boolean hasZipCode = StringUtils.hasText(normalized.getCodigoPostal());
        boolean hasBuildingValue = normalized.getBuildingValue() != null;
        boolean hasFireKey = StringUtils.hasText(normalized.getClaveIncendio());

        if (!hasBuildingValue) {
            alerts.add("buildingValue is required");
        }
        if (!hasFireKey) {
            alerts.add("fireKey is required");
        }

        if (!hasZipCode) {
            alerts.add("zipCode is required");
            return Mono.just(applyValidationStatus(normalized, alerts, false));
        }

        return catalogClient.validateZipCode(new ZipCodeValidationRequest(normalized.getCodigoPostal()))
                .map(validationResult -> {
                    if (!validationResult.valid()) {
                        alerts.add("zipCode is not valid in catalog");
                    }
                    boolean isValid = validationResult.valid() && hasBuildingValue && hasFireKey;
                    return applyValidationStatus(normalized, alerts, isValid);
                })
                .onErrorResume(exception -> {
                    alerts.add("zipCode validation is unavailable");
                    return Mono.just(applyValidationStatus(normalized, alerts, false));
                });
    }

    private Ubicacion applyValidationStatus(Ubicacion location, List<String> alerts, boolean isValid) {
        return location.toBuilder()
                .alertasBloqueantes(alerts)
                .estadoValidacion(isValid ? EstadoValidacion.VALID : EstadoValidacion.INCOMPLETE)
                .build();
    }

    private List<String> cleanAlerts(List<String> alerts) {
        return Optional.ofNullable(alerts).orElse(List.of()).stream()
                .filter(StringUtils::hasText)
                .toList();
    }

    private Mono<CotizacionDocument> getDocument(String folio) {
        return cotizacionRepository.findById(folio)
                .switchIfEmpty(Mono.error(new QuoteNotFoundException(folio)));
    }

    private Cotizacion toDomain(CotizacionDocument document) {
        return objectMapper.map(document, Cotizacion.class);
    }

    private Long resolveVersion(Long currentVersion, Long expectedVersion) {
        return expectedVersion != null ? expectedVersion : currentVersion;
    }

    private BigDecimal buildCommercialMultiplier(Map<String, BigDecimal> calculationParameters) {
        return Optional.ofNullable(calculationParameters)
                .orElseGet(Map::of)
                .values()
                .stream()
                .filter(Objects::nonNull)
                .reduce(ONE, BigDecimal::multiply);
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return Optional.ofNullable(value).orElse(BigDecimal.ZERO).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private record LocationCalculationResult(Ubicacion location, BigDecimal netPremium, Cotizacion.PrimaPorUbicacion breakdown) {
    }
}
