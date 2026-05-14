package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByOrganizationIdAndOccurredAtBetweenOrderByOccurredAtDesc(UUID organizationId, Instant from, Instant to);

    List<AuditEvent> findByOccurredAtBefore(Instant cutoff);
}
