package com.cotizador.cotizador_danos_back.infrastructure.adapters.nosqlrepository;

import com.cotizador.cotizador_danos_back.domain.exception.QuoteNotFoundException;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Ubicacion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class CotizacionRepository {
	private final ReactiveMongoTemplate reactiveMongoTemplate;

	public CotizacionRepository(ReactiveMongoTemplate reactiveMongoTemplate) {
		this.reactiveMongoTemplate = reactiveMongoTemplate;
	}

	public Mono<CotizacionDocument> save(CotizacionDocument cotizacionDocument) {
		return reactiveMongoTemplate.save(cotizacionDocument);
	}

	public Mono<CotizacionDocument> findById(String folio) {
		return reactiveMongoTemplate.findById(folio, CotizacionDocument.class);
	}

	public Mono<CotizacionDocument> updateLocationsAtomically(String folio,
											Long expectedVersion,
											List<Ubicacion> locations,
											LocalDateTime updatedAt) {
		Query query = new Query(Criteria.where("_id").is(folio)
				.and("version").is(expectedVersion));

		Update update = new Update()
				.set("locations", locations)
				.set("fechaUltimaActualizacion", updatedAt)
				.inc("version", 1L);

		FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

		return reactiveMongoTemplate.findAndModify(query, update, options, CotizacionDocument.class)
				.switchIfEmpty(findById(folio)
						.flatMap(document -> Mono.<CotizacionDocument>error(new OptimisticLockingFailureException("Version mismatch for quote " + folio)))
						.switchIfEmpty(Mono.error(new QuoteNotFoundException(folio))));
	}

	public Mono<CotizacionDocument> updateLayoutAtomically(String folio,
										Long expectedVersion,
										Map<String, Object> layout,
										LocalDateTime updatedAt) {
		Query query = new Query(Criteria.where("_id").is(folio)
				.and("version").is(expectedVersion));

		Update update = new Update()
				.set("configuracionLayout", layout)
				.set("fechaUltimaActualizacion", updatedAt)
				.inc("version", 1L);

		FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

		return reactiveMongoTemplate.findAndModify(query, update, options, CotizacionDocument.class)
				.switchIfEmpty(findById(folio)
						.flatMap(document -> {
							// Si el documento existe pero la versión no coincide, verificar si es por campos faltantes
							if (document.getConfiguracionLayout() == null) {
								// El documento no tiene configuracionLayout, inicializarlo
								Query updateQuery = new Query(Criteria.where("_id").is(folio).and("version").is(expectedVersion));
								Update initUpdate = new Update()
										.setOnInsert("configuracionLayout", new java.util.HashMap<>())
										.set("configuracionLayout", layout)
										.set("fechaUltimaActualizacion", updatedAt)
										.inc("version", 1L);
								return reactiveMongoTemplate.findAndModify(updateQuery, initUpdate, 
										FindAndModifyOptions.options().returnNew(true), CotizacionDocument.class)
										.switchIfEmpty(Mono.error(new OptimisticLockingFailureException("Version mismatch for quote " + folio)));
							}
							return Mono.error(new OptimisticLockingFailureException("Version mismatch for quote " + folio));
						})
						.switchIfEmpty(Mono.error(new QuoteNotFoundException(folio))));
	}

	public Mono<CotizacionDocument> updateCoverageOptionsAtomically(String folio,
											 Long expectedVersion,
											 List<String> coverageOptions,
											 LocalDateTime updatedAt) {
		Query query = new Query(Criteria.where("_id").is(folio)
				.and("version").is(expectedVersion));

		Update update = new Update()
				.set("opcionesCobertura", coverageOptions)
				.set("fechaUltimaActualizacion", updatedAt)
				.inc("version", 1L);

		FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

		return reactiveMongoTemplate.findAndModify(query, update, options, CotizacionDocument.class)
				.switchIfEmpty(findById(folio)
						.flatMap(document -> {
							// Si el documento existe pero la versión no coincide, verificar si es por campos faltantes
							if (document.getOpcionesCobertura() == null) {
								// El documento no tiene opcionesCobertura, inicializarlo
								Query updateQuery = new Query(Criteria.where("_id").is(folio).and("version").is(expectedVersion));
								Update initUpdate = new Update()
										.setOnInsert("opcionesCobertura", new java.util.ArrayList<>())
										.set("opcionesCobertura", coverageOptions)
										.set("fechaUltimaActualizacion", updatedAt)
										.inc("version", 1L);
								return reactiveMongoTemplate.findAndModify(updateQuery, initUpdate, 
										FindAndModifyOptions.options().returnNew(true), CotizacionDocument.class)
										.switchIfEmpty(Mono.error(new OptimisticLockingFailureException("Version mismatch for quote " + folio)));
							}
							return Mono.error(new OptimisticLockingFailureException("Version mismatch for quote " + folio));
						})
						.switchIfEmpty(Mono.error(new QuoteNotFoundException(folio))));
	}

	public Mono<CotizacionDocument> findLatestPendingQuote() {
		Query query = new Query()
				.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("estadoCotizacion").is("PENDIENTE"))
				.with(Sort.by(Sort.Direction.DESC, "fechaUltimaActualizacion"))
				.limit(1);

		return reactiveMongoTemplate.find(query, CotizacionDocument.class).next();
	}
}
