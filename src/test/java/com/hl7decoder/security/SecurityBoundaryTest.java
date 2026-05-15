package com.hl7decoder.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityBoundaryTest {
    @Test
    void workspaceAiAndAdminRoutesRequireProtectedAccess() throws Exception {
        String config = Files.readString(Path.of("src/main/java/com/hl7decoder/config/SecurityConfig.java"));

        assertThat(config).contains(".requestMatchers(\"/api/workspaces/**\").authenticated()");
        assertThat(config).contains(".requestMatchers(\"/api/ai/**\", \"/api/v1/ai/**\").authenticated()");
        assertThat(config).contains(".requestMatchers(\"/api/admin/**\").hasAnyRole(\"PLATFORM_ADMIN\", \"ORGANIZATION_ADMIN\")");
    }

    @Test
    void publicRoutesAndRateLimitFilterAreRegistered() throws Exception {
        String config = Files.readString(Path.of("src/main/java/com/hl7decoder/config/SecurityConfig.java"));

        assertThat(config).contains("\"/api/auth/**\"");
        assertThat(config).contains(".addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)");
        assertThat(config).contains(".addFilterBefore(bearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)");
    }
}
