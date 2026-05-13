package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "procedure_code")
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

    protected ProcedureCodeEntity() {
    }

    public ProcedureCodeEntity(String code, String codeType, String shortDescription, String longDescription, String category, LocalDate effectiveDate, LocalDate terminationDate, boolean active, String source) {
        this.code = code;
        this.codeType = codeType;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.category = category;
        this.effectiveDate = effectiveDate;
        this.terminationDate = terminationDate;
        this.active = active;
        this.source = source;
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
}
