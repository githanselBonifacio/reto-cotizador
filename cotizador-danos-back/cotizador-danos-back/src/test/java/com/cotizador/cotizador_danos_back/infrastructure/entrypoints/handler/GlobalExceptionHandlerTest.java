package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.handler;

import com.cotizador.cotizador_danos_back.domain.exception.BusinessException;
import com.cotizador.cotizador_danos_back.domain.exception.QuoteNotFoundException;
import com.cotizador.cotizador_danos_back.infrastructure.entrypoints.handler.GlobalExceptionHandler.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ─── WebClientResponseException ───────────────────────────────────────────

    @Nested
    @DisplayName("handleWebClientResponseException")
    class WebClientResponseExceptionTests {

        @Test
        @DisplayName("returns status and body from upstream response")
        void returnsUpstreamStatus() {
            WebClientResponseException ex = WebClientResponseException.create(
                    503, "Service Unavailable",
                    org.springframework.http.HttpHeaders.EMPTY,
                    "upstream error".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8,
                    null
            );

            ResponseEntity<ApiErrorResponse> response = handler.handleWebClientResponseException(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(503);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(503);
            assertThat(response.getBody().error()).isEqualTo("External API error");
            assertThat(response.getBody().message()).isEqualTo("upstream error");
        }

        @Test
        @DisplayName("falls back to exception message when response body is blank")
        void fallsBackToMessage() {
            WebClientResponseException ex = WebClientResponseException.create(
                    404, "Not Found",
                    org.springframework.http.HttpHeaders.EMPTY,
                    "   ".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8,
                    null
            );

            ResponseEntity<ApiErrorResponse> response = handler.handleWebClientResponseException(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
        }
    }

    // ─── WebClientRequestException ────────────────────────────────────────────

    @Nested
    @DisplayName("handleWebClientRequestException")
    class WebClientRequestExceptionTests {

        @Test
        @DisplayName("returns 502 Bad Gateway")
        void returns502() {
            WebClientRequestException ex = new WebClientRequestException(
                    new RuntimeException("connection refused"),
                    org.springframework.http.HttpMethod.GET,
                    URI.create("http://catalog"),
                    org.springframework.http.HttpHeaders.EMPTY
            );

            ResponseEntity<ApiErrorResponse> response = handler.handleWebClientRequestException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(502);
            assertThat(response.getBody().error()).isEqualTo("External API unavailable");
            assertThat(response.getBody().message()).isEqualTo("Unable to reach external API");
        }
    }

    // ─── OptimisticLockingFailureException ────────────────────────────────────

    @Nested
    @DisplayName("handleOptimisticLockingFailureException")
    class OptimisticLockingTests {

        @Test
        @DisplayName("returns 409 Conflict with message")
        void returns409() {
            OptimisticLockingFailureException ex =
                    new OptimisticLockingFailureException("Version mismatch");

            ResponseEntity<ApiErrorResponse> response =
                    handler.handleOptimisticLockingFailureException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(409);
            assertThat(response.getBody().error()).isEqualTo("Conflict");
            assertThat(response.getBody().message()).contains("Reload");
        }
    }

    // ─── WebExchangeBindException ─────────────────────────────────────────────

    @Nested
    @DisplayName("handleValidationException")
    class ValidationExceptionTests {

        @Test
        @DisplayName("returns 400 with field error messages")
        void returns400WithDetails() {
            WebExchangeBindException ex = mock(WebExchangeBindException.class);
            FieldError fieldError = new FieldError("request", "version", "must not be null");
            when(ex.getFieldErrors()).thenReturn(List.of(fieldError));

            ResponseEntity<ApiErrorResponse> response = handler.handleValidationException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().error()).isEqualTo("Validation error");
            assertThat(response.getBody().message()).contains("version").contains("must not be null");
        }

        @Test
        @DisplayName("concatenates multiple field errors with comma")
        void concatenatesMultipleErrors() {
            WebExchangeBindException ex = mock(WebExchangeBindException.class);
            when(ex.getFieldErrors()).thenReturn(List.of(
                    new FieldError("req", "version", "must not be null"),
                    new FieldError("req", "locations", "must not be null")
            ));

            ResponseEntity<ApiErrorResponse> response = handler.handleValidationException(ex);

            assertThat(response.getBody().message())
                    .contains("version")
                    .contains("locations")
                    .contains(", ");
        }
    }

    // ─── BusinessException ────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleBusinessException")
    class BusinessExceptionTests {

        @Test
        @DisplayName("returns 400 with business message")
        void returns400() {
            BusinessException ex = new BusinessException("Folio cannot be blank");

            ResponseEntity<ApiErrorResponse> response = handler.handleBusinessException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().error()).isEqualTo("Business error");
            assertThat(response.getBody().message()).isEqualTo("Folio cannot be blank");
        }
    }

    // ─── QuoteNotFoundException ───────────────────────────────────────────────

    @Nested
    @DisplayName("handleQuoteNotFoundException")
    class QuoteNotFoundExceptionTests {

        @Test
        @DisplayName("returns 404 with not found message")
        void returns404() {
            QuoteNotFoundException ex = new QuoteNotFoundException("FOL000001");

            ResponseEntity<ApiErrorResponse> response = handler.handleQuoteNotFoundException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().error()).isEqualTo("Not found");
        }
    }

    // ─── ResponseStatusException ──────────────────────────────────────────────

    @Nested
    @DisplayName("handleResponseStatusException")
    class ResponseStatusExceptionTests {

        @Test
        @DisplayName("maps status code and reason phrase")
        void mapsStatusCodeAndReason() {
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Custom reason");

            ResponseEntity<ApiErrorResponse> response =
                    handler.handleResponseStatusException(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(422);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(422);
            assertThat(response.getBody().message()).isEqualTo("Custom reason");
        }

        @Test
        @DisplayName("falls back to exception message when reason is null")
        void fallsBackToMessageWhenReasonNull() {
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT);

            ResponseEntity<ApiErrorResponse> response =
                    handler.handleResponseStatusException(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(418);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isNotNull();
        }
    }

    // ─── Generic Exception ────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleUnexpectedException")
    class UnexpectedExceptionTests {

        @Test
        @DisplayName("returns 500 for any non-matched exception")
        void returns500() {
            RuntimeException ex = new RuntimeException("Unexpected failure");

            ResponseEntity<ApiErrorResponse> response = handler.handleUnexpectedException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().error()).isEqualTo("Internal server error");
            assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        }
    }
}
