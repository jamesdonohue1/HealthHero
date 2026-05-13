package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SavedIcd10SearchRepository extends JpaRepository<SavedIcd10Search, UUID> {
    List<SavedIcd10Search> findByExpiresAtBefore(Instant now);

    List<SavedIcd10Search> findByOrganizationId(UUID organizationId);

    java.util.Optional<SavedIcd10Search> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
