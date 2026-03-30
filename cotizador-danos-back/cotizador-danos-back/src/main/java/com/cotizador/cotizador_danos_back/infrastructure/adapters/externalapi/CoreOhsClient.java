package com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi;

import com.cotizador.cotizador_danos_back.application.configuration.CoreOhsProperties;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.TechnicalTariffs;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.ZipCodeValidationResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CoreOhsClient {
    private final WebClient coreOhsWebClient;
    private final CoreOhsProperties properties;

    public Mono<String> getFolio() {
        return coreOhsWebClient.get()
                .uri(properties.getEndpoints().getFolio())
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractFolio);
    }

    public Mono<TechnicalTariffs> getTariffs(String zipCode, String giro) {
        return coreOhsWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(properties.getEndpoints().getTariffs())
                        .queryParam("zipCode", zipCode)
                        .queryParam("giro", giro)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractTariffs);
    }

    public Mono<ZipCodeValidationResult> validateZipCode(String zipCode) {
        return coreOhsWebClient.get()
                .uri(properties.getEndpoints().getZipCodeValidation(), zipCode)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractZipCodeValidation);
    }

    private String extractFolio(Map<String, Object> response) {
        Map<String, Object> payload = unwrap(response);
        return firstNonBlank(readString(payload, "folio", "numeroFolio", "numero_folio", "id"),
                readString(response, "folio", "numeroFolio", "numero_folio", "id"));
    }

    private TechnicalTariffs extractTariffs(Map<String, Object> response) {
        Map<String, Object> payload = unwrap(response);
        return new TechnicalTariffs(
                readBigDecimal(payload, "incendio", "fireFactor", "incendioFactor", "incendio_factor"),
                readBigDecimal(payload, "cat", "catFactor", "cat_factor"),
                readBigDecimal(payload, "fhm", "fhmFactor", "fhm_factor"),
                firstNonBlank(readString(payload, "fireKey", "claveIncendio", "clave_incendio"), null),
                firstNonBlank(readString(payload, "catastropheZone", "zonaCatastrofica", "zona_catastrofica"), null)
        );
    }

    private ZipCodeValidationResult extractZipCodeValidation(Map<String, Object> response) {
        Map<String, Object> payload = unwrap(response);
        Boolean valid = readBoolean(payload, "valid", "isValid", "valido");
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrap(Map<String, Object> response) {
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
                return Boolean.parseBoolean(String.valueOf(value));
            }
        }
        return null;
    }

    private String firstNonBlank(String firstValue, String secondValue) {
        return Optional.ofNullable(firstValue).filter(StringUtils::hasText).orElse(secondValue);
    }
}