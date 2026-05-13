package com.hl7decoder.model;

public record ValidationIssue(
        ValidationSeverity severity,
        String segment,
        Integer segmentIndex,
        Integer fieldIndex,
        Integer componentIndex,
        String location,
        String description,
        String suggestedFix
) {
}
