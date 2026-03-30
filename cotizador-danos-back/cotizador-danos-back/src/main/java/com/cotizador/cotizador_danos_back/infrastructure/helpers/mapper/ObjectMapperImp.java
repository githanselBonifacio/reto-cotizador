package com.cotizador.cotizador_danos_back.infrastructure.helpers.mapper;

import com.cotizador.cotizador_danos_back.domain.model.cotizador.mapper.ObjectMapper;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;


@Component
public class ObjectMapperImp implements ObjectMapper {
    private static final ModelMapper mapper = createObjectModelMapper();
    private static final ModelMapper mMapper = createModelMapper();

    public static ModelMapper createModelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }

    public static ModelMapper createObjectModelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }

    @Override
    public <T> T map(Object src, Class<T> target) {
        return mapper.map(src, target);
    }

    @Override
    public <T> T mapBuilder(Object src, Class<T> target) {
        return mMapper.map(src, target);
    }
}
