package com.hl7decoder.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BearerAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final ApiKeyService apiKeyService;

    public BearerAuthenticationFilter(TokenService tokenService, ApiKeyService apiKeyService) {
        this.tokenService = tokenService;
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        authenticate(request);
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        String authorization = request.getHeader("Authorization");
        java.util.Optional<AuthenticatedPrincipal> principal = java.util.Optional.empty();
        if (authorization != null && authorization.startsWith("Bearer ")) {
            principal = tokenService.parse(authorization.substring("Bearer ".length()).trim());
        }
        if (principal.isEmpty()) {
            principal = apiKeyService.authenticate(request.getHeader("X-API-Key"));
        }
        principal.ifPresent(value -> SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                value,
                null,
                value.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.name())).toList()
        )));
    }
}
