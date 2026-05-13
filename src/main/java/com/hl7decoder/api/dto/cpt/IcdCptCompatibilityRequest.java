package com.hl7decoder.api.dto.cpt;

public record IcdCptCompatibilityRequest(
        String diagnosisText,
        String icd10Code,
        String procedureText,
        String procedureCode,
        String payer
) {
}
