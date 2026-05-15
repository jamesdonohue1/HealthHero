package com.hl7decoder.model.workspace;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceActivityResponse(
        UUID workspaceId,
        UUID actorUserId,
        Instant occurredAt,
        String action,
        String detail
) {
}
