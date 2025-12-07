package com.innowise.orderservice.security;

import java.security.Principal;


public record JwtPrincipal(
        Long id,
        String email,
        String role
) implements Principal {
    @Override
    public String getName() {
        return email;
    }
}