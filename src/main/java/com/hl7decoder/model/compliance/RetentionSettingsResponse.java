package com.hl7decoder.model.compliance;

public record RetentionSettingsResponse(
        int savedRecordRetentionDays,
        int auditRetentionDays
) {
}
