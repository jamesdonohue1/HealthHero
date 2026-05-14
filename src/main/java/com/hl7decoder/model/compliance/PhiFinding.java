package com.hl7decoder.model.compliance;

public record PhiFinding(
        String type,
        int start,
        int end,
        String preview
) {
}
