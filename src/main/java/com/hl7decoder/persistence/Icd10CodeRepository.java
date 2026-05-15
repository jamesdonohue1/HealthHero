package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Icd10CodeRepository extends JpaRepository<Icd10CodeEntity, String> {
    List<Icd10CodeEntity> findTop25ByCodeContainingIgnoreCaseOrShortDescriptionContainingIgnoreCaseOrLongDescriptionContainingIgnoreCase(
            String code,
            String shortDescription,
            String longDescription
    );

    void deleteByImportJobId(String importJobId);
}
