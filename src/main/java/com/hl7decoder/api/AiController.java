package com.hl7decoder.api;

import com.hl7decoder.api.dto.ai.AiAssistRequest;
import com.hl7decoder.model.ai.AiAssistResponse;
import com.hl7decoder.model.ai.AiAuditEventResponse;
import com.hl7decoder.security.AuthenticatedPrincipal;
import com.hl7decoder.service.AiAssistService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/ai", "/api/v1/ai"})
public class AiController {
    private final AiAssistService aiAssistService;

    public AiController(AiAssistService aiAssistService) {
        this.aiAssistService = aiAssistService;
    }

    @PostMapping("/assist")
    public AiAssistResponse assist(@AuthenticationPrincipal AuthenticatedPrincipal principal, @Valid @RequestBody AiAssistRequest request) {
        return aiAssistService.assist(request, requirePrincipal(principal));
    }

    @PostMapping("/icd-refine")
    public AiAssistResponse icdRefine(@AuthenticationPrincipal AuthenticatedPrincipal principal, @RequestBody AiAssistRequest request) {
        return aiAssistService.assist(typed("ICD_REFINEMENT", request), requirePrincipal(principal));
    }

    @PostMapping("/cpt-suggest")
    public AiAssistResponse cptSuggest(@AuthenticationPrincipal AuthenticatedPrincipal principal, @RequestBody AiAssistRequest request) {
        return aiAssistService.assist(typed("CPT_SUGGESTION", request), requirePrincipal(principal));
    }

    @PostMapping("/prior-auth-summary")
    public AiAssistResponse priorAuthSummary(@AuthenticationPrincipal AuthenticatedPrincipal principal, @RequestBody AiAssistRequest request) {
        return aiAssistService.assist(typed("PRIOR_AUTH_SUMMARY", request), requirePrincipal(principal));
    }

    @PostMapping("/denial-root-cause")
    public AiAssistResponse denialRootCause(@AuthenticationPrincipal AuthenticatedPrincipal principal, @RequestBody AiAssistRequest request) {
        return aiAssistService.assist(typed("DENIAL_ROOT_CAUSE", request), requirePrincipal(principal));
    }

    @PostMapping("/documentation-specificity")
    public AiAssistResponse documentationSpecificity(@AuthenticationPrincipal AuthenticatedPrincipal principal, @RequestBody AiAssistRequest request) {
        return aiAssistService.assist(typed("DOCUMENTATION_SPECIFICITY", request), requirePrincipal(principal));
    }

    @GetMapping("/audit")
    public List<AiAuditEventResponse> audit(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return aiAssistService.audit(requirePrincipal(principal));
    }

    private AiAssistRequest typed(String promptType, AiAssistRequest request) {
        return new AiAssistRequest(promptType, request.inputText(), request.redactPhi(), true, request.model());
    }

    private AuthenticatedPrincipal requirePrincipal(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new BadCredentialsException("Authentication required.");
        }
        return principal;
    }
}
