package com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi;

import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.TechnicalTariffs;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.ZipCodeValidationRequest;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CatalogClient Tests")
class CatalogClientTest {

    @Mock
    private WebClient catalogWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private CatalogClient catalogClient;

    @BeforeEach
    void setUp() {
        catalogClient = new CatalogClient(catalogWebClient);
    }

    // ─── helpers to wire common mock chains ───────────────────────────────────

    private void givenGetReturns(Map<String, Object> body) {
        when(catalogWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(anyString(), (Object) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(body));
    }

    private void givenPostReturns(Map<String, Object> body) {
        when(catalogWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(body));
    }

    // ─── getNextFolio ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getNextFolio")
    class GetNextFolioTests {

        @Test
        @DisplayName("returns folio_number from response")
        void returnsFolioNumber() {
            givenGetReturns(Map.of("folio_number", "FOL000001", "sequence_value", 1));

            StepVerifier.create(catalogClient.getNextFolio())
                    .assertNext(folio -> assertThat(folio).isEqualTo("FOL000001"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("falls back to 'folio' field when folio_number absent")
        void fallsBackToFolioField() {
            givenGetReturns(Map.of("folio", "FOL000002"));

            StepVerifier.create(catalogClient.getNextFolio())
                    .assertNext(f -> assertThat(f).isEqualTo("FOL000002"))
                    .verifyComplete();
        }
    }

    // ─── validateZipCode ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateZipCode")
    class ValidateZipCodeTests {

        @Test
        @DisplayName("returns valid result when is_valid is true")
        void returnsValidResult() {
            givenPostReturns(Map.of(
                    "is_valid", true,
                    "zip_code", "28001",
                    "city", "Madrid",
                    "state", "Community of Madrid",
                    "risk_zone", "LOW"
            ));

            ZipCodeValidationRequest request = new ZipCodeValidationRequest("28001");
            StepVerifier.create(catalogClient.validateZipCode(request))
                    .assertNext(r -> {
                        assertThat(r.valid()).isTrue();
                        assertThat(r.state()).isEqualTo("Community of Madrid");
                        assertThat(r.city()).isEqualTo("Madrid");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("returns invalid result when is_valid is false")
        void returnsInvalidResult() {
            givenPostReturns(Map.of("is_valid", false, "zip_code", "99999"));

            StepVerifier.create(catalogClient.validateZipCode(new ZipCodeValidationRequest("99999")))
                    .assertNext(r -> assertThat(r.valid()).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("interprets missing is_valid as invalid when no state present")
        void missingIsValidWithoutState() {
            givenPostReturns(Map.of("zip_code", "00000"));

            StepVerifier.create(catalogClient.validateZipCode(new ZipCodeValidationRequest("00000")))
                    .assertNext(r -> assertThat(r.valid()).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("interprets missing is_valid as valid when state is present")
        void missingIsValidWithState() {
            givenPostReturns(Map.of("state", "Jalisco", "zip_code", "44100"));

            StepVerifier.create(catalogClient.validateZipCode(new ZipCodeValidationRequest("44100")))
                    .assertNext(r -> assertThat(r.valid()).isTrue())
                    .verifyComplete();
        }
    }

    // ─── getTariffs ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTariffs")
    class GetTariffsTests {

        @Test
        @DisplayName("parses INCENDIO, CAT and FHM from items array")
        void parsesAllFactorTypes() {
            Map<String, Object> response = Map.of(
                    "total", 3,
                    "items", List.of(
                            Map.of("factor_type", "INCENDIO", "base_rate", 0.015, "min_rate", 0.010,
                                    "max_rate", 0.025, "status", "active",
                                    "effective_date", "2020-01-01T00:00:00Z"),
                            Map.of("factor_type", "CAT", "base_rate", 0.010, "min_rate", 0.005,
                                    "max_rate", 0.020, "status", "active",
                                    "effective_date", "2020-01-01T00:00:00Z"),
                            Map.of("factor_type", "FHM", "base_rate", 0.003, "min_rate", 0.001,
                                    "max_rate", 0.008, "status", "active",
                                    "effective_date", "2020-01-01T00:00:00Z")
                    )
            );
            givenGetReturns(response);

            StepVerifier.create(catalogClient.getTariffs("28001", "FK001"))
                    .assertNext(t -> {
                        assertThat(t.fireFactor()).isEqualByComparingTo("0.015");
                        assertThat(t.catFactor()).isEqualByComparingTo("0.010");
                        assertThat(t.fhmFactor()).isEqualByComparingTo("0.003");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("returns ZERO rates when items list is empty")
        void returnsZeroWhenEmpty() {
            givenGetReturns(Map.of("total", 0, "items", List.of()));

            StepVerifier.create(catalogClient.getTariffs("28001", "FK001"))
                    .assertNext(t -> {
                        assertThat(t.fireFactor()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(t.catFactor()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(t.fhmFactor()).isEqualByComparingTo(BigDecimal.ZERO);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("clamps base_rate to max_rate when it exceeds maximum")
        void clampsToMax() {
            Map<String, Object> response = Map.of(
                    "total", 1,
                    "items", List.of(
                            Map.of("factor_type", "INCENDIO", "base_rate", 0.99,
                                    "min_rate", 0.010, "max_rate", 0.025,
                                    "status", "active", "effective_date", "2020-01-01T00:00:00Z")
                    )
            );
            givenGetReturns(response);

            StepVerifier.create(catalogClient.getTariffs("28001", "FK001"))
                    .assertNext(t -> assertThat(t.fireFactor()).isEqualByComparingTo("0.025"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("clamps base_rate to min_rate when it is below minimum")
        void clampsToMin() {
            Map<String, Object> response = Map.of(
                    "total", 1,
                    "items", List.of(
                            Map.of("factor_type", "INCENDIO", "base_rate", 0.001,
                                    "min_rate", 0.010, "max_rate", 0.025,
                                    "status", "active", "effective_date", "2020-01-01T00:00:00Z")
                    )
            );
            givenGetReturns(response);

            StepVerifier.create(catalogClient.getTariffs("28001", "FK001"))
                    .assertNext(t -> assertThat(t.fireFactor()).isEqualByComparingTo("0.010"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("skips expired tariffs")
        void skipsExpiredTariffs() {
            Map<String, Object> response = Map.of(
                    "total", 1,
                    "items", List.of(
                            Map.of("factor_type", "INCENDIO", "base_rate", 0.015,
                                    "min_rate", 0.010, "max_rate", 0.025,
                                    "status", "active",
                                    "effective_date", "2020-01-01T00:00:00Z",
                                    "expiration_date", "2021-01-01T00:00:00Z")   // expired
                    )
            );
            givenGetReturns(response);

            StepVerifier.create(catalogClient.getTariffs("28001", "FK001"))
                    .assertNext(t -> assertThat(t.fireFactor()).isEqualByComparingTo(BigDecimal.ZERO))
                    .verifyComplete();
        }

        @Test
        @DisplayName("skips inactive tariffs")
        void skipsInactiveTariffs() {
            Map<String, Object> response = Map.of(
                    "total", 1,
                    "items", List.of(
                            Map.of("factor_type", "INCENDIO", "base_rate", 0.015,
                                    "min_rate", 0.010, "max_rate", 0.025,
                                    "status", "inactive",
                                    "effective_date", "2020-01-01T00:00:00Z")
                    )
            );
            givenGetReturns(response);

            StepVerifier.create(catalogClient.getTariffs("28001", "FK001"))
                    .assertNext(t -> assertThat(t.fireFactor()).isEqualByComparingTo(BigDecimal.ZERO))
                    .verifyComplete();
        }
    }

    // ─── collection helpers ───────────────────────────────────────────────────

    @Nested
    @DisplayName("collection endpoints")
    class CollectionEndpointTests {

        @Test
        @DisplayName("getSubscribers returns items from response")
        void getSubscribers() {
            givenGetReturns(Map.of(
                    "total", 1,
                    "items", List.of(Map.of("_id", "sub1", "name", "Test Sub", "status", "active"))
            ));

            StepVerifier.create(catalogClient.getSubscribers())
                    .assertNext(item -> assertThat(item.get("name")).isEqualTo("Test Sub"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("getAgents returns items from response")
        void getAgents() {
            givenGetReturns(Map.of(
                    "total", 1,
                    "items", List.of(Map.of("_id", "ag1", "agent_code", "AGT001"))
            ));

            StepVerifier.create(catalogClient.getAgents())
                    .assertNext(item -> assertThat(item.get("agent_code")).isEqualTo("AGT001"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("getBusinessLines returns items from response")
        void getBusinessLines() {
            givenGetReturns(Map.of(
                    "total", 1,
                    "items", List.of(Map.of("_id", "bl1", "code", "DANOS"))
            ));

            StepVerifier.create(catalogClient.getBusinessLines())
                    .assertNext(item -> assertThat(item.get("code")).isEqualTo("DANOS"))
                    .verifyComplete();
        }
    }
}
