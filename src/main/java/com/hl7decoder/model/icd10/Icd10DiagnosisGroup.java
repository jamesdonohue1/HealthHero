package com.hl7decoder.model.icd10;

import java.util.List;

public record Icd10DiagnosisGroup(
        String diagnosisText,
        boolean needsMoreInformation,
        List<String> clarifyingQuestions,
        List<Icd10SearchResult> results
) {
}
