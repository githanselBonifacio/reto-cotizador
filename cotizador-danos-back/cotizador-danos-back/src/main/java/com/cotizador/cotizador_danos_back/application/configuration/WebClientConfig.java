package com.cotizador.cotizador_danos_back.application.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient.Builder webClientBuilder() {
        // Configurar connection provider con límites más altos para evitar 429
        ConnectionProvider connectionProvider = ConnectionProvider.builder("cotizador-pool")
                .maxConnections(1000)
                .maxIdleTime(java.time.Duration.ofSeconds(60))
                .maxLifeTime(java.time.Duration.ofMinutes(30))
                .pendingAcquireTimeout(java.time.Duration.ofSeconds(45))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(java.time.Duration.ofSeconds(30));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    WebClient coreOhsWebClient(WebClient.Builder builder, CoreOhsProperties properties) {
        WebClient.Builder clientBuilder = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json");

        if (StringUtils.hasText(properties.getApiKey())) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + properties.getApiKey());
        }

        return clientBuilder.build();
    }

    @Bean
    WebClient catalogWebClient(WebClient.Builder builder,
                               @Value("${catalog.base-url:http://localhost:8001}") String baseUrl,
                       @Value("${catalog.api-key:}") String apiKey) {
        WebClient.Builder clientBuilder = builder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, "application/json");

        if (StringUtils.hasText(apiKey)) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKey)
                .defaultHeader("x-api-key", apiKey);
        }

        return clientBuilder.build();
    }
}