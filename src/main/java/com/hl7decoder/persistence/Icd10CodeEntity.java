package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "icd10_code", indexes = {
        @Index(name = "idx_icd10_code", columnList = "code"),
        @Index(name = "idx_icd10_version", columnList = "codeSetVersion"),
        @Index(name = "idx_icd10_effective", columnList = "effectiveDate,terminationDate")
})
public class Icd10CodeEntity {
    @Id
    @Column(length = 20)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String longDescription;

    private String chapter;
    private LocalDate effectiveDate;
    private LocalDate terminationDate;
    private boolean active = true;
    private String replacementCode;
    private String codeSetVersion;
    private String source;
    private String importJobId;
    private String provenance;
    private double confidence = 1.0;

    @Lob
    private String searchText;

    protected Icd10CodeEntity() {
    }

    public Icd10CodeEntity(String code, String shortDescription, String longDescription, String chapter,
                           LocalDate effectiveDate, LocalDate terminationDate, boolean active,
                           String replacementCode, String codeSetVersion, String source, String importJobId,
                           String provenance, double confidence) {
        this.code = code;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.chapter = chapter;
        this.effectiveDate = effectiveDate;
        this.terminationDate = terminationDate;
        this.active = active;
        this.replacementCode = replacementCode;
        this.codeSetVersion = codeSetVersion;
        this.source = source;
        this.importJobId = importJobId;
        this.provenance = provenance;
        this.confidence = confidence;
        this.searchText = String.join(" ", nullSafe(code), nullSafe(shortDescription), nullSafe(longDescription), nullSafe(chapter));
    }

    public String getCode() { return code; }
    public String getShortDescription() { return shortDescription; }
    public String getLongDescription() { return longDescription; }
    public String getChapter() { return chapter; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public LocalDate getTerminationDate() { return terminationDate; }
    public boolean isActive() { return active; }
    public String getReplacementCode() { return replacementCode; }
    public String getCodeSetVersion() { return codeSetVersion; }
    public String getSource() { return source; }
    public String getImportJobId() { return importJobId; }
    public String getProvenance() { return provenance; }
    public double getConfidence() { return confidence; }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
