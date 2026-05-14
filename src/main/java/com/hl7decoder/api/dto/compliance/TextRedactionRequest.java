package com.hl7decoder.api.dto.compliance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TextRedactionRequest(
        @NotNull @Size(max = 200000) String text
) {
}
