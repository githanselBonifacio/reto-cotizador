package com.cotizador.cotizador_danos_back.infrastructure.adapters.nosqlrepository;

import com.cotizador.cotizador_danos_back.domain.exception.QuoteNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CotizacionRepository Tests")
class CotizacionRepositoryTest {

    @Mock
    private ReactiveMongoTemplate reactiveMongoTemplate;

    private CotizacionRepository repository;

    private static final String FOLIO = "FOL000001";
    private static final Long VERSION = 0L;

    @BeforeEach
    void setUp() {
        repository = new CotizacionRepository(reactiveMongoTemplate);
        // Java evaluates switchIfEmpty arguments eagerly, so findById() is called even
        // when findAndModify returns a non-empty Mono. This baseline stub prevents NPE.
        lenient().when(reactiveMongoTemplate.findById(FOLIO, CotizacionDocument.class))
                .thenReturn(Mono.empty());
    }

    private CotizacionDocument buildDocument() {
        return CotizacionDocument.builder()
                .numeroFolio(FOLIO)
                .estadoCotizacion("PENDING")
                .version(VERSION)
                .fechaUltimaActualizacion(LocalDateTime.now())
                .locations(List.of())
                .primasPorUbicacion(List.of())
                .primaNeta(BigDecimal.ZERO)
                .primaComercial(BigDecimal.ZERO)
                .build();
    }

    @Nested
    @DisplayName("save")
    class SaveTests {
        @Test
        @DisplayName("delegates to ReactiveMongoTemplate.save")
        void delegatesToMongoTemplate() {
            CotizacionDocument doc = buildDocument();
            when(reactiveMongoTemplate.save(doc)).thenReturn(Mono.just(doc));

            StepVerifier.create(repository.save(doc))
                    .assertNext(d -> assertThat(d.getNumeroFolio()).isEqualTo(FOLIO))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {
        @Test
        @DisplayName("returns document when found")
        void returnsDocumentWhenFound() {
            CotizacionDocument doc = buildDocument();
            when(reactiveMongoTemplate.findById(FOLIO, CotizacionDocument.class)).thenReturn(Mono.just(doc));

            StepVerifier.create(repository.findById(FOLIO))
                    .assertNext(d -> assertThat(d.getNumeroFolio()).isEqualTo(FOLIO))
                    .verifyComplete();
        }

        @Test
        @DisplayName("returns empty when not found")
        void returnsEmptyWhenNotFound() {
            StepVerifier.create(repository.findById(FOLIO)).verifyComplete();
        }
    }

    @Nested
    @DisplayName("updateLocationsAtomically")
    class UpdateLocationsAtomicallyTests {
        @Test
        @DisplayName("returns updated document on success")
        void returnsUpdatedDocument() {
            CotizacionDocument updated = buildDocument();
            updated.setVersion(1L);

            when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class),
                    any(FindAndModifyOptions.class), eq(CotizacionDocument.class)))
                    .thenReturn(Mono.just(updated));

            StepVerifier.create(repository.updateLocationsAtomically(FOLIO, VERSION, List.of(), LocalDateTime.now()))
                    .assertNext(d -> assertThat(d.getVersion()).isEqualTo(1L))
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws OptimisticLockingFailureException on version mismatch")
        void throwsOnVersionMismatch() {
            CotizacionDocument existing = buildDocument();
            when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class),
                    any(FindAndModifyOptions.class), eq(CotizacionDocument.class)))
                    .thenReturn(Mono.empty());
            when(reactiveMongoTemplate.findById(FOLIO, CotizacionDocument.class)).thenReturn(Mono.just(existing));

            StepVerifier.create(repository.updateLocationsAtomically(FOLIO, VERSION, List.of(), LocalDateTime.now()))
                    .expectError(OptimisticLockingFailureException.class)
                    .verify();
        }

        @Test
        @DisplayName("throws QuoteNotFoundException when folio does not exist")
        void throwsQuoteNotFoundException() {
            when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class),
                    any(FindAndModifyOptions.class), eq(CotizacionDocument.class)))
                    .thenReturn(Mono.empty());

            StepVerifier.create(repository.updateLocationsAtomically(FOLIO, VERSION, List.of(), LocalDateTime.now()))
                    .expectError(QuoteNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("updateLayoutAtomically")
    class UpdateLayoutAtomicallyTests {
        @Test
        @DisplayName("returns updated document on success")
        void returnsUpdated() {
            CotizacionDocument updated = buildDocument();
            when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class),
                    any(FindAndModifyOptions.class), eq(CotizacionDocument.class)))
                    .thenReturn(Mono.just(updated));

            StepVerifier.create(repository.updateLayoutAtomically(FOLIO, VERSION, Map.of("active", "map"), LocalDateTime.now()))
                    .assertNext(d -> assertThat(d).isNotNull())
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws OptimisticLockingFailureException when version mismatch")
        void throwsOnVersionMismatch() {
            CotizacionDocument existing = buildDocument();
            when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class),
                    any(FindAndModifyOptions.class), eq(CotizacionDocument.class)))
                    .thenReturn(Mono.empty());
            when(reactiveMongoTemplate.findById(FOLIO, CotizacionDocument.class)).thenReturn(Mono.just(existing));

            StepVerifier.create(repository.updateLayoutAtomically(FOLIO, VERSION, Map.of(), LocalDateTime.now()))
                    .expectError(OptimisticLockingFailureException.class)
                    .verify();
        }

        @Test
        @DisplayName("throws QuoteNotFoundException when folio not found")
        void throwsQuoteNotFoundException() {
            when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class),
                    any(FindAndModifyOptions.class), eq(CotizacionDocument.class)))
                    .thenReturn(Mono.empty());

            StepVerifier.create(repository.updateLayoutAtomically(FOLIO, VERSION, Map.of(), LocalDateTime.now()))
                    .expectError(QuoteNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("updateCoverageOptionsAtomically")
    class UpdateCoverageOptionsAtomicallyTests {
        @Test
        @DisplayName("returns updated document on success")
        void returnsUpdated() {
            CotizacionDocument updated = buildDocument();
            when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class),
                    any(FindAndModifyOptions.class), eq(CotizacionDocument.class)))
                    .thenReturn(Mono.just(updated));

            StepVerifier.create(repository.updateCoverageOptionsAtomically(FOLIO, VERSION, List.of("INCENDIO"), LocalDateTime.now()))
                    .assertNext(d -> assertThat(d).isNotNull())
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws OptimisticLockingFailureException on version mismatch")
        void throwsOnVersionMismatch() {
            CotizacionDocument existing = buildDocument();
            when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class),
                    any(FindAndModifyOptions.class), eq(CotizacionDocument.class)))
                    .thenReturn(Mono.empty());
            when(reactiveMongoTemplate.findById(FOLIO, CotizacionDocument.class)).thenReturn(Mono.just(existing));

            StepVerifier.create(repository.updateCoverageOptionsAtomically(FOLIO, VERSION, List.of(), LocalDateTime.now()))
                    .expectError(OptimisticLockingFailureException.class)
                    .verify();
        }

        @Test
        @DisplayName("throws QuoteNotFoundException when folio not found")
        void throwsQuoteNotFoundException() {
            when(reactiveMongoTemplate.findAndModify(any(Query.class), any(Update.class),
                    any(FindAndModifyOptions.class), eq(CotizacionDocument.class)))
                    .thenReturn(Mono.empty());

            StepVerifier.create(repository.updateCoverageOptionsAtomically(FOLIO, VERSION, List.of(), LocalDateTime.now()))
                    .expectError(QuoteNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("findLatestPendingQuote")
    class FindLatestPendingQuoteTests {
        @Test
        @DisplayName("returns first result from query")
        void returnsFirstResult() {
            CotizacionDocument doc = buildDocument();
            when(reactiveMongoTemplate.find(any(Query.class), eq(CotizacionDocument.class))).thenReturn(Flux.just(doc));

            StepVerifier.create(repository.findLatestPendingQuote())
                    .assertNext(d -> assertThat(d.getNumeroFolio()).isEqualTo(FOLIO))
                    .verifyComplete();
        }

        @Test
        @DisplayName("returns empty when no pending quotes found")
        void returnsEmptyWhenNone() {
            when(reactiveMongoTemplate.find(any(Query.class), eq(CotizacionDocument.class))).thenReturn(Flux.empty());

            StepVerifier.create(repository.findLatestPendingQuote()).verifyComplete();
        }
    }
}
