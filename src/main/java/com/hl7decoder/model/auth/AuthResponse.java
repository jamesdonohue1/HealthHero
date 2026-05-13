package com.hl7decoder.model.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthResponse(
        String token,
        Instant expiresAt,
        AuthUser user,
        List<String> capabilities
) {
    public record AuthUser(
            UUID id,
            String email,
            String displayName,
            UUID organizationId,
            String organizationName,
            List<UserRole> roles
    ) {
    }
}
