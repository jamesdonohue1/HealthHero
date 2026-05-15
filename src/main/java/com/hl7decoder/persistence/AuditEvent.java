package com.hl7decoder.persistence;

import com.hl7decoder.model.compliance.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    private UUID organizationId;
    private UUID userId;
    private String resourceType;
    private String resourceId;

    @Column(nullable = false)
    private boolean success;

    @Lob
    private String details;

    protected AuditEvent() {
    }

    public AuditEvent(UUID id, Instant occurredAt, AuditAction action, UUID organizationId, UUID userId,
                      String resourceType, String resourceId, boolean success, String details) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.action = action;
        this.organizationId = organizationId;
        this.userId = userId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.success = success;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public AuditAction getAction() {
        return action;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getDetails() {
        return details;
    }
}
