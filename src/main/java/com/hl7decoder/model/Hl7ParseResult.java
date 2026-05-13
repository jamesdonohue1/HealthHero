package com.hl7decoder.model;

import java.time.Instant;
import java.util.List;

public record Hl7ParseResult(
        MessageMetadata metadata,
        List<Hl7Segment> segments,
        List<ValidationIssue> issues,
        ValidationSummary summary,
        String normalizedMessage,
        Instant parsedAt
) {
}
