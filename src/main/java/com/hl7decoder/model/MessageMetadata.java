package com.hl7decoder.model;

public record MessageMetadata(
        String hl7Version,
        String messageType,
        String triggerEvent,
        String sendingApplication,
        String sendingFacility,
        String receivingApplication,
        String receivingFacility,
        String timestamp,
        String controlId,
        String processingId
) {
}
