package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "code_synonym", indexes = {
        @Index(name = "idx_synonym_term", columnList = "term"),
        @Index(name = "idx_synonym_code_type", columnList = "codeType")
})
public class CodeSynonymEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codeType;

    @Column(nullable = false)
    private String term;

    @Column(nullable = false)
    private String synonyms;

    private String importJobId;

    protected CodeSynonymEntity() {
    }

    public CodeSynonymEntity(String codeType, String term, String synonyms, String importJobId) {
        this.codeType = codeType;
        this.term = term;
        this.synonyms = synonyms;
        this.importJobId = importJobId;
    }

    public String getCodeType() { return codeType; }
    public String getTerm() { return term; }
    public String getSynonyms() { return synonyms; }
}
