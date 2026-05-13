package com.hl7decoder.model.cpt;

public record ModifierSuggestion(
        String modifier,
        String reason,
        boolean required
) {
}
