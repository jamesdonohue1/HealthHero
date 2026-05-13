package com.hl7decoder.model.cpt;

import java.util.List;

public record IcdCptMatchResult(
        String diagnosisText,
        String diagnosisCode,
        String procedureText,
        String procedureCode,
        String payer,
        String status,
        double confidence,
        String reason,
        List<String> warnings,
        List<String> recommendations,
        List<ModifierSuggestion> modifierSuggestions
) {
}
