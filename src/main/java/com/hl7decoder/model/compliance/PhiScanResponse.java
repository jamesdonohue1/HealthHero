package com.hl7decoder.model.compliance;

import java.util.List;

public record PhiScanResponse(
        boolean suspicious,
        int findingCount,
        List<PhiFinding> findings,
        String redactedText,
        String policy
) {
}
