package com.hl7decoder.model.icd10;

import java.time.Instant;
import java.util.List;

public record Icd10SearchResponse(
        String originalInput,
        String normalizedInput,
        Instant searchedAt,
        String disclaimer,
        List<Icd10DiagnosisGroup> diagnosisGroups
) {
}
