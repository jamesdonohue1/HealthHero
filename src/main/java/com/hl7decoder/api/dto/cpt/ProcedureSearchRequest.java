package com.hl7decoder.api.dto.cpt;

import java.time.LocalDate;

public record ProcedureSearchRequest(
        String query,
        Integer resultLimit,
        LocalDate effectiveDate
) {
}
