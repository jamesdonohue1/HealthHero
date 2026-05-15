package com.hl7decoder.model;

public record FeatureFlagsResponse(
        boolean dashboard,
        boolean workspaces,
        boolean aiAssist,
        boolean adminImports,
        boolean platformTools,
        boolean darkMode
) {
}
