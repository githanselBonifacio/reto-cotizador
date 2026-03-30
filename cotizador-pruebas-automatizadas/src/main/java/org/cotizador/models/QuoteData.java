package org.cotizador.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteData {

    private GeneralInfo generalInfo;

    @Builder.Default
    private List<Location> locations = new ArrayList<>();

    @Builder.Default
    private Map<String, String> technicalInfo = new LinkedHashMap<>();

    @Builder.Default
    private String expectedStatus = "CALCULATED";

    private String expectedPdfName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneralInfo {
        private String nombre;
        private String rfc;
        private String codigoAgente;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private String zipCode;
        private String buildingValue;
        private String contentsValue;
    }
}

