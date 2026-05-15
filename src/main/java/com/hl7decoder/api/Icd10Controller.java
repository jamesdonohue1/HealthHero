package com.hl7decoder.api;

import com.hl7decoder.api.dto.icd10.Icd10AutocompleteRequest;
import com.hl7decoder.api.dto.icd10.Icd10ExportRequest;
import com.hl7decoder.api.dto.icd10.Icd10SearchRequest;
import com.hl7decoder.model.icd10.Icd10AutocompleteResponse;
import com.hl7decoder.model.icd10.Icd10RefineResponse;
import com.hl7decoder.model.icd10.Icd10SavedSearchResponse;
import com.hl7decoder.model.icd10.Icd10SearchResponse;
import com.hl7decoder.model.compliance.AuditAction;
import com.hl7decoder.security.AuthenticatedPrincipal;
import com.hl7decoder.service.AuditService;
import com.hl7decoder.service.Icd10Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/icd10")
public class Icd10Controller {
    private final Icd10Service icd10Service;
    private final AuditService auditService;
    private final Map<String, Window> searchLimits = new ConcurrentHashMap<>();
    private final Map<String, Window> autocompleteLimits = new ConcurrentHashMap<>();
    private final Map<String, Window> exportLimits = new ConcurrentHashMap<>();

    public Icd10Controller(Icd10Service icd10Service) {
        this(icd10Service, null);
    }

    @Autowired
    public Icd10Controller(Icd10Service icd10Service, AuditService auditService) {
        this.icd10Service = icd10Service;
        this.auditService = auditService;
    }

    @PostMapping("/search")
    public Icd10SearchResponse search(@Valid @RequestBody Icd10SearchRequest request, HttpServletRequest servletRequest) {
        rateLimit(searchLimits, clientKey(servletRequest), 20, "Anonymous search limit exceeded. Please wait before searching again.");
        return icd10Service.search(request);
    }

    @PostMapping("/autocomplete")
    public Icd10AutocompleteResponse autocomplete(@Valid @RequestBody Icd10AutocompleteRequest request, HttpServletRequest servletRequest) {
        rateLimit(autocompleteLimits, clientKey(servletRequest), 100, "Anonymous autocomplete limit exceeded. Please wait before searching again.");
        return icd10Service.autocomplete(request);
    }

    @PostMapping("/refine")
    public Icd10RefineResponse refine(@Valid @RequestBody Icd10SearchRequest request) {
        return icd10Service.refine(request);
    }

    @PostMapping("/save")
    public Icd10SavedSearchResponse save(@Valid @RequestBody Icd10SearchRequest request, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return icd10Service.save(request, principal.organizationId(), principal.userId());
    }

    @GetMapping("/history")
    public List<Icd10SavedSearchResponse> history(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return icd10Service.history(principal.organizationId());
    }

    @GetMapping("/saved/{id}")
    public Icd10SavedSearchResponse saved(@PathVariable String id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return icd10Service.saved(id, principal.organizationId());
    }

    @DeleteMapping("/saved/{id}")
    public ResponseEntity<Void> deleteSaved(@PathVariable String id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        icd10Service.deleteSaved(id, principal.organizationId(), principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/export/json")
    public ResponseEntity<byte[]> exportJson(@Valid @RequestBody Icd10ExportRequest request, HttpServletRequest servletRequest,
                                             @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        rateLimit(exportLimits, clientKey(servletRequest), 20, "Anonymous export limit exceeded. Please wait before exporting again.");
        auditExport(principal, "json", request);
        return download(icd10Service.exportJson(request), MediaType.APPLICATION_JSON, "icd10-search.json");
    }

    @PostMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(@Valid @RequestBody Icd10ExportRequest request, HttpServletRequest servletRequest,
                                            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        rateLimit(exportLimits, clientKey(servletRequest), 20, "Anonymous export limit exceeded. Please wait before exporting again.");
        auditExport(principal, "csv", request);
        return download(icd10Service.exportCsv(request), MediaType.parseMediaType("text/csv"), "icd10-search.csv");
    }

    @PostMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@Valid @RequestBody Icd10ExportRequest request, HttpServletRequest servletRequest,
                                            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        rateLimit(exportLimits, clientKey(servletRequest), 10, "Anonymous PDF export limit exceeded. Please wait before exporting again.");
        auditExport(principal, "pdf", request);
        return download(icd10Service.exportPdf(request), MediaType.APPLICATION_PDF, "icd10-search.pdf");
    }

    @PostMapping("/export/text")
    public ResponseEntity<byte[]> exportText(@Valid @RequestBody Icd10ExportRequest request, HttpServletRequest servletRequest,
                                             @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        rateLimit(exportLimits, clientKey(servletRequest), 20, "Anonymous export limit exceeded. Please wait before exporting again.");
        auditExport(principal, "text", request);
        return download(icd10Service.exportText(request), MediaType.TEXT_PLAIN, "icd10-search.txt");
    }

    private ResponseEntity<byte[]> download(byte[] body, MediaType mediaType, String filename) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }

    private void rateLimit(Map<String, Window> limits, String key, int maxRequests, String message) {
        Instant now = Instant.now();
        Window window = limits.compute(key, (ignored, current) -> {
            if (current == null || current.resetAt().isBefore(now)) {
                return new Window(now.plus(Duration.ofHours(1)), 1);
            }
            return new Window(current.resetAt(), current.count() + 1);
        });
        if (window.count() > maxRequests) {
            throw new IllegalArgumentException(message);
        }
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void auditExport(AuthenticatedPrincipal principal, String format, Icd10ExportRequest request) {
        if (principal != null && auditService != null) {
            auditService.record(AuditAction.EXPORT, principal.organizationId(), principal.userId(), "ICD10_EXPORT",
                    format, true, "ICD-10 export; format=" + format + "; redacted=" + Boolean.TRUE.equals(request.redactPhi()));
        }
    }

    private record Window(Instant resetAt, int count) {
    }
}
