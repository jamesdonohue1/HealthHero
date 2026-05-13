package com.hl7decoder.api.dto.icd10;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Icd10SearchRequest(
        @NotBlank @Size(max = 8000) String inputText,
        Integer resultLimit,
        Boolean includeClarifyingQuestions,
        Boolean includeAiRefinement
) {
}
