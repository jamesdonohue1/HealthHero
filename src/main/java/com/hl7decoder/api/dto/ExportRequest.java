package com.hl7decoder.api.dto;

import com.hl7decoder.model.ValidationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExportRequest(
        @NotBlank String message,
        @NotNull ValidationMode mode,
        Boolean redactPhi
) {
    public ExportRequest(String message, ValidationMode mode) {
        this(message, mode, false);
    }
}
