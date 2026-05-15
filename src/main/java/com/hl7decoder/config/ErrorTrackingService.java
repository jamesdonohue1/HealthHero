package com.hl7decoder.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ErrorTrackingService {
    private static final Logger log = LoggerFactory.getLogger(ErrorTrackingService.class);

    public void capture(String correlationId, Exception exception) {
        log.warn("Handled API error correlationId={} type={} message={}",
                correlationId,
                exception.getClass().getSimpleName(),
                sanitize(exception.getMessage()));
    }

    private String sanitize(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("\\b\\d{3}-\\d{2}-\\d{4}\\b", "[SSN]")
                .replaceAll("\\b[A-Z][A-Z'-]{1,}\\^[A-Z][A-Z'-]{1,}\\b", "[PATIENT_NAME]");
    }
}
