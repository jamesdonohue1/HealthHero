package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiAuditEventRepository extends JpaRepository<AiAuditEvent, UUID> {
    List<AiAuditEvent> findTop100ByOrganizationIdOrderByOccurredAtDesc(UUID organizationId);
}
