package com.cotizador.cotizador_danos_back.application;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.cotizador.cotizador_danos_back")
@ConfigurationPropertiesScan
@EnableReactiveMongoRepositories(basePackages = "com.cotizador.cotizador_danos_back.infrastructure.adapters.nosqlrepository")
public class CotizadorDanosBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(CotizadorDanosBackApplication.class, args);
	}

}
