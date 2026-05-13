package com.hl7decoder.model.platform;

import java.util.List;
import java.util.Map;

public record SyntheticDataResponse(
        List<String> hl7Messages,
        List<Map<String, Object>> fhirBundles,
        List<String> x12Claims,
        List<String> patients
) {
}
