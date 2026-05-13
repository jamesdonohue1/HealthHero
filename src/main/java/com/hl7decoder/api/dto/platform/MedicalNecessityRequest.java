package com.hl7decoder.api.dto.platform;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MedicalNecessityRequest(
        @NotBlank String cptCode,
        List<String> icd10Codes,
        String payer
) {
}
