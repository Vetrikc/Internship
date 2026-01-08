package com.innowise.apigeteway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
public class RouterValidator {

    private static final List<Pattern> openPatterns = List.of(
            Pattern.compile("^/api/v1/auth/register$"),
            Pattern.compile("^/api/v1/auth/login$"),
            Pattern.compile("^/api/v1/internal/.*")  // Любой путь, начинающийся с /api/v1/internal/
    );

    public final Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getPath().value();
        return openPatterns.stream().noneMatch(pattern -> pattern.matcher(path).matches());
    };
}