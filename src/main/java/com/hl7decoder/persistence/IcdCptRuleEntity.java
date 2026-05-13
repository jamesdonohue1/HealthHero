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
}
