package com.innowise.apigeteway.config;

import com.innowise.apigeteway.security.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, AuthenticationFilter authenticationFilter) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/v1/auth/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("http://localhost:8081"))  // Пропустит /auth/login и /auth/register благодаря RouterValidator
                .route("user-service", r -> r.path("/api/v1/users/**", "/api/v1/cards/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("http://localhost:8082"))
                .route("order-service", r -> r.path("/api/v1/orders/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("http://localhost:8083/order-service"))
                .build();
    }
}