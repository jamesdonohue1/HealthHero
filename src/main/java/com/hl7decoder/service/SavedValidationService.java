package com.hl7decoder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.Hl7ParseResult;
import com.hl7decoder.model.SavedValidationResponse;
import com.hl7decoder.persistence.SavedValidation;
import com.hl7decoder.persistence.SavedValidationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class SavedValidationService {
    private final SavedValidationRepository repository;
    private final Hl7Service hl7Service;
    private final ObjectMapper objectMapper;
    private final PayloadEncryptionService encryptionService;
    private final PhiScannerService phiScannerService;
    private final AuditService auditService;

    public SavedValidationService(SavedValidationRepository repository, Hl7Service hl7Service, ObjectMapper objectMapper,
                                  PayloadEncryptionService encryptionService, PhiScannerService phiScannerService,
                                  AuditService auditService) {
        this.repository = repository;
        this.hl7Service = hl7Service;
        this.objectMapper = objectMapper;
        this.encryptionService = encryptionService;
        this.phiScannerService = phiScannerService;
        this.auditService = auditService;
    }

    public SavedValidationResponse saveAnonymous(Hl7Request request) {
        return saveForOrganization(request, null);
    }

    public SavedValidationResponse saveForOrganization(Hl7Request request, UUID organizationId) {
        return saveForOrganization(request, organizationId, null);
    }

    public SavedValidationResponse saveForOrganization(Hl7Request request, UUID organizationId, UUID userId) {
        String message = Boolean.TRUE.equals(request.redactPhi()) ? phiScannerService.redact(request.message()) : request.message();
        Hl7ParseResult result = hl7Service.parseAndValidate(new Hl7Request(message, request.mode(), false));
        Instant created = Instant.now();
        Instant expires = created.plus(24, ChronoUnit.HOURS);
        SavedValidation saved = new SavedValidation(UUID.randomUUID(), created, expires, organizationId,
                encryptionService.encrypt(message), encryptionService.encrypt(write(result)));
        repository.save(saved);
        auditService.record(com.hl7decoder.model.compliance.AuditAction.SAVE, organizationId, userId, "HL7_VALIDATION",
                saved.getId().toString(), true, "saved validation; redacted=" + Boolean.TRUE.equals(request.redactPhi()));
        return new SavedValidationResponse(saved.getId(), created, expires, result);
    }

    public SavedValidationResponse get(UUID id) {
        SavedValidation saved = repository.findById(id).filter(item -> item.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new EntityNotFoundException("Validation not found or expired"));
        try {
            return new SavedValidationResponse(saved.getId(), saved.getCreatedAt(), saved.getExpiresAt(),
                    objectMapper.readValue(encryptionService.decrypt(saved.getResultJson()), Hl7ParseResult.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Saved validation could not be decoded", ex);
        }
    }

    public SavedValidationResponse getForOrganization(UUID id, UUID organizationId) {
        SavedValidation saved = repository.findByIdAndOrganizationId(id, organizationId)
                .filter(item -> item.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new EntityNotFoundException("Validation not found or expired"));
        try {
            return new SavedValidationResponse(saved.getId(), saved.getCreatedAt(), saved.getExpiresAt(),
                    objectMapper.readValue(encryptionService.decrypt(saved.getResultJson()), Hl7ParseResult.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Saved validation could not be decoded", ex);
        }
    }

    @Scheduled(fixedDelayString = "${app.saved-message-cleanup-ms:3600000}")
    public void deleteExpired() {
        repository.deleteAll(repository.findByExpiresAtBefore(Instant.now()));
    }

    @Transactional
    public void deleteForOrganization(UUID id, UUID organizationId) {
        deleteForOrganization(id, organizationId, null);
    }

    @Transactional
    public void deleteForOrganization(UUID id, UUID organizationId, UUID userId) {
        SavedValidation saved = repository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Validation not found."));
        repository.delete(saved);
        auditService.record(com.hl7decoder.model.compliance.AuditAction.DELETE, organizationId, userId, "HL7_VALIDATION",
                id.toString(), true, "user-controlled saved validation delete");
    }

    private String write(Hl7ParseResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Validation result could not be serialized", ex);
        }
    }

}
