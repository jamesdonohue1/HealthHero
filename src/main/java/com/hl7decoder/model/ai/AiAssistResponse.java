package com.hl7decoder.model.ai;

import java.time.Instant;
import java.util.List;

public record AiAssistResponse(
        String promptType,
        String provider,
        String model,
        boolean externalCall,
        boolean redacted,
        int tokenEstimate,
        String summary,
        List<String> suggestions,
        List<String> warnings,
        String approvalStatus,
        Instant generatedAt
) {
}
