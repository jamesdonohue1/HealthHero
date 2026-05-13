package com.hl7decoder.model.icd10;

import java.util.List;

public record Icd10AutocompleteResponse(
        String inputText,
        List<Icd10AutocompleteSuggestion> suggestions
) {
}
