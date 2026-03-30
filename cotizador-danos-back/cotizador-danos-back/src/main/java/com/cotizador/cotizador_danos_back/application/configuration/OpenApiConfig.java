package com.cotizador.cotizador_danos_back.application.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Property Damage Quotes API",
                version = "1.0",
                description = "Reactive API for property damage quotes"
        )
)
@Configuration
public class OpenApiConfig {
}
