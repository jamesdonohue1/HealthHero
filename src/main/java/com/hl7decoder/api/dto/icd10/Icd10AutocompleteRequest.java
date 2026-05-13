package com.hl7decoder.api.dto.icd10;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Icd10AutocompleteRequest(
        @NotBlank @Size(max = 200) String inputText,
        Integer resultLimit
) {
}
