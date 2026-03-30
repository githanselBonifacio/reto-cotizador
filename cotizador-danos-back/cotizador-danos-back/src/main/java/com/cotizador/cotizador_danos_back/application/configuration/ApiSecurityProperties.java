package com.cotizador.cotizador_danos_back.application.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.security")
public class ApiSecurityProperties {
    /**
     * Enables API-key authentication for incoming requests.
     * Keep false for local development unless explicitly required.
     */
    private boolean requireApiKey = false;

    /**
     * Shared secret used to validate incoming API keys.
     */
    private String apiKey;
}