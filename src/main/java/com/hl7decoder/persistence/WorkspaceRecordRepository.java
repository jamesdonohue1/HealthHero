package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRecordRepository extends JpaRepository<WorkspaceRecord, UUID> {
    List<WorkspaceRecord> findTop50ByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);

    List<WorkspaceRecord> findTop50ByOrganizationIdAndRecordTypeOrderByUpdatedAtDesc(UUID organizationId, String recordType);

    List<WorkspaceRecord> findTop50ByOrganizationIdAndSearchTextContainingOrderByUpdatedAtDesc(UUID organizationId, String query);

    Optional<WorkspaceRecord> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
