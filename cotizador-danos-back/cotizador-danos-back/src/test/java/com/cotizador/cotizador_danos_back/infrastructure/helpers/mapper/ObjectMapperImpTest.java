package com.cotizador.cotizador_danos_back.infrastructure.helpers.mapper;

import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion.DatosAsegurado;
import com.cotizador.cotizador_danos_back.domain.model.cotizador.Cotizacion.DatosConduccion;
import com.cotizador.cotizador_danos_back.infrastructure.adapters.nosqlrepository.CotizacionDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObjectMapperImp Tests")
class ObjectMapperImpTest {

    private ObjectMapperImp mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapperImp();
    }

    private CotizacionDocument buildDocument() {
        return CotizacionDocument.builder()
                .numeroFolio("FOL000001")
                .estadoCotizacion("PENDING")
                .datosAsegurado(new DatosAsegurado("Juan Perez", "PERJ800101AAA"))
                .datosConduccion(new DatosConduccion("AG001"))
                .locations(List.of())
                .primasPorUbicacion(List.of())
                .primaNeta(new BigDecimal("1000.00"))
                .primaComercial(new BigDecimal("1150.00"))
                .version(2L)
                .fechaUltimaActualizacion(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();
    }

    // ─── map ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("map()")
    class MapTests {

        @Test
        @DisplayName("maps CotizacionDocument to Cotizacion")
        void mapsCotizacionDocument() {
            CotizacionDocument doc = buildDocument();

            Cotizacion result = mapper.map(doc, Cotizacion.class);

            assertThat(result).isNotNull();
            assertThat(result.getNumeroFolio()).isEqualTo("FOL000001");
            assertThat(result.getVersion()).isEqualTo(2L);
            assertThat(result.getPrimaNeta()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(result.getPrimaComercial()).isEqualByComparingTo(new BigDecimal("1150.00"));
        }

        @Test
        @DisplayName("maps nested DatosAsegurado correctly")
        void mapsNestedDatosAsegurado() {
            CotizacionDocument doc = buildDocument();

            Cotizacion result = mapper.map(doc, Cotizacion.class);

            assertThat(result.getDatosAsegurado()).isNotNull();
            assertThat(result.getDatosAsegurado().getNombre()).isEqualTo("Juan Perez");
            assertThat(result.getDatosAsegurado().getRfc()).isEqualTo("PERJ800101AAA");
        }

        @Test
        @DisplayName("maps null nested object as null")
        void mapsNullNestedObjectAsNull() {
            CotizacionDocument doc = buildDocument();
            doc.setDatosConduccion(null);

            Cotizacion result = mapper.map(doc, Cotizacion.class);

            assertThat(result.getDatosConduccion()).isNull();
        }
    }

    // ─── mapBuilder ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mapBuilder()")
    class MapBuilderTests {

        @Test
        @DisplayName("maps CotizacionDocument to Cotizacion using mapBuilder")
        void mapsCotizacionDocument() {
            CotizacionDocument doc = buildDocument();

            Cotizacion result = mapper.mapBuilder(doc, Cotizacion.class);

            assertThat(result).isNotNull();
            assertThat(result.getNumeroFolio()).isEqualTo("FOL000001");
            assertThat(result.getVersion()).isEqualTo(2L);
        }

        @Test
        @DisplayName("produces equivalent result as map()")
        void producesEquivalentResult() {
            CotizacionDocument doc = buildDocument();

            Cotizacion viaMmap = mapper.map(doc, Cotizacion.class);
            Cotizacion viaMapBuilder = mapper.mapBuilder(doc, Cotizacion.class);

            assertThat(viaMapBuilder.getNumeroFolio()).isEqualTo(viaMmap.getNumeroFolio());
            assertThat(viaMapBuilder.getVersion()).isEqualTo(viaMmap.getVersion());
            assertThat(viaMapBuilder.getPrimaNeta()).isEqualByComparingTo(viaMmap.getPrimaNeta());
        }
    }
}
