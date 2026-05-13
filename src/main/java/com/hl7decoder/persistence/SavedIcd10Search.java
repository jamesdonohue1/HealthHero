package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_icd10_searches")
public class SavedIcd10Search {
    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private UUID organizationId;

    @Lob
    @Column(nullable = false)
    private String searchJson;

    protected SavedIcd10Search() {
    }

    public SavedIcd10Search(UUID id, Instant createdAt, Instant expiresAt, String searchJson) {
        this(id, createdAt, expiresAt, null, searchJson);
    }

    public SavedIcd10Search(UUID id, Instant createdAt, Instant expiresAt, UUID organizationId, String searchJson) {
        this.id = id;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.organizationId = organizationId;
        this.searchJson = searchJson;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getSearchJson() {
        return searchJson;
    }
}
