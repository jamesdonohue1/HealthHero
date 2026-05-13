package com.hl7decoder.model.icd10;

public record Icd10SelectedCode(
        String code,
        String description,
        String longDescription,
        Boolean billable,
        String chapter
) {
}
