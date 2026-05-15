package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "coverage_policy_source")
public class CoveragePolicySourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String payer;
    private String policyType;
    private String policyId;
    private String title;
    private String url;
    private String source;
    private LocalDate effectiveDate;
    private LocalDate expirationDate;
    private String importJobId;

    protected CoveragePolicySourceEntity() {
    }

    public CoveragePolicySourceEntity(String payer, String policyType, String policyId, String title, String url,
                                      String source, LocalDate effectiveDate, LocalDate expirationDate, String importJobId) {
        this.payer = payer;
        this.policyType = policyType;
        this.policyId = policyId;
        this.title = title;
        this.url = url;
        this.source = source;
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
        this.importJobId = importJobId;
    }

    public Long getId() { return id; }
    public String getPayer() { return payer; }
    public String getPolicyType() { return policyType; }
    public String getPolicyId() { return policyId; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getSource() { return source; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
}
