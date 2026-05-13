package com.hl7decoder.api.dto.platform;

public record SyntheticDataRequest(
        Integer count,
        Integer minAge,
        Integer maxAge,
        String diagnosis
) {
}
