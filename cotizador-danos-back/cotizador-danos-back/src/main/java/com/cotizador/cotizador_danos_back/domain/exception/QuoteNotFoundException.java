package com.cotizador.cotizador_danos_back.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class QuoteNotFoundException extends BusinessException {
    public QuoteNotFoundException(String folio) {
        super("Quote not found for folio: " + folio);
    }
}