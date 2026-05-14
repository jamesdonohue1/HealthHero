package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization")
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID publicId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private int savedRecordRetentionDays = 30;

    @Column(nullable = false)
    private int auditRetentionDays = 365;

    protected Organization() {
    }

    public Organization(String name) {
        this.publicId = UUID.randomUUID();
        this.name = name;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getName() {
        return name;
    }

    public int getSavedRecordRetentionDays() {
        return savedRecordRetentionDays;
    }

    public int getAuditRetentionDays() {
        return auditRetentionDays;
    }

    public void updateRetentionSettings(Integer savedRecordRetentionDays, Integer auditRetentionDays) {
        if (savedRecordRetentionDays != null) {
            this.savedRecordRetentionDays = savedRecordRetentionDays;
        }
        if (auditRetentionDays != null) {
            this.auditRetentionDays = auditRetentionDays;
        }
    }
}
