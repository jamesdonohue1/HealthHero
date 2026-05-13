package com.hl7decoder.model.platform;

import java.util.List;

public record MedicalNecessityResponse(
        String cptCode,
        List<String> icd10Codes,
        boolean likelyCovered,
        String riskLevel,
        List<String> matchedRules,
        List<String> recommendations
) {
}
