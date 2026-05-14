package com.hl7decoder.api;

import com.hl7decoder.api.dto.compliance.RetentionSettingsRequest;
import com.hl7decoder.api.dto.compliance.TextRedactionRequest;
import com.hl7decoder.model.compliance.AuditEventResponse;
import com.hl7decoder.model.compliance.PhiScanResponse;
import com.hl7decoder.model.compliance.RetentionSettingsResponse;
import com.hl7decoder.security.AuthenticatedPrincipal;
import com.hl7decoder.service.AuditService;
import com.hl7decoder.service.ComplianceService;
import com.hl7decoder.service.PhiScannerService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {
    private final PhiScannerService phiScannerService;
    private final AuditService auditService;
    private final ComplianceService complianceService;

    public ComplianceController(PhiScannerService phiScannerService, AuditService auditService, ComplianceService complianceService) {
        this.phiScannerService = phiScannerService;
        this.auditService = auditService;
        this.complianceService = complianceService;
    }

    @PostMapping("/phi-preview")
    public PhiScanResponse phiPreview(@Valid @RequestBody TextRedactionRequest request) {
        return phiScannerService.scan(request.text());
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN','PLATFORM_ADMIN')")
    public List<AuditEventResponse> audit(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return auditService.search(principal.organizationId(), from, to);
    }

    @GetMapping("/audit/export")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<byte[]> auditExport(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("audit-events.csv").build().toString())
                .body(auditService.exportCsv(principal.organizationId(), from, to));
    }

    @GetMapping("/retention")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN','PLATFORM_ADMIN')")
    public RetentionSettingsResponse retention(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return complianceService.retention(principal.organizationId());
    }

    @PatchMapping("/retention")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN','PLATFORM_ADMIN')")
    public RetentionSettingsResponse updateRetention(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                     @Valid @RequestBody RetentionSettingsRequest request) {
        return complianceService.updateRetention(principal.organizationId(), principal.userId(), request);
    }

    @PostMapping("/retention/apply")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN','PLATFORM_ADMIN')")
    public Map<String, Object> applyRetention(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        complianceService.applyRetention(principal.organizationId(), principal.userId());
        return Map.of("ok", true);
    }

    @GetMapping("/policy")
    public Map<String, String> policy() {
        return Map.of(
                "loggingPolicy", complianceService.loggingPolicy(),
                "encryptionAndKeyRotation", complianceService.encryptionStrategy(),
                "terms", "Use Healthcare Hero only with authorization. Coding output is informational and must be verified before use.",
                "privacy", "Do not submit PHI unless your organization is authorized to process it in this deployment."
        );
    }
}
