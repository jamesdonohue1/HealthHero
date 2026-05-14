package com.hl7decoder.service;

import com.hl7decoder.api.dto.compliance.RetentionSettingsRequest;
import com.hl7decoder.model.compliance.AuditAction;
import com.hl7decoder.model.compliance.RetentionSettingsResponse;
import com.hl7decoder.persistence.Organization;
import com.hl7decoder.persistence.OrganizationRepository;
import com.hl7decoder.persistence.SavedIcd10SearchRepository;
import com.hl7decoder.persistence.SavedValidationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ComplianceService {
    private final OrganizationRepository organizationRepository;
    private final SavedValidationRepository savedValidationRepository;
    private final SavedIcd10SearchRepository savedIcd10SearchRepository;
    private final AuditService auditService;
    private final PayloadEncryptionService encryptionService;
    private final PhiScannerService phiScannerService;

    public ComplianceService(OrganizationRepository organizationRepository,
                             SavedValidationRepository savedValidationRepository,
                             SavedIcd10SearchRepository savedIcd10SearchRepository,
                             AuditService auditService,
                             PayloadEncryptionService encryptionService,
                             PhiScannerService phiScannerService) {
        this.organizationRepository = organizationRepository;
        this.savedValidationRepository = savedValidationRepository;
        this.savedIcd10SearchRepository = savedIcd10SearchRepository;
        this.auditService = auditService;
        this.encryptionService = encryptionService;
        this.phiScannerService = phiScannerService;
    }

    @Transactional(readOnly = true)
    public RetentionSettingsResponse retention(UUID organizationId) {
        Organization organization = organization(organizationId);
        return new RetentionSettingsResponse(organization.getSavedRecordRetentionDays(), organization.getAuditRetentionDays());
    }

    @Transactional
    public RetentionSettingsResponse updateRetention(UUID organizationId, UUID userId, RetentionSettingsRequest request) {
        Organization organization = organization(organizationId);
        organization.updateRetentionSettings(request.savedRecordRetentionDays(), request.auditRetentionDays());
        auditService.record(AuditAction.ADMIN, organizationId, userId, "ORGANIZATION", organizationId.toString(),
                true, "updated retention settings");
        return new RetentionSettingsResponse(organization.getSavedRecordRetentionDays(), organization.getAuditRetentionDays());
    }

    @Transactional
    public void applyRetention(UUID organizationId, UUID userId) {
        Organization organization = organization(organizationId);
        Instant savedCutoff = Instant.now().minusSeconds((long) organization.getSavedRecordRetentionDays() * 24 * 60 * 60);
        savedValidationRepository.deleteAll(savedValidationRepository.findByOrganizationIdAndCreatedAtBefore(organizationId, savedCutoff));
        savedIcd10SearchRepository.deleteAll(savedIcd10SearchRepository.findByOrganizationIdAndCreatedAtBefore(organizationId, savedCutoff));
        auditService.record(AuditAction.ADMIN, organizationId, userId, "ORGANIZATION", organizationId.toString(),
                true, "applied organization retention settings");
    }

    public String encryptionStrategy() {
        return "Saved HL7 payloads, validation results, and ICD-10 saved searches are encrypted with AES-GCM. Active key id: "
                + encryptionService.activeKeyId() + ". Rotate by deploying a new managed key id/material, then re-encrypt saved records during a maintenance job while keeping prior keys available until migration is complete.";
    }

    public String loggingPolicy() {
        return phiScannerService.policy();
    }

    private Organization organization(UUID organizationId) {
        return organizationRepository.findByPublicId(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found."));
    }
}
