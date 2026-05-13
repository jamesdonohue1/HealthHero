package com.hl7decoder.model.platform;

import java.util.List;
import java.util.Map;

public record FhirConversionResponse(
        String sourceType,
        String targetType,
        Map<String, Object> bundle,
        List<String> mappingNotes
) {
}
