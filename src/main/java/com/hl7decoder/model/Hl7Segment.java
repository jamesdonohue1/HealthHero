package com.hl7decoder.model;

import java.util.List;

public record Hl7Segment(
        int index,
        String name,
        String description,
        boolean custom,
        List<Hl7Field> fields
) {
}
