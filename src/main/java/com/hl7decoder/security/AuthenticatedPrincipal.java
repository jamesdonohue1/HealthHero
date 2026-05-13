package com.hl7decoder.security;

import com.hl7decoder.model.auth.UserRole;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedPrincipal(
        UUID userId,
        String email,
        UUID organizationId,
        Set<UserRole> roles,
        String credentialType
) {
}
