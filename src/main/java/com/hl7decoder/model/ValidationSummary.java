package com.hl7decoder.model;

public record ValidationSummary(
        int errors,
        int warnings,
        int info,
        boolean valid
) {
}
