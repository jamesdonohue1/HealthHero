package com.hl7decoder.model.cpt;

import java.time.LocalDate;

public record ProcedureCode(
        String code,
        String codeType,
        String shortDescription,
        String longDescription,
        String category,
        LocalDate effectiveDate,
        LocalDate terminationDate,
        boolean active,
        String source,
        java.util.List<String> synonyms
) {
}
