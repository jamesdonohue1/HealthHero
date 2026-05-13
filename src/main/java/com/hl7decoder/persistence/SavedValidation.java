package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_messages")
public class SavedValidation {
    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Lob
    @Column(nullable = false)
    private String encryptedPayload;

    @Lob
    @Column(nullable = false)
    private String resultJson;

    protected SavedValidation() {
    }

    public SavedValidation(UUID id, Instant createdAt, Instant expiresAt, String encryptedPayload, String resultJson) {
        this.id = id;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.encryptedPayload = encryptedPayload;
        this.resultJson = resultJson;
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

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public String getResultJson() {
        return resultJson;
    }
}
