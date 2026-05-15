package com.hl7decoder.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hl7decoder.service.FeatureFlagService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class FeatureFlagFilter extends OncePerRequestFilter {
    private final FeatureFlagService featureFlagService;
    private final ObjectMapper objectMapper;

    public FeatureFlagFilter(FeatureFlagService featureFlagService, ObjectMapper objectMapper) {
        this.featureFlagService = featureFlagService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!featureFlagService.enabledForPath(request.getRequestURI())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "status", 404,
                    "detail", "Feature is not enabled for this release."
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
