package com.hl7decoder.model.icd10;

import java.time.Instant;

public record Icd10SavedSearchResponse(
        String id,
        Instant expiresAt,
        Icd10SearchResponse search
) {
}
