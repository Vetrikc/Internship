package com.innowise.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Log4j2
public class HeaderToPrincipalFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        log.info(request.getHeader("X-User-Email"));
        String email = request.getHeader("X-User-Email");
        String idStr = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");

        if (email != null && idStr != null && role != null) {
            Long id = Long.parseLong(idStr);
            JwtPrincipal principal = new JwtPrincipal(id, email, role);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            log.info(role);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}