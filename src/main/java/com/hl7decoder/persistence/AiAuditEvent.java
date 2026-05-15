package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_audit_events")
public class AiAuditEvent {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private Instant occurredAt = Instant.now();

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 80)
    private String promptType;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(nullable = false)
    private int tokenEstimate;

    @Column(nullable = false)
    private boolean redacted;

    @Column(nullable = false)
    private boolean externalCall;

    @Column(nullable = false, length = 40)
    private String approvalStatus;

    @Column(length = 500)
    private String details;

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getPromptType() {
        return promptType;
    }

    public void setPromptType(String promptType) {
        this.promptType = promptType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTokenEstimate() {
        return tokenEstimate;
    }

    public void setTokenEstimate(int tokenEstimate) {
        this.tokenEstimate = tokenEstimate;
    }

    public boolean isRedacted() {
        return redacted;
    }

    public void setRedacted(boolean redacted) {
        this.redacted = redacted;
    }

    public boolean isExternalCall() {
        return externalCall;
    }

    public void setExternalCall(boolean externalCall) {
        this.externalCall = externalCall;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
