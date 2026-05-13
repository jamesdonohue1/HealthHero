package com.hl7decoder.api.dto;

import com.hl7decoder.model.ValidationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Hl7Request(
        @NotBlank String message,
        @NotNull ValidationMode mode
) {
}
