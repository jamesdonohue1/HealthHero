package com.hl7decoder.model.workspace;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceRecordResponse(
        UUID id,
        String recordType,
        String title,
        String folder,
        String tags,
        String notes,
        String visibility,
        UUID ownerUserId,
        Instant createdAt,
        Instant updatedAt,
        JsonNode payload
) {
}
