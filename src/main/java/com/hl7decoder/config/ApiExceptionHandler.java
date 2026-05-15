package com.hl7decoder.config;

import com.hl7decoder.service.Icd10LookupException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final ErrorTrackingService errorTrackingService;

    public ApiExceptionHandler(ErrorTrackingService errorTrackingService) {
        this.errorTrackingService = errorTrackingService;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail notFound(EntityNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setDetail(ex.getMessage());
        addCorrelation(detail, ex);
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setDetail("Request validation failed.");
        addCorrelation(detail, ex);
        return detail;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail illegalState(IllegalStateException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        detail.setDetail(ex.getMessage());
        addCorrelation(detail, ex);
        return detail;
    }

    @ExceptionHandler(Icd10LookupException.class)
    ProblemDetail icd10Lookup(Icd10LookupException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        detail.setDetail(ex.getMessage());
        addCorrelation(detail, ex);
        return detail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail badCredentials(BadCredentialsException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        detail.setDetail(ex.getMessage());
        addCorrelation(detail, ex);
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail illegalArgument(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        detail.setDetail(ex.getMessage());
        addCorrelation(detail, ex);
        return detail;
    }

    private void addCorrelation(ProblemDetail detail, Exception ex) {
        String correlationId = java.util.UUID.randomUUID().toString();
        detail.setProperty("correlationId", correlationId);
        MDC.put("correlationId", correlationId);
        errorTrackingService.capture(correlationId, ex);
        MDC.remove("correlationId");
    }
}
