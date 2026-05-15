package com.hl7decoder.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CodeSetImportRequest(
        @NotBlank String codeSetType,
        @NotBlank String codeSetVersion,
        String sourceName,
        LocalDate defaultEffectiveDate,
        Boolean activate,
        @NotBlank @Size(max = 2_000_000) String content
) {
}
