package com.hl7decoder.service;

import com.hl7decoder.config.FeatureFlagsProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagServiceTest {
    private final FeatureFlagService service = new FeatureFlagService(new FeatureFlagsProperties(
            true,
            false,
            false,
            false,
            true,
            true
    ));

    @Test
    void exposesPublicFlags() {
        var flags = service.publicFlags();

        assertThat(flags.dashboard()).isTrue();
        assertThat(flags.workspaces()).isFalse();
        assertThat(flags.aiAssist()).isFalse();
    }

    @Test
    void gatesFeaturePaths() {
        assertThat(service.enabledForPath("/api/workspaces")).isFalse();
        assertThat(service.enabledForPath("/api/workspaces/123")).isFalse();
        assertThat(service.enabledForPath("/api/ai/assist")).isFalse();
        assertThat(service.enabledForPath("/api/admin/imports")).isFalse();
        assertThat(service.enabledForPath("/api/platform/x12/decode")).isTrue();
        assertThat(service.enabledForPath("/api/hl7/parse")).isTrue();
    }
}
