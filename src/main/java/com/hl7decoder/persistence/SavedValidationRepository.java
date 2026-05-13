package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SavedValidationRepository extends JpaRepository<SavedValidation, UUID> {
    List<SavedValidation> findByExpiresAtBefore(Instant now);

    java.util.Optional<SavedValidation> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
