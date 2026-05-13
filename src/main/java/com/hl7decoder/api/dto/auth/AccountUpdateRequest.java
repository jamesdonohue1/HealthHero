package com.hl7decoder.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AccountUpdateRequest(
        @NotBlank String displayName
) {
}
