package com.hl7decoder.api;

import com.hl7decoder.api.dto.ExportRequest;
import com.hl7decoder.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exports")
public class ExportController {
    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/json")
    public ResponseEntity<byte[]> json(@Valid @RequestBody ExportRequest request) {
        return file("hl7-validation.json", MediaType.APPLICATION_JSON, exportService.exportJson(request));
    }

    @PostMapping("/xml")
    public ResponseEntity<byte[]> xml(@Valid @RequestBody ExportRequest request) {
        return file("hl7-validation.xml", MediaType.APPLICATION_XML, exportService.exportXml(request));
    }

    @PostMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@Valid @RequestBody ExportRequest request) {
        return file("hl7-validation-report.pdf", MediaType.APPLICATION_PDF, exportService.exportPdf(request));
    }

    @PostMapping("/hl7")
    public ResponseEntity<byte[]> prettyHl7(@Valid @RequestBody ExportRequest request) {
        return file("hl7-message.hl7", MediaType.TEXT_PLAIN, exportService.exportPrettyHl7(request));
    }

    @PostMapping("/csv")
    public ResponseEntity<byte[]> csv(@Valid @RequestBody ExportRequest request) {
        return file("hl7-validation.csv", MediaType.parseMediaType("text/csv"), exportService.exportCsv(request));
    }

    private ResponseEntity<byte[]> file(String filename, MediaType type, byte[] content) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
