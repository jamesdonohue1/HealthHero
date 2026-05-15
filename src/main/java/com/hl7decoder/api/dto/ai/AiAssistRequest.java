package com.hl7decoder.api.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record AiAssistRequest(
        @NotBlank String promptType,
        @NotBlank String inputText,
        Boolean redactPhi,
        Boolean requireHumanApproval,
        String model
) {
}
