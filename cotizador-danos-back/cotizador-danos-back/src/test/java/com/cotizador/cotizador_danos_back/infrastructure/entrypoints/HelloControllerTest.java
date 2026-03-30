package com.cotizador.cotizador_danos_back.infrastructure.entrypoints;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

@DisplayName("HelloController Tests")
class HelloControllerTest {

    @Test
    @DisplayName("GET /hello returns expected message")
    void returnsHelloMessage() {
        WebTestClient client = WebTestClient.bindToController(new HelloController()).build();

        client.get().uri("/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Hello, world! The API is running.");
    }
}
