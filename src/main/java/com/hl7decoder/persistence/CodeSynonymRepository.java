package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeSynonymRepository extends JpaRepository<CodeSynonymEntity, Long> {
    List<CodeSynonymEntity> findByCodeTypeIgnoreCaseAndTermContainingIgnoreCase(String codeType, String term);

    void deleteByImportJobId(String importJobId);
}
