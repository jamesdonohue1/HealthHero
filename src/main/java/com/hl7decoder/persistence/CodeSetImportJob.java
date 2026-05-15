package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "code_set_import_job")
public class CodeSetImportJob {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String codeSetType;

    @Column(nullable = false)
    private String codeSetVersion;

    private String sourceName;

    @Column(nullable = false)
    private String status;

    private int totalRows;
    private int importedRows;
    private int rejectedRows;
    private Instant createdAt;
    private Instant completedAt;
    private Instant rolledBackAt;

    @Lob
    private String validationSummary;

    protected CodeSetImportJob() {
    }

    public CodeSetImportJob(UUID id, String codeSetType, String codeSetVersion, String sourceName) {
        this.id = id;
        this.codeSetType = codeSetType;
        this.codeSetVersion = codeSetVersion;
        this.sourceName = sourceName;
        this.status = "RUNNING";
        this.createdAt = Instant.now();
    }

    public void complete(int totalRows, int importedRows, int rejectedRows, String validationSummary) {
        this.totalRows = totalRows;
        this.importedRows = importedRows;
        this.rejectedRows = rejectedRows;
        this.validationSummary = validationSummary;
        this.status = rejectedRows > 0 ? "COMPLETED_WITH_WARNINGS" : "COMPLETED";
        this.completedAt = Instant.now();
    }

    public void fail(String validationSummary) {
        this.status = "FAILED";
        this.validationSummary = validationSummary;
        this.completedAt = Instant.now();
    }

    public void rollback() {
        this.status = "ROLLED_BACK";
        this.rolledBackAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCodeSetType() { return codeSetType; }
    public String getCodeSetVersion() { return codeSetVersion; }
    public String getSourceName() { return sourceName; }
    public String getStatus() { return status; }
    public int getTotalRows() { return totalRows; }
    public int getImportedRows() { return importedRows; }
    public int getRejectedRows() { return rejectedRows; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getRolledBackAt() { return rolledBackAt; }
    public String getValidationSummary() { return validationSummary; }
}
