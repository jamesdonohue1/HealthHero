package com.hl7decoder.model.cpt;

import java.time.LocalDate;

public record ProcedureSearchResult(
        String code,
        String type,
        String description,
        String longDescription,
        String category,
        int confidence,
        boolean active,
        LocalDate effectiveDate,
        LocalDate terminationDate,
        String source,
        String matchReason
) {
}
