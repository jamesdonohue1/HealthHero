package com.hl7decoder.model.auth;

import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        String name,
        String apiKey
) {
}
