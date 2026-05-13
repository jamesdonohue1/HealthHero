package com.hl7decoder.model.icd10;

public record Icd10SearchResult(
        String code,
        String shortDescription,
        String longDescription,
        int rank,
        double score,
        int matchPercentage,
        boolean billable,
        String chapter,
        String matchReason
) {
}
