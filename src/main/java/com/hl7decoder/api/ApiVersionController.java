package com.hl7decoder.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api", "/api/v1"})
public class ApiVersionController {
    private final String apiVersion;
    private final HealthEndpoint healthEndpoint;

    public ApiVersionController(@Value("${app.api.version:v1}") String apiVersion, HealthEndpoint healthEndpoint) {
        this.apiVersion = apiVersion;
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/version")
    public Map<String, Object> version() {
        return Map.of(
                "version", apiVersion,
                "stablePrefix", "/api/v1",
                "legacyPrefix", "/api",
                "health", "/actuator/health",
                "readiness", "/actuator/health/readiness",
                "liveness", "/actuator/health/liveness",
                "prometheus", "/actuator/prometheus"
        );
    }

    @GetMapping("/health")
    public Object health() {
        return healthEndpoint.health();
    }
}
