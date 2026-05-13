package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedureCodeRepository extends JpaRepository<ProcedureCodeEntity, Long> {
    Optional<ProcedureCodeEntity> findFirstByCodeIgnoreCase(String code);

    List<ProcedureCodeEntity> findTop25ByCodeContainingIgnoreCaseOrShortDescriptionContainingIgnoreCaseOrLongDescriptionContainingIgnoreCase(
            String code,
            String shortDescription,
            String longDescription
    );
}
