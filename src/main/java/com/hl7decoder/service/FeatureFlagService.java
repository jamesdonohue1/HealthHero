package com.hl7decoder.service;

import com.hl7decoder.config.FeatureFlagsProperties;
import com.hl7decoder.model.FeatureFlagsResponse;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {
    private final FeatureFlagsProperties properties;

    public FeatureFlagService(FeatureFlagsProperties properties) {
        this.properties = properties;
    }

    public FeatureFlagsResponse publicFlags() {
        return new FeatureFlagsResponse(
                properties.dashboard(),
                properties.workspaces(),
                properties.aiAssist(),
                properties.adminImports(),
                properties.platformTools(),
                properties.darkMode()
        );
    }

    public boolean enabledForPath(String path) {
        if (path.startsWith("/api/v1/ai/") || path.startsWith("/api/ai/")) {
            return properties.aiAssist();
        }
        if (path.startsWith("/api/workspaces/") || path.equals("/api/workspaces")) {
            return properties.workspaces();
        }
        if (path.startsWith("/api/admin/imports")) {
            return properties.adminImports();
        }
        if (path.startsWith("/api/v1/platform/") || path.startsWith("/api/platform/")) {
            return properties.platformTools();
        }
        return true;
    }
}
