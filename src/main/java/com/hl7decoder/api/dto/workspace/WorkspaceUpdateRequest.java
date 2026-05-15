package com.hl7decoder.api.dto.workspace;

import com.fasterxml.jackson.databind.JsonNode;

public record WorkspaceUpdateRequest(
        String title,
        String folder,
        String tags,
        String notes,
        String visibility,
        JsonNode payload
) {
}
