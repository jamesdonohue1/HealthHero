package com.hl7decoder.service;

import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.Hl7Field;
import com.hl7decoder.model.Hl7ParseResult;
import com.hl7decoder.model.Hl7Segment;
import com.hl7decoder.model.ValidationIssue;
import com.hl7decoder.model.ValidationMode;
import com.hl7decoder.model.ValidationSeverity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PlatformRoadmapService {
    private static final Pattern HL7_TS = Pattern.compile("\\d{4,14}([+-]\\d{4})?");
    private static final Pattern POSSIBLE_PHI = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b|\\b\\d{10}\\b|\\b[A-Z][a-z]+\\^[A-Z][a-z]+\\b");
    private final Hl7Service hl7Service;

    public PlatformRoadmapService(Hl7Service hl7Service) {
        this.hl7Service = hl7Service;
    }

    public Map<String, Object> profileValidate(String message, ValidationMode mode) {
        Hl7ParseResult parsed = hl7Service.parseAndValidate(new Hl7Request(message, mode == null ? ValidationMode.STANDARD : mode));
        List<ValidationIssue> advancedIssues = new ArrayList<>(parsed.issues());
        validateTimestamps(parsed, advancedIssues);
        validateFieldLengths(parsed, advancedIssues);
        validateEscapeSequences(parsed, advancedIssues);
        validateAckNack(parsed, advancedIssues);
        return Map.of(
                "profile", parsed.metadata().messageType() == null ? "UNKNOWN" : parsed.metadata().messageType(),
                "parsed", parsed,
                "advancedIssues", advancedIssues,
                "repairSuggestions", advancedIssues.stream().map(ValidationIssue::suggestedFix).distinct().toList()
        );
    }

    public Map<String, Object> hl7DeepAnalysis(String message) {
        Hl7ParseResult parsed = hl7Service.parseAndValidate(new Hl7Request(message, ValidationMode.STANDARD));
        List<Map<String, String>> diagnosisLookups = parsed.segments().stream()
                .filter(segment -> "DG1".equals(segment.name()))
                .map(segment -> Map.of(
                        "segment", "DG1-" + segment.index(),
                        "code", value(segment, 3),
                        "description", value(segment, 4),
                        "oneClickLookup", "/api/icd10/search"))
                .toList();
        List<Map<String, String>> labObservations = parsed.segments().stream()
                .filter(segment -> "OBX".equals(segment.name()))
                .map(segment -> Map.of(
                        "segment", "OBX-" + segment.index(),
                        "test", value(segment, 3),
                        "value", value(segment, 5),
                        "units", value(segment, 6),
                        "flag", value(segment, 8)))
                .toList();
        List<Map<String, Object>> inlineHighlights = parsed.issues().stream()
                .map(issue -> Map.<String, Object>of(
                        "location", issue.location(),
                        "severity", issue.severity().name(),
                        "description", issue.description()))
                .toList();
        return Map.of(
                "messageType", parsed.metadata().messageType(),
                "profile", parsed.metadata().messageType() == null ? "CUSTOM" : parsed.metadata().messageType(),
                "diagnosisLookups", diagnosisLookups,
                "labObservations", labObservations,
                "inlineHighlights", inlineHighlights,
                "ackNack", ackNackWorkflow(parsed),
                "sequencing", sequencing(parsed),
                "sideBySideRepairReady", true
        );
    }

    public Map<String, Object> mappingTemplate(String sourceType) {
        String source = sourceType == null || sourceType.isBlank() ? "HL7" : sourceType.toUpperCase(Locale.ROOT);
        List<Map<String, String>> mappings = List.of(
                Map.of("source", "PID-3", "target", "Patient.identifier", "required", "true"),
                Map.of("source", "PID-5", "target", "Patient.name", "required", "true"),
                Map.of("source", "PID-7", "target", "Patient.birthDate", "required", "false"),
                Map.of("source", "PV1-2", "target", "Encounter.class", "required", "false"),
                Map.of("source", "OBR-4", "target", "DiagnosticReport.code", "required", "false"),
                Map.of("source", "OBX-3", "target", "Observation.code", "required", "true"),
                Map.of("source", "OBX-5", "target", "Observation.value[x]", "required", "false")
        );
        return Map.of(
                "sourceType", source,
                "targetType", source.startsWith("FHIR") ? "HL7v2 ORU^R01" : "FHIR R4-like Bundle",
                "mappings", mappings,
                "batchSupported", true
        );
    }

    public Map<String, Object> syntheticExportManifest() {
        return Map.of(
                "formats", List.of(".hl7", "json", "xml", "csv"),
                "bundleTypes", List.of("single-patient", "batch-patient", "claim-package"),
                "notes", List.of("Generated data is fake and suitable for interface testing.", "Batch downloads can be assembled from generated HL7/FHIR/X12 arrays.")
        );
    }

    public Map<String, Object> priorAuth(String text) {
        List<String> checklist = new ArrayList<>(List.of("Patient demographics", "Insurance member ID", "Ordering provider NPI", "Requested CPT/HCPCS code", "Primary ICD-10 diagnosis"));
        if (containsAny(text, "mri", "ct", "imaging")) {
            checklist.add("Conservative therapy dates and imaging rationale");
        }
        if (containsAny(text, "drug", "medication", "rx")) {
            checklist.add("Prior failed medications and dose history");
        }
        return Map.of(
                "riskLevel", containsAny(text, "urgent", "stat") ? "HIGH" : "MEDIUM",
                "missingInfo", checklist,
                "packet", List.of("Cover sheet", "Clinical summary", "Diagnosis/procedure codes", "Conservative treatment proof", "Relevant imaging/labs"),
                "summary", "Prior authorization packet checklist generated from submitted context.",
                "payerNotes", List.of("Verify payer portal requirements.", "Attach relevant clinical notes before submission.")
        );
    }

    public Map<String, Object> denial(String text) {
        List<String> reasons = new ArrayList<>();
        if (containsAny(text, "medical necessity", "not necessary", "lcd")) {
            reasons.add("Medical necessity denial pattern");
        }
        if (containsAny(text, "timely", "filing")) {
            reasons.add("Timely filing risk");
        }
        if (containsAny(text, "authorization", "precert")) {
            reasons.add("Authorization mismatch");
        }
        if (reasons.isEmpty()) {
            reasons.add("General claim review");
        }
        return Map.of(
                "denialScore", Math.min(95, 35 + reasons.size() * 20),
                "rootCauses", reasons,
                "appealActions", List.of("Gather remittance details.", "Attach clinical documentation.", "Validate CPT/ICD and modifier set."),
                "appealPacket", List.of("Appeal letter", "EOB/835 adjustment details", "Medical necessity evidence", "Corrected claim when needed")
        );
    }

    public Map<String, Object> cdi(String text) {
        List<String> prompts = new ArrayList<>();
        if (!containsAny(text, "acute", "chronic")) {
            prompts.add("Clarify acuity when clinically supported.");
        }
        if (!containsAny(text, "left", "right", "bilateral")) {
            prompts.add("Clarify laterality for diagnoses where applicable.");
        }
        if (!containsAny(text, "mild", "moderate", "severe")) {
            prompts.add("Document severity when it affects coding specificity.");
        }
        return Map.of(
                "documentationScore", Math.max(20, 100 - prompts.size() * 20),
                "specificityPrompts", prompts,
                "comorbidityPrompts", List.of("Review diabetes, CKD, CHF, obesity, and malnutrition when documented.")
        );
    }

    public Map<String, Object> terminology(String text) {
        String normalized = normalize(text);
        List<Map<String, String>> candidates = new ArrayList<>();
        if (normalized.contains("knee pain")) {
            candidates.add(Map.of("system", "ICD-10-CM", "code", "M25.56", "display", "Pain in knee"));
            candidates.add(Map.of("system", "SNOMED CT", "code", "30989003", "display", "Knee pain"));
        }
        if (normalized.contains("cbc")) {
            candidates.add(Map.of("system", "LOINC", "code", "57021-8", "display", "CBC W Auto Differential panel"));
        }
        if (normalized.contains("diabetes")) {
            candidates.add(Map.of("system", "ICD-10-CM", "code", "E11.9", "display", "Type 2 diabetes mellitus without complications"));
        }
        return Map.of("normalizedText", normalized, "candidates", candidates, "supportedSystems", List.of("ICD-10", "SNOMED", "CPT", "LOINC", "RxNorm"));
    }

    public Map<String, Object> labInterpret(String text) {
        List<String> abnormal = new ArrayList<>();
        if (containsAny(text, "H|", "|H", " high", "abnormal")) {
            abnormal.add("High/abnormal result marker detected.");
        }
        if (containsAny(text, "L|", "|L", " low")) {
            abnormal.add("Low result marker detected.");
        }
        if (text != null && text.contains("OBX")) {
            Hl7ParseResult parsed = hl7Service.parseAndValidate(new Hl7Request(text, ValidationMode.STANDARD));
            for (Hl7Segment segment : parsed.segments().stream().filter(segment -> "OBX".equals(segment.name())).toList()) {
                abnormal.add("OBX-" + segment.index() + " " + value(segment, 3) + " = " + value(segment, 5) + " " + value(segment, 6));
            }
        }
        return Map.of("abnormalFindings", abnormal, "trendStatus", "single-sample", "recommendations", List.of("Compare against lab reference range.", "Review prior results before clinical interpretation."));
    }

    public Map<String, Object> monitoring(String text) {
        int messages = text == null || text.isBlank() ? 0 : Math.max(1, text.split("MSH\\|").length - 1);
        int nacks = count(text, "MSA|AE") + count(text, "MSA|AR");
        return Map.of(
                "messageCount", messages,
                "nackCount", nacks,
                "ackRate", messages == 0 ? 0 : Math.round(((messages - nacks) * 100.0 / messages)),
                "queues", List.of(Map.of("name", "inbound-hl7", "depth", messages), Map.of("name", "retry", "depth", nacks)),
                "checkedAt", LocalDateTime.now().toString()
        );
    }

    public Map<String, Object> coding(String text) {
        Map<String, Object> terminology = terminology(text);
        List<?> candidates = (List<?>) terminology.get("candidates");
        return Map.of(
                "icdSuggestions", candidates,
                "cptSuggestions", containsAny(text, "office", "visit", "evaluation") ? List.of("99213", "99214") : List.of(),
                "clarificationPrompts", cdi(text).get("specificityPrompts"),
                "hccSignals", containsAny(text, "diabetes", "ckd", "heart failure") ? List.of("Potential HCC review needed") : List.of()
        );
    }

    public Map<String, Object> sandbox(String text) {
        return Map.of(
                "mockEndpoints", List.of("/api/hl7/parse", "/api/platform/fhir/hl7-to-fhir", "/api/platform/x12/decode"),
                "replayPlan", List.of("Load sample payload.", "Run parser/converter.", "Compare normalized output.", "Capture response contract."),
                "samplePayloadAccepted", text != null && !text.isBlank()
        );
    }

    public Map<String, Object> eligibility(String text) {
        String member = firstToken(text, "SYN000001");
        String x270 = "ISA*00*          *00*          *ZZ*HEALTHHERO    *ZZ*PAYER          *260101*1230*^*00501*000000001*0*T*:~"
                + "GS*HS*HEALTHHERO*PAYER*20260101*1230*1*X*005010X279A1~ST*270*0001*005010X279A1~"
                + "BHT*0022*13*ELIG" + member + "*20260101*1230~NM1*IL*1*DOE*JANE****MI*" + member + "~SE*4*0001~";
        return Map.of(
                "generated270", x270,
                "coverageSummary", List.of("Active coverage status requires payer 271 response.", "Member ID included in generated inquiry."),
                "copayDeductibleFields", List.of("EB-07 copay", "EB-08 coinsurance", "EB-09 deductible"),
                "decoded271Fields", containsAny(text, "EB*") ? List.of("Benefit loop detected", "Review EB-01 status, EB-07 copay, EB-08 coinsurance, EB-09 deductible") : List.of()
        );
    }

    public Map<String, Object> payerRequirements(String text) {
        String payer = text == null || text.isBlank() ? "Default payer" : text.trim();
        return Map.of(
                "payer", payer,
                "requirements", List.of("Verify active coverage", "Check prior authorization by CPT/HCPCS", "Confirm LCD/NCD medical necessity when Medicare applies", "Attach diagnosis-specific documentation"),
                "policySources", List.of("Imported payer rules", "LCD/NCD policy source tracker", "Local medical necessity rules"),
                "lastReviewed", LocalDateTime.now().toLocalDate().toString()
        );
    }

    private Map<String, Object> ackNackWorkflow(Hl7ParseResult parsed) {
        List<String> msa = parsed.segments().stream().filter(segment -> "MSA".equals(segment.name())).map(segment -> value(segment, 1)).toList();
        return Map.of(
                "isAck", "ACK".equals(parsed.metadata().messageType()),
                "ackCodes", msa,
                "status", msa.stream().anyMatch(code -> "AE".equals(code) || "AR".equals(code)) ? "NACK" : msa.isEmpty() ? "NO_ACK" : "ACK"
        );
    }

    private Map<String, Object> sequencing(Hl7ParseResult parsed) {
        String sequence = parsed.segments().stream()
                .filter(segment -> "MSH".equals(segment.name()))
                .findFirst()
                .map(segment -> value(segment, 13))
                .orElse("");
        return Map.of(
                "sequenceNumber", sequence,
                "continuationPointer", parsed.segments().stream().filter(segment -> "MSH".equals(segment.name())).findFirst().map(segment -> value(segment, 14)).orElse(""),
                "messageControlId", parsed.metadata().controlId() == null ? "" : parsed.metadata().controlId(),
                "checks", sequence.isBlank() ? List.of("No MSH-13 sequence number supplied.") : List.of("MSH-13 sequence number present.")
        );
    }

    public Map<String, Object> compliance(String text) {
        boolean phi = text != null && POSSIBLE_PHI.matcher(text).find();
        return Map.of(
                "phiDetected", phi,
                "auditEvents", List.of("tool_request_received", "local_analysis_completed"),
                "sanitizedPreview", phi ? POSSIBLE_PHI.matcher(text).replaceAll("[REDACTED]") : safeText(text),
                "recommendations", List.of("Avoid external logging of PHI.", "Use encrypted storage for saved payloads.", "Track user access for production use.")
        );
    }

    public Map<String, Object> globalSearch(String text) {
        String query = normalize(text);
        Map<String, List<String>> results = new LinkedHashMap<>();
        results.put("HL7", containsAny(query, "msh", "pid", "obx") ? List.of("HL7 Decoder", "HL7 Repair") : List.of());
        results.put("FHIR", containsAny(query, "patient", "observation", "bundle") ? List.of("FHIR Converter", "Mapping Template") : List.of());
        results.put("Claims", containsAny(query, "837", "835", "claim", "denial") ? List.of("X12 Decoder", "Denial Analyzer") : List.of());
        results.put("Coding", containsAny(query, "icd", "cpt", "diagnosis") ? List.of("ICD-10 Search", "Medical Necessity", "AI Coding Assistant") : List.of());
        return Map.of("query", text, "results", results);
    }

    private void validateTimestamps(Hl7ParseResult parsed, List<ValidationIssue> issues) {
        for (Hl7Segment segment : parsed.segments()) {
            for (Hl7Field field : segment.fields()) {
                if (field.name().toLowerCase(Locale.ROOT).contains("date") && !field.value().isBlank() && !HL7_TS.matcher(field.value()).matches()) {
                    issues.add(issue(segment, field, "Timestamp is not in expected HL7 TS format.", "Use yyyyMMddHHmmss or a valid shorter HL7 timestamp."));
                }
            }
        }
    }

    private void validateFieldLengths(Hl7ParseResult parsed, List<ValidationIssue> issues) {
        for (Hl7Segment segment : parsed.segments()) {
            for (Hl7Field field : segment.fields()) {
                if (field.value().length() > 250) {
                    issues.add(issue(segment, field, "Field exceeds the local 250 character safety threshold.", "Move long narrative content to an NTE/TXA segment or configured profile field."));
                }
            }
        }
    }

    private void validateEscapeSequences(Hl7ParseResult parsed, List<ValidationIssue> issues) {
        Set<String> allowed = Set.of("\\F\\", "\\S\\", "\\T\\", "\\R\\", "\\E\\");
        for (Hl7Segment segment : parsed.segments()) {
            for (Hl7Field field : segment.fields()) {
                if (field.value().contains("\\") && allowed.stream().noneMatch(field.value()::contains)) {
                    issues.add(issue(segment, field, "Potential invalid HL7 escape sequence.", "Use standard HL7 escape sequences such as \\F\\, \\S\\, \\T\\, \\R\\, or \\E\\."));
                }
            }
        }
    }

    private void validateAckNack(Hl7ParseResult parsed, List<ValidationIssue> issues) {
        if ("ACK".equals(parsed.metadata().messageType())) {
            boolean hasMsa = parsed.segments().stream().anyMatch(segment -> "MSA".equals(segment.name()));
            if (!hasMsa) {
                issues.add(new ValidationIssue(ValidationSeverity.ERROR, "MSA", null, null, null, "MSA", "ACK message is missing MSA acknowledgment segment.", "Add MSA with AA, AE, or AR status."));
            }
        }
    }

    private ValidationIssue issue(Hl7Segment segment, Hl7Field field, String description, String fix) {
        return new ValidationIssue(ValidationSeverity.WARNING, segment.name(), segment.index(), field.index(), null, segment.name() + "-" + field.index(), description, fix);
    }

    private String value(Hl7Segment segment, int fieldIndex) {
        return segment.fields().stream().filter(field -> field.index() == fieldIndex).findFirst().map(Hl7Field::value).orElse("");
    }

    private boolean containsAny(String text, String... needles) {
        String normalized = normalize(text);
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private int count(String text, String needle) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.split(Pattern.quote(needle), -1).length - 1;
    }

    private String normalize(String text) {
        return safeText(text).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private String firstToken(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.trim().split("\\s+")[0].replaceAll("[^A-Za-z0-9]", "");
    }
}
