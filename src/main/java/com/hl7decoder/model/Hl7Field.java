package com.hl7decoder.model;

import java.util.List;

public record Hl7Field(
        int index,
        String name,
        String value,
        boolean required,
        boolean repeating,
        String datatype,
        List<List<Hl7Component>> repetitions
) {
}
