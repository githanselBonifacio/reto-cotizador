package com.cotizador.cotizador_danos_back.application.configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ApiSecurityProperties securityProperties) {
        if (!securityProperties.isRequireApiKey()) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .cors(Customizer.withDefaults())
                    .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                    .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                    .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                    .build();
        }

        if (!StringUtils.hasText(securityProperties.getApiKey())) {
            throw new IllegalStateException("app.security.require-api-key is true but app.security.api-key is empty");
        }

        AuthenticationWebFilter apiKeyFilter = new AuthenticationWebFilter(apiKeyAuthenticationManager(securityProperties));
        apiKeyFilter.setServerAuthenticationConverter(apiKeyConverter());

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(Customizer.withDefaults())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> Mono.fromRunnable(() ->
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED))))
                .addFilterAt(apiKeyFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/hello", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyExchange().authenticated())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private ReactiveAuthenticationManager apiKeyAuthenticationManager(ApiSecurityProperties securityProperties) {
        byte[] expected = securityProperties.getApiKey().getBytes(StandardCharsets.UTF_8);

        return authentication -> {
            byte[] provided = String.valueOf(authentication.getCredentials()).getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, provided)) {
                return Mono.error(new BadCredentialsException("Invalid API key"));
            }

            Authentication authenticated = new UsernamePasswordAuthenticationToken(
                    "api-key-user",
                    authentication.getCredentials(),
                    AuthorityUtils.createAuthorityList("ROLE_API")
            );
            return Mono.just(authenticated);
        };
    }

    private ServerAuthenticationConverter apiKeyConverter() {
        return exchange -> {
            List<String> apiKeyHeader = exchange.getRequest().getHeaders().get("x-api-key");
            if (apiKeyHeader != null && !apiKeyHeader.isEmpty() && StringUtils.hasText(apiKeyHeader.get(0))) {
                return Mono.just(new UsernamePasswordAuthenticationToken("api-key-user", apiKeyHeader.get(0)));
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("ApiKey ")) {
                String token = authHeader.substring("ApiKey ".length()).trim();
                if (StringUtils.hasText(token)) {
                    return Mono.just(new UsernamePasswordAuthenticationToken("api-key-user", token));
                }
            }

            return Mono.empty();
        };
    }
}