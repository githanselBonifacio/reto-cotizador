package com.cotizador.cotizador_danos_back.infrastructure.entrypoints.handler;

import com.cotizador.cotizador_danos_back.domain.exception.BusinessException;
import com.cotizador.cotizador_danos_back.domain.exception.QuoteNotFoundException;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleWebClientResponseException(WebClientResponseException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        exception.getStatusCode().value(),
                        "External API error",
                        exception.getResponseBodyAsString().isBlank() ? exception.getMessage() : exception.getResponseBodyAsString()
                ));
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleWebClientRequestException(WebClientRequestException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.BAD_GATEWAY.value(),
                        "External API unavailable",
                        "Unable to reach external API"
                ));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailureException(OptimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.CONFLICT.value(),
                        "Conflict",
                        "The quote was modified by another request. Reload the latest version and retry."
                ));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(WebExchangeBindException exception) {
        String message = exception.getFieldErrors()
                .stream()
                .map(this::toFieldMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation error",
                        message
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Business error",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(QuoteNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleQuoteNotFoundException(QuoteNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        "Not found",
                        exception.getMessage()
                ));
    }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
                HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
                return ResponseEntity.status(status)
                                .body(new ApiErrorResponse(
                                                OffsetDateTime.now(),
                                                status.value(),
                                                status.getReasonPhrase(),
                                                exception.getReason() != null ? exception.getReason() : exception.getMessage()
                                ));
        }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal server error",
                        "An unexpected error occurred"
                ));
    }

    private String toFieldMessage(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    public record ApiErrorResponse(
            OffsetDateTime timestamp,
            int status,
            String error,
            String message
    ) {
    }
}