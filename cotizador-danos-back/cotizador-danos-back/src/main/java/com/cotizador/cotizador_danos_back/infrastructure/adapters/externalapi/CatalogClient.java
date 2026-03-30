package com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi;

import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.TechnicalTariffs;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.ZipCodeValidationRequest;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.ZipCodeValidationResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CatalogClient {

    private final WebClient catalogWebClient;

    public Flux<Map<String, Object>> getSubscribers() {
        return getCollection("/v1/subscribers");
    }

    public Flux<Map<String, Object>> getAgents() {
        return getCollection("/v1/agents");
    }

    public Flux<Map<String, Object>> getBusinessLines() {
        return getCollection("/v1/business-lines");
    }

    public Mono<Map<String, Object>> getZipCode(String zipCode) {
        return catalogWebClient.get()
                .uri("/v1/zip-codes/{zipCode}", zipCode)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::unwrapObject);
    }

    public Mono<ZipCodeValidationResult> validateZipCode(ZipCodeValidationRequest request) {
        return catalogWebClient.post()
                .uri("/v1/zip-codes/validate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::toZipCodeValidationResult);
    }

    public Mono<String> getNextFolio() {
        return catalogWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v1/folios")
                .queryParam("prefix", "FOL")
                .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractFolio);
    }

    public Mono<String> getFolio() {
        return getNextFolio();
    }

    public Mono<TechnicalTariffs> getTariffs(String zipCode, String fireKey) {
        // CONTRATO: GET /v1/tariffs con query params: skip, limit, factor_type (optional), business_line_id (optional)
        // Nota: zipCode y fireKey son ignorados según el nuevo contrato de API
        // Los tariffs se filtran por factor_type (INCENDIO, CAT, FHM) en la respuesta
        return catalogWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/tariffs")
                        .queryParam("skip", 0)
                        .queryParam("limit", 100)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::toTechnicalTariffs);
    }

    private Flux<Map<String, Object>> getCollection(String path) {
        return catalogWebClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMapMany(response -> Flux.fromIterable(unwrapList(response)));
    }

    private String extractFolio(Map<String, Object> response) {
        Map<String, Object> payload = unwrapObject(response);
        return firstNonBlank(
                readString(payload, "folio_number", "folio", "nextFolio", "next_folio", "numeroFolio", "numero_folio", "id"),
                readString(response, "folio_number", "folio", "nextFolio", "next_folio", "numeroFolio", "numero_folio", "id")
        );
    }

    private ZipCodeValidationResult toZipCodeValidationResult(Map<String, Object> response) {
        Map<String, Object> payload = unwrapObject(response);
        Boolean valid = readBoolean(payload, "valid", "isValid", "is_valid", "valido");
        if (valid == null) {
            valid = StringUtils.hasText(readString(payload, "state", "estado"));
        }

        return new ZipCodeValidationResult(
                Boolean.TRUE.equals(valid),
                readString(payload, "state", "estado"),
                readString(payload, "municipality", "municipio"),
                readString(payload, "city", "ciudad"),
                readString(payload, "neighborhood", "colonia"),
                readString(payload, "catastropheZone", "zonaCatastrofica", "zona_catastrofica")
        );
    }

    private TechnicalTariffs toTechnicalTariffs(Map<String, Object> response) {
        List<Map<String, Object>> tariffItems = unwrapList(response);
        if (!tariffItems.isEmpty()) {
            BigDecimal fireRate = resolveRateByFactorType(tariffItems, "INCENDIO");
            BigDecimal catRate = resolveRateByFactorType(tariffItems, "CAT");
            BigDecimal fhmRate = resolveRateByFactorType(tariffItems, "FHM");

            return new TechnicalTariffs(
                    fireRate,
                    catRate,
                    fhmRate,
                    null,
                    null
            );
        }

        Map<String, Object> payload = unwrapObject(response);
        return new TechnicalTariffs(
                readBigDecimal(payload, "incendio", "fireFactor", "incendioFactor", "incendio_factor"),
                readBigDecimal(payload, "cat", "catFactor", "cat_factor"),
                readBigDecimal(payload, "fhm", "fhmFactor", "fhm_factor"),
                firstNonBlank(readString(payload, "fireKey", "claveIncendio", "clave_incendio"), null),
                firstNonBlank(readString(payload, "catastropheZone", "zonaCatastrofica", "zona_catastrofica"), null)
        );
    }

    private BigDecimal resolveRateByFactorType(List<Map<String, Object>> tariffItems, String factorType) {
        LocalDate today = LocalDate.now();

        Optional<Map<String, Object>> activeTariff = tariffItems.stream()
                .filter(item -> factorType.equalsIgnoreCase(readString(item, "factor_type", "factorType")))
                .filter(item -> {
                    String status = readString(item, "status");
                    return !StringUtils.hasText(status) || "active".equalsIgnoreCase(status);
                })
                .filter(item -> isTariffEffective(item, today))
                .findFirst();

        if (activeTariff.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<String, Object> tariff = activeTariff.get();
        BigDecimal baseRate = readBigDecimal(tariff, "base_rate", "baseRate");
        BigDecimal minRate = readBigDecimal(tariff, "min_rate", "minRate");
        BigDecimal maxRate = readBigDecimal(tariff, "max_rate", "maxRate");

        return clampRate(baseRate, minRate, maxRate);
    }

    private boolean isTariffEffective(Map<String, Object> tariff, LocalDate today) {
        LocalDate effectiveDate = parseDate(readString(tariff, "effective_date", "effectiveDate"));
        LocalDate expirationDate = parseDate(readString(tariff, "expiration_date", "expirationDate"));

        boolean starts = effectiveDate == null || !today.isBefore(effectiveDate);
        boolean ends = expirationDate == null || !today.isAfter(expirationDate);
        return starts && ends;
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.length() >= 10 ? value.substring(0, 10) : value;
        return LocalDate.parse(normalized);
    }

    private BigDecimal clampRate(BigDecimal baseRate, BigDecimal minRate, BigDecimal maxRate) {
        BigDecimal safeBase = Optional.ofNullable(baseRate).orElse(BigDecimal.ZERO);
        BigDecimal safeMin = Optional.ofNullable(minRate).orElse(BigDecimal.ZERO);
        BigDecimal safeMax = Optional.ofNullable(maxRate).orElse(safeBase.max(safeMin));

        if (safeBase.compareTo(safeMin) < 0) {
            return safeMin;
        }
        if (safeBase.compareTo(safeMax) > 0) {
            return safeMax;
        }
        return safeBase;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> unwrapList(Map<String, Object> response) {
        for (String key : List.of("data", "result", "payload", "items")) {
            Object candidate = response.get(key);
            if (candidate instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
        }

        if (response instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapObject(Map<String, Object> response) {
        for (String key : List.of("data", "result", "payload")) {
            Object candidate = response.get(key);
            if (candidate instanceof Map<?, ?> nested) {
                return (Map<String, Object>) nested;
            }
        }
        return response;
    }

    private String readString(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private BigDecimal readBigDecimal(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                return new BigDecimal(String.valueOf(value));
            }
        }
        return BigDecimal.ZERO;
    }

    private Boolean readBoolean(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            if (value != null) {
                String normalized = String.valueOf(value).toLowerCase(Locale.ROOT);
                if (List.of("true", "1", "yes", "si", "sí").contains(normalized)) {
                    return true;
                }
                if (List.of("false", "0", "no").contains(normalized)) {
                    return false;
                }
                return Boolean.parseBoolean(normalized);
            }
        }
        return null;
    }

    private String firstNonBlank(String firstValue, String secondValue) {
        return Optional.ofNullable(firstValue).filter(StringUtils::hasText).orElse(secondValue);
    }
}
