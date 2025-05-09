package com.food.delivery.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // Use this for reactive Spring Security (Gateway is reactive)
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges ->
                        exchanges
                                .pathMatchers("/**").permitAll() // Allow all paths at Spring Security level
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable); // Disable CSRF

        return http.build();
    }
}