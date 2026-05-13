package com.hl7decoder.model;

import java.time.Instant;
import java.util.UUID;

public record SavedValidationResponse(
        UUID id,
        Instant createdAt,
        Instant expiresAt,
        Hl7ParseResult result
) {
}
