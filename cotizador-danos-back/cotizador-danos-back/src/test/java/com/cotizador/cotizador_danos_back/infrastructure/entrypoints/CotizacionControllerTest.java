package com.cotizador.cotizador_danos_back.infrastructure.entrypoints;

import com.cotizador.cotizador_danos_back.domain.exception.QuoteNotFoundException;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion.DatosAsegurado;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion.DatosConduccion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.LocationsSummary;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion;
import com.cotizador.cotizador_danos_back.domain.usecase.CotizacionUseCase;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CotizacionController Tests")
class CotizacionControllerTest {

    @Mock
    private CotizacionUseCase cotizacionUseCase;

    private WebTestClient webClient;

    private static final String FOLIO = "FOL000001";

    private Cotizacion buildQuote() {
        return Cotizacion.builder()
                .numeroFolio(FOLIO)
                .estadoCotizacion(Cotizacion.EstadoCotizacion.PENDING)
                .version(0L)
                .fechaUltimaActualizacion(LocalDateTime.of(2025, 1, 1, 12, 0))
                .locations(List.of())
                .primasPorUbicacion(List.of())
                .primaNeta(BigDecimal.ZERO)
                .primaComercial(BigDecimal.ZERO)
                .build();
    }

    @BeforeEach
    void setUp() {
        CotizacionController controller = new CotizacionController(cotizacionUseCase);
        webClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ─── POST /v1/folios ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /v1/folios")
    class CreateFolioTests {

        @Test
        @DisplayName("returns 201 and quote")
        void returns201() {
            when(cotizacionUseCase.initializeQuote()).thenReturn(Mono.just(buildQuote()));

            webClient.post().uri("/v1/folios")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.numeroFolio").isEqualTo(FOLIO);
        }

        @Test
        @DisplayName("propagates error when use case fails")
        void propagatesError() {
            when(cotizacionUseCase.initializeQuote())
                    .thenReturn(Mono.error(new RuntimeException("API down")));

            webClient.post().uri("/v1/folios")
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    // ─── GET /v1/quotes/{folio}/general-info ──────────────────────────────────

    @Nested
    @DisplayName("GET /v1/quotes/{folio}/general-info")
    class GetGeneralInfoTests {

        @Test
        @DisplayName("returns 200 with general info")
        void returns200() {
            Cotizacion quote = buildQuote();
            quote.setDatosAsegurado(new DatosAsegurado("Juan Perez", "PERJ800101AAA"));
            quote.setDatosConduccion(new DatosConduccion("AG001"));
            when(cotizacionUseCase.getQuote(FOLIO)).thenReturn(Mono.just(quote));

            webClient.get().uri("/v1/quotes/{folio}/general-info", FOLIO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.version").isEqualTo(0);
        }

        @Test
        @DisplayName("returns 404 when quote not found")
        void returns404() {
            when(cotizacionUseCase.getQuote(FOLIO))
                    .thenReturn(Mono.error(new QuoteNotFoundException(FOLIO)));

            webClient.get().uri("/v1/quotes/{folio}/general-info", FOLIO)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    // ─── PUT /v1/quotes/{folio}/general-info ──────────────────────────────────

    @Nested
    @DisplayName("PUT /v1/quotes/{folio}/general-info")
    class UpdateGeneralInfoTests {

        @Test
        @DisplayName("returns 200 with updated quote")
        void returns200() {
            when(cotizacionUseCase.updateGeneralInfo(eq(FOLIO), eq(0L), any(), any()))
                    .thenReturn(Mono.just(buildQuote()));

            webClient.put().uri("/v1/quotes/{folio}/general-info", FOLIO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"version": 0, "datosAsegurado": {"nombre": "Test", "rfc": "RFC123"}, "codigoAgente": "AG001"}
                            """)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$.numeroFolio").isEqualTo(FOLIO);
        }

        @Test
        @DisplayName("returns 400 when version is null")
        void returns400OnMissingVersion() {
            webClient.put().uri("/v1/quotes/{folio}/general-info", FOLIO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"datosAsegurado": {"nombre": "Test", "rfc": "RFC123"}}
                            """)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    // ─── GET /v1/quotes/{folio}/locations ─────────────────────────────────────

    @Nested
    @DisplayName("GET /v1/quotes/{folio}/locations")
    class GetLocationsTests {

        @Test
        @DisplayName("returns 200 with locations list")
        void returns200() {
            Cotizacion quote = buildQuote();
            quote.setLocations(List.of(
                    Ubicacion.builder().indice(0).nombreUbicacion("Oficina Norte").build()
            ));
            when(cotizacionUseCase.getQuote(FOLIO)).thenReturn(Mono.just(quote));

            webClient.get().uri("/v1/quotes/{folio}/locations", FOLIO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.version").isEqualTo(0);
        }
    }

    // ─── GET /v1/quotes/{folio}/locations/layout ──────────────────────────────

    @Nested
    @DisplayName("GET /v1/quotes/{folio}/locations/layout")
    class GetLocationsLayoutTests {

        @Test
        @DisplayName("returns 200 with layout map")
        void returns200() {
            when(cotizacionUseCase.getLocationsLayout(FOLIO))
                    .thenReturn(Mono.just(Map.of("columns", List.of("col1", "col2"))));
            when(cotizacionUseCase.getQuote(FOLIO)).thenReturn(Mono.just(buildQuote()));

            webClient.get().uri("/v1/quotes/{folio}/locations/layout", FOLIO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.version").isEqualTo(0);
        }
    }

    // ─── PUT /v1/quotes/{folio}/locations/layout ──────────────────────────────

    @Nested
    @DisplayName("PUT /v1/quotes/{folio}/locations/layout")
    class UpdateLocationsLayoutTests {

        @Test
        @DisplayName("returns 200 with updated quote")
        void returns200() {
            when(cotizacionUseCase.updateLocationsLayout(eq(FOLIO), eq(0L), any()))
                    .thenReturn(Mono.just(buildQuote()));

            webClient.put().uri("/v1/quotes/{folio}/locations/layout", FOLIO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"version": 0, "configuracionLayout": {"col": "value"}}
                            """)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    // ─── GET /v1/quotes/{folio}/locations/summary ─────────────────────────────

    @Nested
    @DisplayName("GET /v1/quotes/{folio}/locations/summary")
    class GetLocationsSummaryTests {

        @Test
        @DisplayName("returns 200 with summary data")
        void returns200() {
            LocationsSummary summary = LocationsSummary.builder()
                    .totalLocations(3)
                    .validLocations(2)
                    .warningLocations(1)
                    .totalBlockingAlerts(0)
                    .totalPrimaNeta(new BigDecimal("5000"))
                    .totalPrimaComercial(new BigDecimal("5500"))
                    .build();
            when(cotizacionUseCase.getLocationsSummary(FOLIO)).thenReturn(Mono.just(summary));

            webClient.get().uri("/v1/quotes/{folio}/locations/summary", FOLIO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.totalLocations").isEqualTo(3)
                    .jsonPath("$.validLocations").isEqualTo(2);
        }
    }

    // ─── PUT /v1/quotes/{folio}/locations ─────────────────────────────────────

    @Nested
    @DisplayName("PUT /v1/quotes/{folio}/locations")
    class PutLocationsTests {

        @Test
        @DisplayName("returns 200 with saved quote")
        void returns200() {
            when(cotizacionUseCase.updateLocations(eq(FOLIO), eq(0L), any()))
                    .thenReturn(Mono.just(buildQuote()));

            webClient.put().uri("/v1/quotes/{folio}/locations", FOLIO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"version": 0, "locations": [{"index": 0, "zipCode": "06600"}]}
                            """)
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("returns 409 on version conflict")
        void returns409OnConflict() {
            when(cotizacionUseCase.updateLocations(eq(FOLIO), eq(0L), any()))
                    .thenReturn(Mono.error(new OptimisticLockingFailureException("conflict")));

            webClient.put().uri("/v1/quotes/{folio}/locations", FOLIO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"version": 0, "locations": [{"index": 0, "zipCode": "06600"}]}
                            """)
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }
    }

    // ─── PATCH /v1/quotes/{folio}/locations/{index} ───────────────────────────

    @Nested
    @DisplayName("PATCH /v1/quotes/{folio}/locations/{index}")
    class PatchLocationTests {

        @Test
        @DisplayName("returns 200 on success")
        void returns200() {
            when(cotizacionUseCase.patchLocation(eq(FOLIO), eq(0L), eq(0), any()))
                    .thenReturn(Mono.just(buildQuote()));

            webClient.patch().uri("/v1/quotes/{folio}/locations/{index}", FOLIO, 0)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"version": 0, "nombreUbicacion": "Oficina Sur"}
                            """)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    // ─── POST /v1/quotes/{folio}/calculate ────────────────────────────────────

    @Nested
    @DisplayName("POST /v1/quotes/{folio}/calculate")
    class CalculateQuoteTests {

        @Test
        @DisplayName("returns 200 with calculated quote")
        void returns200() {
            when(cotizacionUseCase.calculateQuote(eq(FOLIO), eq(0L), any()))
                    .thenReturn(Mono.just(buildQuote()));

            webClient.post().uri("/v1/quotes/{folio}/calculate", FOLIO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"version": 0, "parametros_calculo": {"FACTOR_COMERCIAL": 1.15}}
                            """)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    // ─── GET /v1/quotes/{folio}/coverage-options ──────────────────────────────

    @Nested
    @DisplayName("GET /v1/quotes/{folio}/coverage-options")
    class GetCoverageOptionsTests {

        @Test
        @DisplayName("returns 200 with options")
        void returns200() {
            when(cotizacionUseCase.getCoverageOptions(FOLIO))
                    .thenReturn(Mono.just(List.of("INCENDIO", "CATASTROFICO")));
            when(cotizacionUseCase.getQuote(FOLIO)).thenReturn(Mono.just(buildQuote()));

            webClient.get().uri("/v1/quotes/{folio}/coverage-options", FOLIO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.version").isEqualTo(0);
        }
    }

    // ─── PUT /v1/quotes/{folio}/coverage-options ──────────────────────────────

    @Nested
    @DisplayName("PUT /v1/quotes/{folio}/coverage-options")
    class UpdateCoverageOptionsTests {

        @Test
        @DisplayName("returns 200 with updated quote")
        void returns200() {
            when(cotizacionUseCase.updateCoverageOptions(eq(FOLIO), eq(0L), any()))
                    .thenReturn(Mono.just(buildQuote()));

            webClient.put().uri("/v1/quotes/{folio}/coverage-options", FOLIO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"version": 0, "opcionesCobertura": ["INCENDIO"]}
                            """)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    // ─── GET /v1/quotes/{folio}/state ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /v1/quotes/{folio}/state")
    class GetQuoteStateTests {

        @Test
        @DisplayName("returns 200 with state")
        void returns200() {
            when(cotizacionUseCase.getQuote(FOLIO)).thenReturn(Mono.just(buildQuote()));

            webClient.get().uri("/v1/quotes/{folio}/state", FOLIO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.numeroFolio").isEqualTo(FOLIO)
                    .jsonPath("$.estadoCotizacion").isEqualTo("PENDING");
        }

        @Test
        @DisplayName("returns null state when estadoCotizacion is null")
        void returnsNullStateWhenNull() {
            Cotizacion quote = buildQuote();
            quote.setEstadoCotizacion(null);
            when(cotizacionUseCase.getQuote(FOLIO)).thenReturn(Mono.just(quote));

            webClient.get().uri("/v1/quotes/{folio}/state", FOLIO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.estadoCotizacion").doesNotExist();
        }
    }
}
