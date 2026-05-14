package com.hl7decoder.api.dto.compliance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RetentionSettingsRequest(
        @Min(1) @Max(3650) Integer savedRecordRetentionDays,
        @Min(30) @Max(3650) Integer auditRetentionDays
) {
}
