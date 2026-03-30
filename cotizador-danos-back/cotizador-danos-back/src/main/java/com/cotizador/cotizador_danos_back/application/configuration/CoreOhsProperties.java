package com.cotizador.cotizador_danos_back.application.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "core-ohs")
public class CoreOhsProperties {
    private String baseUrl;
    private String apiKey;
    private Endpoints endpoints = new Endpoints();

    @Data
    public static class Endpoints {
        private String folio = "/folios";
        private String tariffs = "/tariffs";
        private String zipCodeValidation = "/zip-codes/{zipCode}/validate";
    }
}