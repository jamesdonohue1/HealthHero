package com.hl7decoder.model.admin;

import java.time.Instant;
import java.util.UUID;

public record CodeSetImportJobResponse(
        UUID id,
        String codeSetType,
        String codeSetVersion,
        String sourceName,
        String status,
        int totalRows,
        int importedRows,
        int rejectedRows,
        Instant createdAt,
        Instant completedAt,
        Instant rolledBackAt,
        String validationSummary
) {
}
