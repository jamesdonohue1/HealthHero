package com.hl7decoder.model.icd10;

import java.util.List;

public record Icd10RefineResponse(
        String inputText,
        String normalizedInput,
        List<String> diagnosisConcepts,
        List<String> clarifyingQuestions
) {
}
