package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_key")
public class ApiKeyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID publicId;

    @Column(nullable = false)
    private String keyHash;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser owner;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Organization organization;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastUsedAt;
    private Instant revokedAt;

    protected ApiKeyEntity() {
    }

    public ApiKeyEntity(String keyHash, String name, AppUser owner) {
        this.publicId = UUID.randomUUID();
        this.keyHash = keyHash;
        this.name = name;
        this.owner = owner;
        this.organization = owner.getOrganization();
        this.createdAt = Instant.now();
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getName() {
        return name;
    }

    public AppUser getOwner() {
        return owner;
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public void markUsed() {
        lastUsedAt = Instant.now();
    }
}
