package com.hl7decoder.api;

import com.hl7decoder.api.dto.ExportRequest;
import com.hl7decoder.model.compliance.AuditAction;
import com.hl7decoder.security.AuthenticatedPrincipal;
import com.hl7decoder.service.AuditService;
import com.hl7decoder.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exports")
public class ExportController {
    private final ExportService exportService;
    private final AuditService auditService;

    public ExportController(ExportService exportService, AuditService auditService) {
        this.exportService = exportService;
        this.auditService = auditService;
    }

    @PostMapping("/json")
    public ResponseEntity<byte[]> json(@Valid @RequestBody ExportRequest request, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        auditExport(principal, "json", request);
        return file("hl7-validation.json", MediaType.APPLICATION_JSON, exportService.exportJson(request));
    }

    @PostMapping("/xml")
    public ResponseEntity<byte[]> xml(@Valid @RequestBody ExportRequest request, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        auditExport(principal, "xml", request);
        return file("hl7-validation.xml", MediaType.APPLICATION_XML, exportService.exportXml(request));
    }

    @PostMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@Valid @RequestBody ExportRequest request, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        auditExport(principal, "pdf", request);
        return file("hl7-validation-report.pdf", MediaType.APPLICATION_PDF, exportService.exportPdf(request));
    }

    @PostMapping("/hl7")
    public ResponseEntity<byte[]> prettyHl7(@Valid @RequestBody ExportRequest request, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        auditExport(principal, "hl7", request);
        return file("hl7-message.hl7", MediaType.TEXT_PLAIN, exportService.exportPrettyHl7(request));
    }

    @PostMapping("/csv")
    public ResponseEntity<byte[]> csv(@Valid @RequestBody ExportRequest request, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        auditExport(principal, "csv", request);
        return file("hl7-validation.csv", MediaType.parseMediaType("text/csv"), exportService.exportCsv(request));
    }

    private ResponseEntity<byte[]> file(String filename, MediaType type, byte[] content) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }

    private void auditExport(AuthenticatedPrincipal principal, String format, ExportRequest request) {
        if (principal != null) {
            auditService.record(AuditAction.EXPORT, principal.organizationId(), principal.userId(), "HL7_EXPORT",
                    format, true, "HL7 export; format=" + format + "; redacted=" + Boolean.TRUE.equals(request.redactPhi()));
        }
    }
}
