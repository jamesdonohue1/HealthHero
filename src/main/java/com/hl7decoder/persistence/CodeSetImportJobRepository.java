package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CodeSetImportJobRepository extends JpaRepository<CodeSetImportJob, UUID> {
    List<CodeSetImportJob> findTop25ByOrderByCreatedAtDesc();
}
