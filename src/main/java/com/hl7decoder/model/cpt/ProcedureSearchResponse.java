package com.hl7decoder.model.cpt;

import java.time.Instant;
import java.util.List;

public record ProcedureSearchResponse(
        String query,
        Instant searchedAt,
        String licensingNotice,
        List<ProcedureSearchResult> results
) {
}
