package com.hl7decoder.api.dto.platform;

import jakarta.validation.constraints.NotBlank;

public record TextRequest(@NotBlank String text) {
}
