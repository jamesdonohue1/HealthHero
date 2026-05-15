package com.hl7decoder.api.dto.workspace;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkspaceCreateRequest(
        @NotBlank String recordType,
        @NotBlank String title,
        String folder,
        String tags,
        String notes,
        String visibility,
        @NotNull JsonNode payload
) {
}
