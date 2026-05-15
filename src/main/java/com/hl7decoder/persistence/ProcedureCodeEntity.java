package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "procedure_code", indexes = {
        @Index(name = "idx_procedure_code", columnList = "code"),
        @Index(name = "idx_procedure_type_version", columnList = "codeType,codeSetVersion"),
        @Index(name = "idx_procedure_effective", columnList = "effectiveDate,terminationDate")
})
public class ProcedureCodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 20)
    private String codeType;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String longDescription;

    private String category;
    private LocalDate effectiveDate;
    private LocalDate terminationDate;
    private boolean active = true;
    private String source;
    private String codeSetVersion;
    private String replacementCode;
    private String importJobId;
    private String provenance;
    private double confidence = 1.0;

    @Lob
    private String searchText;

    protected ProcedureCodeEntity() {
    }

    public ProcedureCodeEntity(String code, String codeType, String shortDescription, String longDescription, String category, LocalDate effectiveDate, LocalDate terminationDate, boolean active, String source) {
        this(code, codeType, shortDescription, longDescription, category, effectiveDate, terminationDate, active, source, null, null, null, source, 1.0);
    }

    public ProcedureCodeEntity(String code, String codeType, String shortDescription, String longDescription, String category,
                               LocalDate effectiveDate, LocalDate terminationDate, boolean active, String source,
                               String codeSetVersion, String replacementCode, String importJobId, String provenance, double confidence) {
        this.code = code;
        this.codeType = codeType;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.category = category;
        this.effectiveDate = effectiveDate;
        this.terminationDate = terminationDate;
        this.active = active;
        this.source = source;
        this.codeSetVersion = codeSetVersion;
        this.replacementCode = replacementCode;
        this.importJobId = importJobId;
        this.provenance = provenance;
        this.confidence = confidence;
        this.searchText = String.join(" ", nullSafe(code), nullSafe(shortDescription), nullSafe(longDescription), nullSafe(category), nullSafe(source));
    }

    public String getCode() {
        return code;
    }

    public String getCodeType() {
        return codeType;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public LocalDate getTerminationDate() {
        return terminationDate;
    }

    public boolean isActive() {
        return active;
    }

    public String getSource() {
        return source;
    }

    public String getCodeSetVersion() {
        return codeSetVersion;
    }

    public String getReplacementCode() {
        return replacementCode;
    }

    public String getImportJobId() {
        return importJobId;
    }

    public String getProvenance() {
        return provenance;
    }

    public double getConfidence() {
        return confidence;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
