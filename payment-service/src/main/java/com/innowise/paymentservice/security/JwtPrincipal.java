package com.innowise.paymentservice.security;

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