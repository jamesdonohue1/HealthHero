package com.hl7decoder.api.dto.icd10;

import com.hl7decoder.model.icd10.Icd10SelectedCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record Icd10ExportRequest(
        @NotBlank @Size(max = 8000) String inputText,
        Integer resultLimit,
        List<Icd10SelectedCode> selectedCodes,
        Boolean selectedOnly
) {
}
