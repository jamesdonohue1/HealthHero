package com.hl7decoder.service;

import com.hl7decoder.api.dto.ai.AiAssistRequest;
import com.hl7decoder.model.ai.AiAssistResponse;
import com.hl7decoder.model.ai.AiAuditEventResponse;
import com.hl7decoder.persistence.AiAuditEvent;
import com.hl7decoder.persistence.AiAuditEventRepository;
import com.hl7decoder.security.AuthenticatedPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AiAssistService {
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern MRN = Pattern.compile("\\b(?:MRN|member|patient|id)[:#\\s-]*[A-Z0-9]{5,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HL7_NAME = Pattern.compile("\\b[A-Z][A-Z'-]{1,}\\^[A-Z][A-Z'-]{1,}\\b");
    private static final Pattern DOB = Pattern.compile("\\b(?:19|20)\\d{2}[01]\\d[0-3]\\d\\b");

    private final AiAuditEventRepository auditRepository;
    private final String provider;
    private final boolean localOnly;
    private final boolean defaultMaskPhi;
    private final String defaultModel;

    public AiAssistService(
            AiAuditEventRepository auditRepository,
            @Value("${app.ai.provider:local}") String provider,
            @Value("${app.ai.local-only:true}") boolean localOnly,
            @Value("${app.ai.mask-phi:true}") boolean defaultMaskPhi,
            @Value("${app.ai.default-model:healthcare-hero-local-rules}") String defaultModel
    ) {
        this.auditRepository = auditRepository;
        this.provider = provider;
        this.localOnly = localOnly;
        this.defaultMaskPhi = defaultMaskPhi;
        this.defaultModel = defaultModel;
    }

    public AiAssistResponse assist(AiAssistRequest request, AuthenticatedPrincipal principal) {
        boolean redacted = request.redactPhi() == null ? defaultMaskPhi : request.redactPhi();
        String promptType = normalizePromptType(request.promptType());
        String model = request.model() == null || request.model().isBlank() ? defaultModel : request.model().trim();
        String safeInput = redacted ? maskPhi(request.inputText()) : request.inputText();
        int tokenEstimate = Math.max(1, safeInput.length() / 4);
        List<String> suggestions = suggestions(promptType, safeInput);
        List<String> warnings = warnings(promptType, safeInput, redacted);
        String approvalStatus = Boolean.TRUE.equals(request.requireHumanApproval()) || isCodingPrompt(promptType) ? "PENDING_HUMAN_REVIEW" : "READY_FOR_REVIEW";

        AiAssistResponse response = new AiAssistResponse(
                promptType,
                localOnly ? "local" : provider,
                model,
                !localOnly,
                redacted,
                tokenEstimate,
                summary(promptType, safeInput),
                suggestions,
                warnings,
                approvalStatus,
                Instant.now()
        );
        recordAudit(response, principal);
        return response;
    }

    public List<AiAuditEventResponse> audit(AuthenticatedPrincipal principal) {
        return auditRepository.findTop100ByOrganizationIdOrderByOccurredAtDesc(principal.organizationId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void recordAudit(AiAssistResponse response, AuthenticatedPrincipal principal) {
        AiAuditEvent event = new AiAuditEvent();
        event.setOrganizationId(principal.organizationId());
        event.setUserId(principal.userId());
        event.setPromptType(response.promptType());
        event.setProvider(response.provider());
        event.setModel(response.model());
        event.setTokenEstimate(response.tokenEstimate());
        event.setRedacted(response.redacted());
        event.setExternalCall(response.externalCall());
        event.setApprovalStatus(response.approvalStatus());
        event.setDetails("Stored metadata only; raw prompt/output omitted by default.");
        auditRepository.save(event);
    }

    private AiAuditEventResponse toResponse(AiAuditEvent event) {
        return new AiAuditEventResponse(
                event.getId(),
                event.getOccurredAt(),
                event.getOrganizationId(),
                event.getUserId(),
                event.getPromptType(),
                event.getProvider(),
                event.getModel(),
                event.getTokenEstimate(),
                event.isRedacted(),
                event.isExternalCall(),
                event.getApprovalStatus(),
                event.getDetails()
        );
    }

    private String normalizePromptType(String promptType) {
        return promptType.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private boolean isCodingPrompt(String promptType) {
        return promptType.contains("ICD") || promptType.contains("CPT") || promptType.contains("CODING");
    }

    private String maskPhi(String input) {
        String masked = SSN.matcher(input).replaceAll("[SSN]");
        masked = MRN.matcher(masked).replaceAll("[IDENTIFIER]");
        masked = HL7_NAME.matcher(masked).replaceAll("[PATIENT_NAME]");
        return DOB.matcher(masked).replaceAll("[DATE]");
    }

    private String summary(String promptType, String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        if (promptType.contains("PRIOR_AUTH")) {
            return "Prior authorization summary prepared with documentation checklist focus.";
        }
        if (promptType.contains("DENIAL")) {
            return "Denial root-cause summary prepared with appeal risk indicators.";
        }
        if (promptType.contains("DOCUMENTATION")) {
            return "Documentation specificity review prepared for human follow-up.";
        }
        if (promptType.contains("CPT")) {
            return "Procedure-code suggestion set prepared for coder review.";
        }
        if (promptType.contains("ICD")) {
            return "Diagnosis-code refinement suggestions prepared for coder review.";
        }
        if (normalized.contains("denied")) {
            return "Clinical/revenue-cycle note reviewed for denial drivers.";
        }
        return "Local AI-assist workflow completed without sending data to an external model.";
    }

    private List<String> suggestions(String promptType, String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        if (promptType.contains("ICD") || promptType.contains("DOCUMENTATION")) {
            suggestions.add("Confirm laterality, encounter type, acuity, and causal relationships before final code selection.");
            if (normalized.contains("diabetes")) {
                suggestions.add("Document complication status and control/type details for diabetes-related ICD specificity.");
            }
        }
        if (promptType.contains("CPT") || promptType.contains("CODING")) {
            suggestions.add("Validate CPT/HCPCS candidates against payer policy, modifiers, units, and documentation support.");
            if (normalized.contains("xray") || normalized.contains("x-ray")) {
                suggestions.add("Capture view count and anatomical site for radiology procedure selection.");
            }
        }
        if (promptType.contains("PRIOR_AUTH")) {
            suggestions.add("Summarize diagnosis, requested service, failed conservative therapy, clinical indication, and attached evidence.");
        }
        if (promptType.contains("DENIAL")) {
            suggestions.add("Group root causes by medical necessity, eligibility, coding mismatch, missing documentation, and timeliness.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Review generated guidance before using it in coding, billing, or clinical documentation workflows.");
        }
        return suggestions;
    }

    private List<String> warnings(String promptType, String input, boolean redacted) {
        List<String> warnings = new ArrayList<>();
        if (!redacted && hasPhi(input)) {
            warnings.add("Possible PHI detected and redaction was disabled for this request.");
        }
        if (isCodingPrompt(promptType)) {
            warnings.add("Human coder approval is required before billing or claim submission.");
        }
        if (localOnly) {
            warnings.add("Local-only mode is enabled; no external AI provider was called.");
        }
        return warnings;
    }

    private boolean hasPhi(String input) {
        return SSN.matcher(input).find() || MRN.matcher(input).find() || HL7_NAME.matcher(input).find() || DOB.matcher(input).find();
    }
}
