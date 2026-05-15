package com.hl7decoder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.features")
public record FeatureFlagsProperties(
        boolean dashboard,
        boolean workspaces,
        boolean aiAssist,
        boolean adminImports,
        boolean platformTools,
        boolean darkMode
) {
}
