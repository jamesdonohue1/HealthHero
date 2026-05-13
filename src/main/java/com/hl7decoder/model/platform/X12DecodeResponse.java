package com.hl7decoder.model.platform;

import java.util.List;

public record X12DecodeResponse(
        String transactionType,
        List<X12Segment> segments,
        List<String> issues
) {
    public record X12Segment(
            int index,
            String segmentId,
            String description,
            String loop,
            List<String> elements
    ) {
    }
}
