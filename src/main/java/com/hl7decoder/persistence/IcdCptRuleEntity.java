package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "icd_cpt_rule")
public class IcdCptRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String icd10Code;

    @Column(length = 20)
    private String cptCode;

    private String payer;
    private String ruleType;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String source;
    private LocalDate effectiveDate;
    private LocalDate expirationDate;
    private String importJobId;
    private String provenance;
    private double confidence = 0.75;
    private Long policySourceId;

    protected IcdCptRuleEntity() {
    }

    public IcdCptRuleEntity(String icd10Code, String cptCode, String payer, String ruleType, String status,
                            String notes, String source, LocalDate effectiveDate, LocalDate expirationDate,
                            String importJobId, String provenance, double confidence, Long policySourceId) {
        this.icd10Code = icd10Code;
        this.cptCode = cptCode;
        this.payer = payer;
        this.ruleType = ruleType;
        this.status = status;
        this.notes = notes;
        this.source = source;
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
        this.importJobId = importJobId;
        this.provenance = provenance;
        this.confidence = confidence;
        this.policySourceId = policySourceId;
    }

    public String getIcd10Code() { return icd10Code; }
    public String getCptCode() { return cptCode; }
    public String getPayer() { return payer; }
    public String getRuleType() { return ruleType; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getSource() { return source; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public String getImportJobId() { return importJobId; }
    public String getProvenance() { return provenance; }
    public double getConfidence() { return confidence; }
    public Long getPolicySourceId() { return policySourceId; }
}
