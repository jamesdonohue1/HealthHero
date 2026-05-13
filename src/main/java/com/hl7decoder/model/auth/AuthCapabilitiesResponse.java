package com.hl7decoder.model.auth;

import java.util.List;

public record AuthCapabilitiesResponse(
        boolean localLoginEnabled,
        boolean apiKeysEnabled,
        boolean oidcReady,
        boolean mfaReady,
        List<String> roles,
        String tokenMode,
        int idleTimeoutMinutes
) {
}
