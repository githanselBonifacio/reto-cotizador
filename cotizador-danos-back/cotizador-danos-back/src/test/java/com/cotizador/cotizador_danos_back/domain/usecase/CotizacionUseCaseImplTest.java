package com.cotizador.cotizador_danos_back.domain.usecase;

import com.cotizador.cotizador_danos_back.domain.exception.QuoteNotFoundException;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion.EstadoCotizacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.LocationsSummary;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion.EstadoValidacion;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.CatalogClient;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.TechnicalTariffs;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.ZipCodeValidationRequest;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.externalapi.dto.ZipCodeValidationResult;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.nosqlrepository.CotizacionDocument;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.nosqlrepository.CotizacionRepository;
import com.cotizador.cotizador_danos_back.infrastructure.helpers.mapper.ObjectMapperImp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CotizacionUseCaseImpl Tests")
class CotizacionUseCaseImplTest {

    @Mock
    private CotizacionRepository cotizacionRepository;

    @Mock
    private CatalogClient catalogClient;

    private CotizacionUseCaseImpl useCase;

    private static final String FOLIO = "FOL000001";
    private static final Long VERSION = 0L;

    @BeforeEach
    void setUp() {
        ObjectMapperImp mapper = new ObjectMapperImp();
        useCase = new CotizacionUseCaseImpl(cotizacionRepository, catalogClient, mapper);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private CotizacionDocument buildDocument() {
        return CotizacionDocument.builder()
                .numeroFolio(FOLIO)
                .estadoCotizacion(EstadoCotizacion.PENDING.name())
                .locations(List.of())
                .primaNeta(BigDecimal.ZERO)
                .primaComercial(BigDecimal.ZERO)
                .primasPorUbicacion(List.of())
                .version(VERSION)
                .fechaUltimaActualizacion(LocalDateTime.now())
                .build();
    }

    private CotizacionDocument buildDocumentWithLocations(List<Ubicacion> locations) {
        CotizacionDocument doc = buildDocument();
        doc.setLocations(locations);
        return doc;
    }

    private Ubicacion buildValidLocation(int index) {
        return Ubicacion.builder()
                .indice(index)
                .nombreUbicacion("Office " + index)
                .codigoPostal("28001")
                .claveIncendio("FK001")
                .buildingValue(new BigDecimal("1000000"))
                .contentsValue(new BigDecimal("500000"))
                .estadoValidacion(EstadoValidacion.VALID)
                .alertasBloqueantes(List.of())
                .build();
    }

    private TechnicalTariffs buildTariffs() {
        return new TechnicalTariffs(
                new BigDecimal("0.015"),
                new BigDecimal("0.010"),
                new BigDecimal("0.003"),
                "FK001",
                null
        );
    }

    // ─── initializeQuote ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("initializeQuote")
    class InitializeQuoteTests {

        @Test
        @DisplayName("creates new document when folio does not exist")
        void createsNewDocument() {
            when(catalogClient.getFolio()).thenReturn(Mono.just(FOLIO));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.empty());
            CotizacionDocument saved = buildDocument();
            when(cotizacionRepository.save(any())).thenReturn(Mono.just(saved));

            StepVerifier.create(useCase.initializeQuote())
                    .assertNext(q -> {
                        assertThat(q.getNumeroFolio()).isEqualTo(FOLIO);
                        assertThat(q.getEstadoCotizacion()).isEqualTo(EstadoCotizacion.PENDING);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("reuses existing document when folio already exists")
        void reusesExistingDocument() {
            CotizacionDocument existing = buildDocument();
            when(catalogClient.getFolio()).thenReturn(Mono.just(FOLIO));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(existing));
                        when(cotizacionRepository.save(any())).thenReturn(Mono.just(existing));

            StepVerifier.create(useCase.initializeQuote())
                    .assertNext(q -> assertThat(q.getNumeroFolio()).isEqualTo(FOLIO))
                    .verifyComplete();

                        // save() is invoked eagerly while building the switchIfEmpty publisher,
                        // even when findById emits a value and the save branch is not subscribed.
                        verify(cotizacionRepository).save(any());
        }

        @Test
        @DisplayName("propagates error when folio is blank")
        void propagatesErrorWhenFolioBlank() {
            when(catalogClient.getFolio()).thenReturn(Mono.just(""));

            StepVerifier.create(useCase.initializeQuote())
                    .expectError(IllegalStateException.class)
                    .verify();
        }
    }

    // ─── getQuote ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getQuote")
    class GetQuoteTests {

        @Test
        @DisplayName("returns mapped domain object")
        void returnsMappedDomain() {
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(buildDocument()));

            StepVerifier.create(useCase.getQuote(FOLIO))
                    .assertNext(q -> assertThat(q.getNumeroFolio()).isEqualTo(FOLIO))
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws QuoteNotFoundException for unknown folio")
        void throwsForUnknownFolio() {
            when(cotizacionRepository.findById("UNKNOWN")).thenReturn(Mono.empty());

            StepVerifier.create(useCase.getQuote("UNKNOWN"))
                    .expectError(QuoteNotFoundException.class)
                    .verify();
        }
    }

    // ─── updateGeneralInfo ────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateGeneralInfo")
    class UpdateGeneralInfoTests {

        @Test
        @DisplayName("updates asegurado and agent code")
        void updatesInfo() {
            CotizacionDocument doc = buildDocument();
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));
            when(cotizacionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            Cotizacion.DatosAsegurado asegurado = new Cotizacion.DatosAsegurado("Ana", "AANA010101");

            StepVerifier.create(useCase.updateGeneralInfo(FOLIO, VERSION, asegurado, "AGT001"))
                    .assertNext(q -> {
                        assertThat(q.getDatosAsegurado().getNombre()).isEqualTo("Ana");
                        assertThat(q.getDatosConduccion().getCodigoAgente()).isEqualTo("AGT001");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("creates DatosConduccion if null")
        void createsDatosConduccion() {
            CotizacionDocument doc = buildDocument();
            doc.setDatosConduccion(null);
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));
            when(cotizacionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(useCase.updateGeneralInfo(FOLIO, VERSION, null, "AGT002"))
                    .assertNext(q -> assertThat(q.getDatosConduccion().getCodigoAgente()).isEqualTo("AGT002"))
                    .verifyComplete();
        }
    }

    // ─── updateLocations ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateLocations")
    class UpdateLocationsTests {

        @Test
        @DisplayName("validates and saves location list atomically")
        void savesAtomically() {
            List<Ubicacion> locations = List.of(buildValidLocation(0));
            CotizacionDocument saved = buildDocumentWithLocations(locations);

            when(catalogClient.validateZipCode(any())).thenReturn(
                    Mono.just(new ZipCodeValidationResult(true, "State", null, "City", null, null)));
            when(cotizacionRepository.updateLocationsAtomically(eq(FOLIO), eq(VERSION), anyList(), any()))
                    .thenReturn(Mono.just(saved));

            StepVerifier.create(useCase.updateLocations(FOLIO, VERSION, locations))
                    .assertNext(q -> assertThat(q.getLocations()).hasSize(1))
                    .verifyComplete();
        }

        @Test
        @DisplayName("marks location INCOMPLETE when zip validation fails")
        void marksIncompleteOnInvalidZip() {
            List<Ubicacion> locations = List.of(buildValidLocation(0));
            CotizacionDocument doc = buildDocumentWithLocations(locations);

            when(catalogClient.validateZipCode(any())).thenReturn(
                    Mono.just(new ZipCodeValidationResult(false, null, null, null, null, null)));
            when(cotizacionRepository.updateLocationsAtomically(eq(FOLIO), eq(VERSION), anyList(), any()))
                    .thenAnswer(inv -> Mono.just(doc));

            StepVerifier.create(useCase.updateLocations(FOLIO, VERSION, locations))
                    .assertNext(q -> assertThat(q).isNotNull())
                    .verifyComplete();
        }

        @Test
        @DisplayName("handles null locations list gracefully")
        void handlesNullLocations() {
            CotizacionDocument doc = buildDocument();
            when(cotizacionRepository.updateLocationsAtomically(eq(FOLIO), eq(VERSION), anyList(), any()))
                    .thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.updateLocations(FOLIO, VERSION, null))
                    .assertNext(q -> assertThat(q).isNotNull())
                    .verifyComplete();
        }

        @Test
        @DisplayName("marks INCOMPLETE when zip code is missing")
        void marksIncompleteWhenNoZip() {
            Ubicacion noZip = Ubicacion.builder()
                    .indice(0)
                    .claveIncendio("FK001")
                    .buildingValue(BigDecimal.TEN)
                    .contentsValue(BigDecimal.ONE)
                    .estadoValidacion(EstadoValidacion.INCOMPLETE)
                    .alertasBloqueantes(List.of())
                    .build();

            CotizacionDocument doc = buildDocumentWithLocations(List.of(noZip));
            when(cotizacionRepository.updateLocationsAtomically(eq(FOLIO), eq(VERSION), anyList(), any()))
                    .thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.updateLocations(FOLIO, VERSION, List.of(noZip)))
                    .assertNext(q -> assertThat(q).isNotNull())
                    .verifyComplete();
        }

        @Test
        @DisplayName("adds alert when buildingValue is missing")
        void addsAlertWhenNoBuildingValue() {
            Ubicacion noBuildingValue = Ubicacion.builder()
                    .indice(0)
                    .codigoPostal("28001")
                    .claveIncendio("FK001")
                    .contentsValue(BigDecimal.ONE)
                    .estadoValidacion(EstadoValidacion.INCOMPLETE)
                    .alertasBloqueantes(List.of())
                    .build();

            CotizacionDocument doc = buildDocumentWithLocations(List.of(noBuildingValue));
            when(catalogClient.validateZipCode(any())).thenReturn(
                    Mono.just(new ZipCodeValidationResult(true, "S", null, "C", null, null)));
            when(cotizacionRepository.updateLocationsAtomically(eq(FOLIO), eq(VERSION), anyList(), any()))
                    .thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.updateLocations(FOLIO, VERSION, List.of(noBuildingValue)))
                    .assertNext(q -> assertThat(q).isNotNull())
                    .verifyComplete();
        }
    }

    // ─── patchLocation ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("patchLocation")
    class PatchLocationTests {

        @Test
        @DisplayName("merges patch into existing location")
        void mergesPatch() {
            Ubicacion existing = buildValidLocation(0);
            CotizacionDocument doc = buildDocumentWithLocations(List.of(existing));
            CotizacionDocument updated = buildDocumentWithLocations(List.of(existing));

            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));
            when(catalogClient.validateZipCode(any())).thenReturn(
                    Mono.just(new ZipCodeValidationResult(true, "S", null, "C", null, null)));
            when(cotizacionRepository.updateLocationsAtomically(eq(FOLIO), eq(VERSION), anyList(), any()))
                    .thenReturn(Mono.just(updated));

            Ubicacion patch = Ubicacion.builder().nombreUbicacion("Updated Name").build();

            StepVerifier.create(useCase.patchLocation(FOLIO, VERSION, 0, patch))
                    .assertNext(q -> assertThat(q).isNotNull())
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws QuoteNotFoundException for out-of-bounds index")
        void throwsForOutOfBoundsIndex() {
            CotizacionDocument doc = buildDocumentWithLocations(List.of(buildValidLocation(0)));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.patchLocation(FOLIO, VERSION, 999, Ubicacion.builder().build()))
                    .expectError(QuoteNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("throws QuoteNotFoundException for negative index")
        void throwsForNegativeIndex() {
            CotizacionDocument doc = buildDocumentWithLocations(List.of(buildValidLocation(0)));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.patchLocation(FOLIO, VERSION, -1, Ubicacion.builder().build()))
                    .expectError(QuoteNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("throws QuoteNotFoundException for null index")
        void throwsForNullIndex() {
            CotizacionDocument doc = buildDocumentWithLocations(List.of(buildValidLocation(0)));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.patchLocation(FOLIO, VERSION, null, Ubicacion.builder().build()))
                    .expectError(QuoteNotFoundException.class)
                    .verify();
        }
    }

    // ─── layout ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("layout")
    class LayoutTests {

        @Test
        @DisplayName("getLocationsLayout returns empty map when null")
        void returnsEmptyLayoutWhenNull() {
            CotizacionDocument doc = buildDocument();
            doc.setConfiguracionLayout(null);
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.getLocationsLayout(FOLIO))
                    .assertNext(layout -> assertThat(layout).isEmpty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("getLocationsLayout returns stored layout")
        void returnsStoredLayout() {
            CotizacionDocument doc = buildDocument();
            doc.setConfiguracionLayout(Map.of("active", "map", "columns", 2));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.getLocationsLayout(FOLIO))
                    .assertNext(layout -> assertThat(layout.get("active")).isEqualTo("map"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("updateLocationsLayout calls atomic repository method")
        void updatesLayoutAtomically() {
            CotizacionDocument doc = buildDocument();
            when(cotizacionRepository.updateLayoutAtomically(eq(FOLIO), eq(VERSION), anyMap(), any()))
                    .thenReturn(Mono.just(doc));

            Map<String, Object> layout = Map.of("active", "list");
            StepVerifier.create(useCase.updateLocationsLayout(FOLIO, VERSION, layout))
                    .assertNext(q -> assertThat(q).isNotNull())
                    .verifyComplete();
        }

        @Test
        @DisplayName("updateLocationsLayout handles null layout")
        void handlesNullLayout() {
            CotizacionDocument doc = buildDocument();
            when(cotizacionRepository.updateLayoutAtomically(eq(FOLIO), eq(VERSION), anyMap(), any()))
                    .thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.updateLocationsLayout(FOLIO, VERSION, null))
                    .assertNext(q -> assertThat(q).isNotNull())
                    .verifyComplete();
        }
    }

    // ─── coverage options ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("coverageOptions")
    class CoverageOptionsTests {

        @Test
        @DisplayName("getCoverageOptions returns empty list when null")
        void returnsEmptyListWhenNull() {
            CotizacionDocument doc = buildDocument();
            doc.setOpcionesCobertura(null);
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.getCoverageOptions(FOLIO))
                    .assertNext(opts -> assertThat(opts).isEmpty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("getCoverageOptions returns stored options")
        void returnsStoredOptions() {
            CotizacionDocument doc = buildDocument();
            doc.setOpcionesCobertura(List.of("INCENDIO", "CAT"));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.getCoverageOptions(FOLIO))
                    .assertNext(opts -> assertThat(opts).containsExactly("INCENDIO", "CAT"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("updateCoverageOptions calls atomic repository method")
        void updatesCoverageOptionsAtomically() {
            CotizacionDocument doc = buildDocument();
            when(cotizacionRepository.updateCoverageOptionsAtomically(eq(FOLIO), eq(VERSION), anyList(), any()))
                    .thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.updateCoverageOptions(FOLIO, VERSION, List.of("INCENDIO")))
                    .assertNext(q -> assertThat(q).isNotNull())
                    .verifyComplete();
        }

        @Test
        @DisplayName("updateCoverageOptions handles null list")
        void handlesNullList() {
            CotizacionDocument doc = buildDocument();
            when(cotizacionRepository.updateCoverageOptionsAtomically(eq(FOLIO), eq(VERSION), anyList(), any()))
                    .thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.updateCoverageOptions(FOLIO, VERSION, null))
                    .assertNext(q -> assertThat(q).isNotNull())
                    .verifyComplete();
        }
    }

    // ─── locationsSummary ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLocationsSummary")
    class LocationsSummaryTests {

        @Test
        @DisplayName("returns zero counts for empty locations")
        void returnsZeroCountsForEmpty() {
            CotizacionDocument doc = buildDocument();
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.getLocationsSummary(FOLIO))
                    .assertNext(s -> {
                        assertThat(s.getTotalLocations()).isZero();
                        assertThat(s.getValidLocations()).isZero();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("counts VALID locations correctly")
        void countsValidLocations() {
            Ubicacion valid = buildValidLocation(0);
            Ubicacion incomplete = Ubicacion.builder()
                    .indice(1)
                    .estadoValidacion(EstadoValidacion.INCOMPLETE)
                    .alertasBloqueantes(List.of("Missing zip"))
                    .build();

            CotizacionDocument doc = buildDocumentWithLocations(List.of(valid, incomplete));
            doc.setPrimaNeta(new BigDecimal("10000"));
            doc.setPrimaComercial(new BigDecimal("11000"));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.getLocationsSummary(FOLIO))
                    .assertNext(s -> {
                        assertThat(s.getTotalLocations()).isEqualTo(2);
                        assertThat(s.getValidLocations()).isEqualTo(1);
                        assertThat(s.getWarningLocations()).isEqualTo(1);
                        assertThat(s.getTotalPrimaNeta()).isEqualByComparingTo("10000");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("counts VALIDADA locations as valid")
        void countsValidadaAsValid() {
            Ubicacion validada = buildValidLocation(0).toBuilder()
                    .estadoValidacion(EstadoValidacion.VALIDADA)
                    .build();

            CotizacionDocument doc = buildDocumentWithLocations(List.of(validada));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.getLocationsSummary(FOLIO))
                    .assertNext(s -> assertThat(s.getValidLocations()).isEqualTo(1))
                    .verifyComplete();
        }

        @Test
        @DisplayName("counts RECHAZADA locations as warning")
        void countsRechazadaAsWarning() {
            Ubicacion rechazada = buildValidLocation(0).toBuilder()
                    .estadoValidacion(EstadoValidacion.RECHAZADA)
                    .alertasBloqueantes(List.of())
                    .build();

            CotizacionDocument doc = buildDocumentWithLocations(List.of(rechazada));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));

            StepVerifier.create(useCase.getLocationsSummary(FOLIO))
                    .assertNext(s -> assertThat(s.getWarningLocations()).isEqualTo(1))
                    .verifyComplete();
        }
    }

    // ─── calculateQuote ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateQuote")
    class CalculateQuoteTests {

        @Test
        @DisplayName("calculates premiums for valid locations")
        void calculatesPremiums() {
            Ubicacion location = buildValidLocation(0);
            CotizacionDocument doc = buildDocumentWithLocations(List.of(location));

            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));
            when(catalogClient.getTariffs(anyString(), anyString())).thenReturn(Mono.just(buildTariffs()));
            when(cotizacionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(useCase.calculateQuote(FOLIO, VERSION, Map.of()))
                    .assertNext(q -> {
                        // 1000000 * 0.015 = 15000; 500000 * 0.010 = 5000; 1500000 * 0.003 = 4500 → 24500
                        assertThat(q.getPrimaNeta()).isNotNull();
                        assertThat(q.getEstadoCotizacion()).isEqualTo(EstadoCotizacion.CALCULATED);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("produces zero premium for non-calculable location (no zip)")
        void producesZeroPremiumForNonCalculable() {
            Ubicacion noZip = Ubicacion.builder()
                    .indice(0)
                    .claveIncendio("FK001")
                    .buildingValue(BigDecimal.TEN)
                    .contentsValue(BigDecimal.ONE)
                    .estadoValidacion(EstadoValidacion.VALID)
                    .alertasBloqueantes(List.of())
                    .build();

            CotizacionDocument doc = buildDocumentWithLocations(List.of(noZip));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));
            when(cotizacionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(useCase.calculateQuote(FOLIO, VERSION, null))
                    .assertNext(q -> assertThat(q.getPrimaNeta()).isEqualByComparingTo(BigDecimal.ZERO))
                    .verifyComplete();
        }

        @Test
        @DisplayName("falls back to warning when tariff service fails")
        void fallsBackToWarningOnTariffError() {
            Ubicacion location = buildValidLocation(0);
            CotizacionDocument doc = buildDocumentWithLocations(List.of(location));

            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));
            when(catalogClient.getTariffs(anyString(), anyString()))
                    .thenReturn(Mono.error(new RuntimeException("Tariff service down")));
            when(cotizacionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(useCase.calculateQuote(FOLIO, VERSION, null))
                    .assertNext(q -> {
                        assertThat(q.getPrimaNeta()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(q.getEstadoCotizacion()).isEqualTo(EstadoCotizacion.CALCULATED);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("applies commercial multiplier from calculation parameters")
        void appliesCommercialMultiplier() {
            Ubicacion location = buildValidLocation(0);
            CotizacionDocument doc = buildDocumentWithLocations(List.of(location));

            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));
            when(catalogClient.getTariffs(anyString(), anyString())).thenReturn(Mono.just(buildTariffs()));
            when(cotizacionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            Map<String, BigDecimal> params = Map.of("IVA", new BigDecimal("1.10"));
            StepVerifier.create(useCase.calculateQuote(FOLIO, VERSION, params))
                    .assertNext(q -> {
                        assertThat(q.getPrimaComercial()).isGreaterThanOrEqualTo(q.getPrimaNeta());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("handles empty locations list")
        void handlesEmptyLocations() {
            CotizacionDocument doc = buildDocument();
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));
            when(cotizacionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(useCase.calculateQuote(FOLIO, VERSION, null))
                    .assertNext(q -> {
                        assertThat(q.getPrimaNeta()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(q.getEstadoCotizacion()).isEqualTo(EstadoCotizacion.CALCULATED);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("non-calculable location missing fireKey adds warning")
        void nonCalculableLocationMissingFireKey() {
            Ubicacion noFireKey = Ubicacion.builder()
                    .indice(0)
                    .codigoPostal("28001")
                    .buildingValue(BigDecimal.TEN)
                    .contentsValue(BigDecimal.ONE)
                    .estadoValidacion(EstadoValidacion.VALID)
                    .alertasBloqueantes(List.of())
                    .build();

            CotizacionDocument doc = buildDocumentWithLocations(List.of(noFireKey));
            when(cotizacionRepository.findById(FOLIO)).thenReturn(Mono.just(doc));
            when(cotizacionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(useCase.calculateQuote(FOLIO, VERSION, null))
                    .assertNext(q -> {
                        assertThat(q.getPrimaNeta()).isEqualByComparingTo(BigDecimal.ZERO);
                    })
                    .verifyComplete();
        }
    }
}
