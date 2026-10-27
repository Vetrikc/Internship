package com.innowise.apigeteway.config;

import com.innowise.apigeteway.security.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
class GatewayConfig {
    @Value("${order-service.url}")
    String orderUrl;
    @Value("${user-service.url}")
    String userUrl;
    @Value("${auth-service.url}")
    String authUrl;

    @Value("${payment-service.url}")
    String paymentUrl;


    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, AuthenticationFilter authenticationFilter) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/v1/auth/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri(authUrl))  // Пропустит /auth/login и /auth/register благодаря RouterValidator
                .route("user-service", r -> r.path("/api/v1/users/**", "/api/v1/cards/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri(userUrl))
                .route("order-service", r -> r.path("/api/v1/orders/**","/api/v1/items/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri(orderUrl))
                .route("payment-service", r -> r.path("/api/v1/payments/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri(paymentUrl))
                .build();
    }
}