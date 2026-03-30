package com.cotizador.cotizador_danos_back.domain.model.cotizador.mapper;

public interface ObjectMapper {
    <T> T map(Object src, Class<T> target);
    <T> T mapBuilder(Object src, Class<T> target);
}
