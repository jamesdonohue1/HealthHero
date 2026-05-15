package com.hl7decoder.service;

import com.hl7decoder.model.compliance.AuditAction;
import com.hl7decoder.model.compliance.AuditEventResponse;
import com.hl7decoder.persistence.AuditEvent;
import com.hl7decoder.persistence.AuditEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository repository;
    private final PhiScannerService phiScannerService;

    public AuditService(AuditEventRepository repository, PhiScannerService phiScannerService) {
        this.repository = repository;
        this.phiScannerService = phiScannerService;
    }

    @Transactional
    public void record(AuditAction action, UUID organizationId, UUID userId, String resourceType,
                       String resourceId, boolean success, String details) {
        String safeDetails = phiScannerService.scan(details == null ? "" : details).redactedText();
        repository.save(new AuditEvent(UUID.randomUUID(), Instant.now(), action, organizationId, userId,
                resourceType, resourceId, success, truncate(safeDetails)));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> search(UUID organizationId, Instant from, Instant to) {
        Instant effectiveFrom = from == null ? Instant.now().minusSeconds(30L * 24 * 60 * 60) : from;
        Instant effectiveTo = to == null ? Instant.now() : to;
        return repository.findByOrganizationIdAndOccurredAtBetweenOrderByOccurredAtDesc(organizationId, effectiveFrom, effectiveTo)
                .stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID organizationId, Instant from, Instant to) {
        StringBuilder csv = new StringBuilder("occurredAt,action,organizationId,userId,resourceType,resourceId,success,details\n");
        for (AuditEventResponse event : search(organizationId, from, to)) {
            csv.append(escape(event.occurredAt().toString())).append(',')
                    .append(escape(event.action())).append(',')
                    .append(escape(string(event.organizationId()))).append(',')
                    .append(escape(string(event.userId()))).append(',')
                    .append(escape(event.resourceType())).append(',')
                    .append(escape(event.resourceId())).append(',')
                    .append(event.success()).append(',')
                    .append(escape(event.details())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Scheduled(cron = "${app.audit.cleanup-cron:0 30 2 * * *}")
    @Transactional
    public void deleteExpiredAuditEvents() {
        repository.deleteAll(repository.findByOccurredAtBefore(Instant.now().minusSeconds(365L * 24 * 60 * 60)));
    }

    private AuditEventResponse response(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getOccurredAt(), event.getAction().name(), event.getOrganizationId(),
                event.getUserId(), event.getResourceType(), event.getResourceId(), event.isSuccess(), event.getDetails());
    }

    private String truncate(String details) {
        return details.length() <= 2000 ? details : details.substring(0, 2000);
    }

    private String escape(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }
}
