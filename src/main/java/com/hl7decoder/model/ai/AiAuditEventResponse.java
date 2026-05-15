package com.hl7decoder.model.ai;

import java.time.Instant;
import java.util.UUID;

public record AiAuditEventResponse(
        UUID id,
        Instant occurredAt,
        UUID organizationId,
        UUID userId,
        String promptType,
        String provider,
        String model,
        int tokenEstimate,
        boolean redacted,
        boolean externalCall,
        String approvalStatus,
        String details
) {
}
