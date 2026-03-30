package com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi;

import com.cotizador.cotizador_danos_back.application.configuration.CoreOhsProperties;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.TechnicalTariffs;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.ZipCodeValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CoreOhsClient Tests")
class CoreOhsClientTest {

    @Mock
    private WebClient coreOhsWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private CoreOhsClient client;
    private CoreOhsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CoreOhsProperties();
        properties.getEndpoints().setFolio("/folios");
        properties.getEndpoints().setTariffs("/tariffs");
        properties.getEndpoints().setZipCodeValidation("/zip-codes/{zipCode}/validate");
        client = new CoreOhsClient(coreOhsWebClient, properties);
    }

    private void givenGetReturns(Map<String, Object> body) {
        when(coreOhsWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(anyString(), (Object) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(body));
    }

    @Nested
    @DisplayName("getFolio")
    class GetFolioTests {

        @Test
        @DisplayName("extracts folio from payload.folio")
        void extractsFolioFromPayload() {
            givenGetReturns(Map.of("payload", Map.of("folio", "FOL1001")));

            StepVerifier.create(client.getFolio())
                    .assertNext(folio -> assertThat(folio).isEqualTo("FOL1001"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("uses fallback key numero_folio")
        void usesFallbackNumeroFolio() {
            givenGetReturns(Map.of("numero_folio", "FOL1002"));

            StepVerifier.create(client.getFolio())
                    .assertNext(folio -> assertThat(folio).isEqualTo("FOL1002"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("prefers nested non-blank value over top-level")
        void prefersNestedValue() {
            givenGetReturns(Map.of(
                    "folio", "TOP",
                    "data", Map.of("folio", "NESTED")
            ));

            StepVerifier.create(client.getFolio())
                    .assertNext(folio -> assertThat(folio).isEqualTo("NESTED"))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getTariffs")
    class GetTariffsTests {

        @Test
        @DisplayName("reads tariff values from nested payload")
        void readsTariffsFromPayload() {
            givenGetReturns(Map.of("payload", Map.of(
                    "incendio", "0.015",
                    "cat", "0.010",
                    "fhm", "0.003",
                    "clave_incendio", "FK001",
                    "zona_catastrofica", "Z3"
            )));

            StepVerifier.create(client.getTariffs("06600", "G1"))
                    .assertNext(t -> {
                        assertThat(t.fireFactor()).isEqualByComparingTo(new BigDecimal("0.015"));
                        assertThat(t.catFactor()).isEqualByComparingTo(new BigDecimal("0.010"));
                        assertThat(t.fhmFactor()).isEqualByComparingTo(new BigDecimal("0.003"));
                        assertThat(t.fireKey()).isEqualTo("FK001");
                        assertThat(t.catastropheZone()).isEqualTo("Z3");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("returns zero factors when keys are missing")
        void returnsZeroFactorsWhenMissing() {
            givenGetReturns(Map.of("payload", Map.of()));

            StepVerifier.create(client.getTariffs("06600", "G1"))
                    .assertNext(t -> {
                        assertThat(t.fireFactor()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(t.catFactor()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(t.fhmFactor()).isEqualByComparingTo(BigDecimal.ZERO);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("supports alternate key names")
        void supportsAlternateKeys() {
            givenGetReturns(Map.of("result", Map.of(
                    "fireFactor", 0.02,
                    "cat_factor", 0.01,
                    "fhmFactor", 0.004,
                    "fireKey", "FKX",
                    "catastropheZone", "Z1"
            )));

            StepVerifier.create(client.getTariffs("06600", "G1"))
                    .assertNext(t -> {
                        assertThat(t.fireFactor()).isEqualByComparingTo("0.02");
                        assertThat(t.catFactor()).isEqualByComparingTo("0.01");
                        assertThat(t.fhmFactor()).isEqualByComparingTo("0.004");
                        assertThat(t.fireKey()).isEqualTo("FKX");
                        assertThat(t.catastropheZone()).isEqualTo("Z1");
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("validateZipCode")
    class ValidateZipCodeTests {

        @Test
        @DisplayName("maps explicit boolean valid field")
        void mapsExplicitValidField() {
            givenGetReturns(Map.of("payload", Map.of(
                    "valid", true,
                    "state", "CDMX",
                    "municipality", "Cuauhtemoc",
                    "city", "Ciudad de Mexico",
                    "neighborhood", "Juarez",
                    "catastropheZone", "Z2"
            )));

            StepVerifier.create(client.validateZipCode("06600"))
                    .assertNext(r -> {
                        assertThat(r.valid()).isTrue();
                        assertThat(r.state()).isEqualTo("CDMX");
                        assertThat(r.municipality()).isEqualTo("Cuauhtemoc");
                        assertThat(r.city()).isEqualTo("Ciudad de Mexico");
                        assertThat(r.neighborhood()).isEqualTo("Juarez");
                        assertThat(r.catastropheZone()).isEqualTo("Z2");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("parses string boolean values")
        void parsesStringBoolean() {
            givenGetReturns(Map.of("payload", Map.of("isValid", "false", "estado", "Jalisco")));

            StepVerifier.create(client.validateZipCode("44100"))
                    .assertNext(r -> assertThat(r.valid()).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("infers valid when state exists and valid is missing")
        void infersValidFromState() {
            givenGetReturns(Map.of("payload", Map.of("estado", "Nuevo Leon", "municipio", "Monterrey")));

            StepVerifier.create(client.validateZipCode("64000"))
                    .assertNext(r -> {
                        assertThat(r.valid()).isTrue();
                        assertThat(r.state()).isEqualTo("Nuevo Leon");
                        assertThat(r.municipality()).isEqualTo("Monterrey");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("infers invalid when no valid/state info is present")
        void infersInvalidWhenNoSignals() {
            givenGetReturns(Map.of("payload", Map.of("city", "Unknown")));

            StepVerifier.create(client.validateZipCode("00000"))
                    .assertNext(r -> assertThat(r.valid()).isFalse())
                    .verifyComplete();
        }
    }
}
