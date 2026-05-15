package com.hl7decoder.service;

import com.hl7decoder.api.dto.ai.AiAssistRequest;
import com.hl7decoder.persistence.AiAuditEvent;
import com.hl7decoder.persistence.AiAuditEventRepository;
import com.hl7decoder.security.AuthenticatedPrincipal;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssistServiceTest {
    @Test
    void masksPhiAndStoresOnlyAuditMetadata() {
        List<AiAuditEvent> events = new ArrayList<>();
        AiAuditEventRepository repository = auditRepository(events);
        AiAssistService service = new AiAssistService(repository, "local", true, true, "local-rules");

        var principal = principal();
        var response = service.assist(new AiAssistRequest(
                "DENIAL_ROOT_CAUSE",
                "Patient DOE^JANE MRN ABC123 denied for medical necessity.",
                true,
                true,
                null
        ), principal);

        assertThat(response.externalCall()).isFalse();
        assertThat(response.redacted()).isTrue();
        assertThat(response.approvalStatus()).isEqualTo("PENDING_HUMAN_REVIEW");
        assertThat(response.warnings()).anyMatch(warning -> warning.contains("Local-only"));
        AiAuditEvent event = events.getFirst();
        assertThat(event.getOrganizationId()).isEqualTo(principal.organizationId());
        assertThat(event.getUserId()).isEqualTo(principal.userId());
        assertThat(event.getDetails()).contains("metadata only");
        assertThat(event.getDetails()).doesNotContain("DOE");
    }

    @Test
    void returnsAuditEventsForOrganization() {
        AiAuditEvent event = new AiAuditEvent();
        event.setOrganizationId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        event.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        event.setPromptType("ICD_REFINEMENT");
        event.setProvider("local");
        event.setModel("local-rules");
        event.setTokenEstimate(12);
        event.setRedacted(true);
        event.setExternalCall(false);
        event.setApprovalStatus("PENDING_HUMAN_REVIEW");
        event.setDetails("Stored metadata only.");
        AiAuditEventRepository repository = auditRepository(new ArrayList<>(List.of(event)));
        AiAssistService service = new AiAssistService(repository, "local", true, true, "local-rules");

        assertThat(service.audit(principal())).singleElement()
                .satisfies(audit -> {
                    assertThat(audit.promptType()).isEqualTo("ICD_REFINEMENT");
                    assertThat(audit.details()).contains("metadata");
                });
    }

    private AuthenticatedPrincipal principal() {
        return new AuthenticatedPrincipal(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "coder@example.com",
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                Set.of(com.hl7decoder.model.auth.UserRole.CODER),
                "bearer"
        );
    }

    private AiAuditEventRepository auditRepository(List<AiAuditEvent> events) {
        return (AiAuditEventRepository) Proxy.newProxyInstance(
                AiAuditEventRepository.class.getClassLoader(),
                new Class<?>[]{AiAuditEventRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        AiAuditEvent event = (AiAuditEvent) args[0];
                        events.add(event);
                        return event;
                    }
                    if ("findTop100ByOrganizationIdOrderByOccurredAtDesc".equals(method.getName())) {
                        UUID organizationId = (UUID) args[0];
                        return events.stream().filter(event -> event.getOrganizationId().equals(organizationId)).toList();
                    }
                    if ("toString".equals(method.getName())) {
                        return "InMemoryAiAuditEventRepository";
                    }
                    return null;
                }
        );
    }
}
