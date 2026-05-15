package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_record", indexes = {
        @Index(name = "idx_workspace_org_updated", columnList = "organizationId,updatedAt"),
        @Index(name = "idx_workspace_org_type", columnList = "organizationId,recordType"),
        @Index(name = "idx_workspace_org_folder", columnList = "organizationId,folder")
})
public class WorkspaceRecord {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false, length = 60)
    private String recordType;

    @Column(nullable = false)
    private String title;

    private String folder;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, length = 30)
    private String visibility;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Column(length = 12000)
    private String searchText;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected WorkspaceRecord() {
    }

    public WorkspaceRecord(UUID id, UUID organizationId, UUID ownerUserId, String recordType, String title,
                           String folder, String tags, String notes, String visibility, String payloadJson) {
        this.id = id;
        this.organizationId = organizationId;
        this.ownerUserId = ownerUserId;
        this.recordType = recordType;
        this.title = title;
        this.folder = folder;
        this.tags = tags;
        this.notes = notes;
        this.visibility = visibility;
        this.payloadJson = payloadJson;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
        refreshSearchText();
    }

    public void updateMetadata(String title, String folder, String tags, String notes, String visibility) {
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
        this.folder = trim(folder);
        this.tags = trim(tags);
        this.notes = trim(notes);
        if (visibility != null && !visibility.isBlank()) {
            this.visibility = visibility.trim().toUpperCase();
        }
        this.updatedAt = Instant.now();
        refreshSearchText();
    }

    public void updatePayload(String payloadJson) {
        if (payloadJson != null && !payloadJson.isBlank()) {
            this.payloadJson = payloadJson;
        }
        this.updatedAt = Instant.now();
        refreshSearchText();
    }

    public WorkspaceRecord copy(UUID newId, UUID newOwnerUserId) {
        return new WorkspaceRecord(newId, organizationId, newOwnerUserId, recordType, title + " copy",
                folder, tags, notes, visibility, payloadJson);
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getRecordType() { return recordType; }
    public String getTitle() { return title; }
    public String getFolder() { return folder; }
    public String getTags() { return tags; }
    public String getNotes() { return notes; }
    public String getVisibility() { return visibility; }
    public String getPayloadJson() { return payloadJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private void refreshSearchText() {
        String value = String.join(" ", safe(recordType), safe(title), safe(folder), safe(tags), safe(notes), safe(payloadJson)).toLowerCase();
        this.searchText = value.length() <= 12000 ? value : value.substring(0, 12000);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
