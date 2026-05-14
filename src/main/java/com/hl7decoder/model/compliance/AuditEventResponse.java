package com.hl7decoder.model.compliance;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        Instant occurredAt,
        String action,
        UUID organizationId,
        UUID userId,
        String resourceType,
        String resourceId,
        boolean success,
        String details
) {
}
