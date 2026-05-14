package com.hl7decoder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hl7decoder.api.dto.ExportRequest;
import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.Hl7ParseResult;
import com.hl7decoder.model.ValidationIssue;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class ExportService {
    private final Hl7Service hl7Service;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final PhiScannerService phiScannerService;

    public ExportService(Hl7Service hl7Service, ObjectMapper objectMapper) {
        this(hl7Service, objectMapper, new PhiScannerService());
    }

    @Autowired
    public ExportService(Hl7Service hl7Service, ObjectMapper objectMapper, PhiScannerService phiScannerService) {
        this.hl7Service = hl7Service;
        this.objectMapper = objectMapper;
        this.phiScannerService = phiScannerService;
        this.xmlMapper = XmlMapper.builder()
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    public byte[] exportJson(ExportRequest request) {
        return writeJson(parse(request));
    }

    public byte[] exportXml(ExportRequest request) {
        try {
            return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(parse(request));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to export XML", ex);
        }
    }

    public byte[] exportPrettyHl7(ExportRequest request) {
        return parse(request).normalizedMessage().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportCsv(ExportRequest request) {
        Hl7ParseResult result = parse(request);
        StringBuilder csv = new StringBuilder("severity,location,description,suggestedFix\n");
        for (ValidationIssue issue : result.issues()) {
            csv.append(escape(issue.severity().name())).append(',')
                    .append(escape(issue.location())).append(',')
                    .append(escape(issue.description())).append(',')
                    .append(escape(issue.suggestedFix())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(ExportRequest request) {
        Hl7ParseResult result = parse(request);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("HL7 Validation Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Message metadata"));
            document.add(new Paragraph("Version: " + nullSafe(result.metadata().hl7Version())));
            document.add(new Paragraph("Type: " + nullSafe(result.metadata().messageType()) + "^" + nullSafe(result.metadata().triggerEvent())));
            document.add(new Paragraph("Control ID: " + nullSafe(result.metadata().controlId())));
            document.add(new Paragraph("Validation summary: " + result.summary().errors() + " errors, "
                    + result.summary().warnings() + " warnings, " + result.summary().info() + " info"));
            document.add(new Paragraph("Issues"));
            for (ValidationIssue issue : result.issues()) {
                document.add(new Paragraph(issue.severity() + " " + issue.location() + ": " + issue.description()
                        + " Suggested fix: " + issue.suggestedFix()));
            }
            document.add(new Paragraph("Raw HL7 message"));
            document.add(new Paragraph(result.normalizedMessage(), FontFactory.getFont(FontFactory.COURIER, 9)));
            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Unable to export PDF", ex);
        }
    }

    private Hl7ParseResult parse(ExportRequest request) {
        String message = Boolean.TRUE.equals(request.redactPhi()) ? phiScannerService.redact(request.message()) : request.message();
        return hl7Service.parseAndValidate(new Hl7Request(message, request.mode(), false));
    }

    private byte[] writeJson(Hl7ParseResult result) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(result);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to export JSON", ex);
        }
    }

    private String escape(String value) {
        String safe = nullSafe(value).replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
