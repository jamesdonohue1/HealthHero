package com.hl7decoder.model.platform;

import java.util.List;

public record Hl7RepairResponse(
        String originalMessage,
        String repairedMessage,
        boolean changed,
        List<String> repairs
) {
}
