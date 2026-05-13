package com.hl7decoder.service;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.GenericParser;
import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.Hl7Component;
import com.hl7decoder.model.Hl7Field;
import com.hl7decoder.model.Hl7ParseResult;
import com.hl7decoder.model.Hl7Segment;
import com.hl7decoder.model.MessageMetadata;
import com.hl7decoder.model.ValidationIssue;
import com.hl7decoder.model.ValidationMode;
import com.hl7decoder.model.ValidationSeverity;
import com.hl7decoder.model.ValidationSummary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class Hl7Service {
    private static final Set<String> KNOWN_SEGMENTS = Set.of("MSH", "EVN", "PID", "PD1", "NK1", "PV1", "PV2", "ORC", "OBR",
            "OBX", "NTE", "SCH", "AIL", "AIP", "AIS", "AIG", "FT1", "DG1", "PR1", "TXA", "MSA", "ERR", "QRD", "QRF",
            "RXA", "RXR", "RXE", "RXO", "RXC", "MFE", "MFI", "IN1", "IN2", "GT1", "AL1");
    private static final Map<String, List<String>> REQUIRED_SEGMENTS = Map.of(
            "ADT", List.of("MSH", "EVN", "PID", "PV1"),
            "ORM", List.of("MSH", "PID", "ORC", "OBR"),
            "ORU", List.of("MSH", "PID", "OBR", "OBX"),
            "SIU", List.of("MSH", "SCH"),
            "DFT", List.of("MSH", "PID", "FT1"),
            "MDM", List.of("MSH", "PID", "TXA"),
            "BAR", List.of("MSH", "PID"),
            "VXU", List.of("MSH", "PID", "RXA"),
            "MFN", List.of("MSH", "MFI", "MFE")
    );
    private static final Map<String, String> SEGMENT_DESCRIPTIONS = Map.ofEntries(
            Map.entry("MSH", "Message Header"),
            Map.entry("EVN", "Event Type"),
            Map.entry("PID", "Patient Identification"),
            Map.entry("PV1", "Patient Visit"),
            Map.entry("ORC", "Common Order"),
            Map.entry("OBR", "Observation Request"),
            Map.entry("OBX", "Observation Result"),
            Map.entry("SCH", "Schedule Activity"),
            Map.entry("FT1", "Financial Transaction"),
            Map.entry("TXA", "Document Notification"),
            Map.entry("RXA", "Pharmacy/Treatment Administration"),
            Map.entry("MFI", "Master File Identification"),
            Map.entry("MFE", "Master File Entry")
    );
    private static final Map<String, Map<Integer, String>> FIELD_NAMES = Map.ofEntries(
            Map.entry("MSH", Map.ofEntries(
                    Map.entry(1, "Field Separator"),
                    Map.entry(2, "Encoding Characters"),
                    Map.entry(3, "Sending Application"),
                    Map.entry(4, "Sending Facility"),
                    Map.entry(5, "Receiving Application"),
                    Map.entry(6, "Receiving Facility"),
                    Map.entry(7, "Date/Time of Message"),
                    Map.entry(9, "Message Type"),
                    Map.entry(10, "Message Control ID"),
                    Map.entry(11, "Processing ID"),
                    Map.entry(12, "Version ID"),
                    Map.entry(13, "Sequence Number"),
                    Map.entry(14, "Continuation Pointer"),
                    Map.entry(15, "Accept Acknowledgment Type"),
                    Map.entry(16, "Application Acknowledgment Type"),
                    Map.entry(17, "Country Code"),
                    Map.entry(18, "Character Set"),
                    Map.entry(19, "Principal Language of Message"),
                    Map.entry(20, "Alternate Character Set Handling Scheme"),
                    Map.entry(21, "Message Profile Identifier"))),
            Map.entry("EVN", Map.ofEntries(
                    Map.entry(1, "Event Type Code"),
                    Map.entry(2, "Recorded Date/Time"),
                    Map.entry(3, "Date/Time Planned Event"),
                    Map.entry(4, "Event Reason Code"),
                    Map.entry(5, "Operator ID"),
                    Map.entry(6, "Event Occurred"),
                    Map.entry(7, "Event Facility"))),
            Map.entry("PID", Map.ofEntries(
                    Map.entry(1, "Set ID - PID"),
                    Map.entry(2, "Patient ID"),
                    Map.entry(3, "Patient Identifier List"),
                    Map.entry(4, "Alternate Patient ID - PID"),
                    Map.entry(5, "Patient Name"),
                    Map.entry(6, "Mother's Maiden Name"),
                    Map.entry(7, "Date/Time of Birth"),
                    Map.entry(8, "Administrative Sex"),
                    Map.entry(9, "Patient Alias"),
                    Map.entry(10, "Race"),
                    Map.entry(11, "Patient Address"),
                    Map.entry(12, "County Code"),
                    Map.entry(13, "Phone Number - Home"),
                    Map.entry(14, "Phone Number - Business"),
                    Map.entry(15, "Primary Language"),
                    Map.entry(16, "Marital Status"),
                    Map.entry(17, "Religion"),
                    Map.entry(18, "Patient Account Number"),
                    Map.entry(19, "SSN Number - Patient"),
                    Map.entry(20, "Driver's License Number - Patient"),
                    Map.entry(21, "Mother's Identifier"),
                    Map.entry(22, "Ethnic Group"),
                    Map.entry(23, "Birth Place"),
                    Map.entry(24, "Multiple Birth Indicator"),
                    Map.entry(25, "Birth Order"),
                    Map.entry(26, "Citizenship"),
                    Map.entry(27, "Veterans Military Status"),
                    Map.entry(28, "Nationality"),
                    Map.entry(29, "Patient Death Date and Time"),
                    Map.entry(30, "Patient Death Indicator"),
                    Map.entry(31, "Identity Unknown Indicator"),
                    Map.entry(32, "Identity Reliability Code"),
                    Map.entry(33, "Last Update Date/Time"),
                    Map.entry(34, "Last Update Facility"),
                    Map.entry(35, "Species Code"),
                    Map.entry(36, "Breed Code"),
                    Map.entry(37, "Strain"),
                    Map.entry(38, "Production Class Code"),
                    Map.entry(39, "Tribal Citizenship"))),
            Map.entry("PV1", Map.ofEntries(
                    Map.entry(1, "Set ID - PV1"),
                    Map.entry(2, "Patient Class"),
                    Map.entry(3, "Assigned Patient Location"),
                    Map.entry(4, "Admission Type"),
                    Map.entry(5, "Preadmit Number"),
                    Map.entry(6, "Prior Patient Location"),
                    Map.entry(7, "Attending Doctor"),
                    Map.entry(8, "Referring Doctor"),
                    Map.entry(9, "Consulting Doctor"),
                    Map.entry(10, "Hospital Service"),
                    Map.entry(11, "Temporary Location"),
                    Map.entry(12, "Preadmit Test Indicator"),
                    Map.entry(13, "Readmission Indicator"),
                    Map.entry(14, "Admit Source"),
                    Map.entry(15, "Ambulatory Status"),
                    Map.entry(16, "VIP Indicator"),
                    Map.entry(17, "Admitting Doctor"),
                    Map.entry(18, "Patient Type"),
                    Map.entry(19, "Visit Number"),
                    Map.entry(20, "Financial Class"),
                    Map.entry(21, "Charge Price Indicator"),
                    Map.entry(22, "Courtesy Code"),
                    Map.entry(23, "Credit Rating"),
                    Map.entry(24, "Contract Code"),
                    Map.entry(25, "Contract Effective Date"),
                    Map.entry(26, "Contract Amount"),
                    Map.entry(27, "Contract Period"),
                    Map.entry(28, "Interest Code"),
                    Map.entry(29, "Transfer to Bad Debt Code"),
                    Map.entry(30, "Transfer to Bad Debt Date"),
                    Map.entry(31, "Bad Debt Agency Code"),
                    Map.entry(32, "Bad Debt Transfer Amount"),
                    Map.entry(33, "Bad Debt Recovery Amount"),
                    Map.entry(34, "Delete Account Indicator"),
                    Map.entry(35, "Delete Account Date"),
                    Map.entry(36, "Discharge Disposition"),
                    Map.entry(37, "Discharged to Location"),
                    Map.entry(38, "Diet Type"),
                    Map.entry(39, "Servicing Facility"),
                    Map.entry(40, "Bed Status"),
                    Map.entry(41, "Account Status"),
                    Map.entry(42, "Pending Location"),
                    Map.entry(43, "Prior Temporary Location"),
                    Map.entry(44, "Admit Date/Time"),
                    Map.entry(45, "Discharge Date/Time"),
                    Map.entry(46, "Current Patient Balance"),
                    Map.entry(47, "Total Charges"),
                    Map.entry(48, "Total Adjustments"),
                    Map.entry(49, "Total Payments"),
                    Map.entry(50, "Alternate Visit ID"),
                    Map.entry(51, "Visit Indicator"),
                    Map.entry(52, "Other Healthcare Provider"))),
            Map.entry("ORC", Map.ofEntries(
                    Map.entry(1, "Order Control"),
                    Map.entry(2, "Placer Order Number"),
                    Map.entry(3, "Filler Order Number"),
                    Map.entry(4, "Placer Group Number"),
                    Map.entry(5, "Order Status"),
                    Map.entry(6, "Response Flag"),
                    Map.entry(7, "Quantity/Timing"),
                    Map.entry(8, "Parent"),
                    Map.entry(9, "Date/Time of Transaction"),
                    Map.entry(10, "Entered By"),
                    Map.entry(11, "Verified By"),
                    Map.entry(12, "Ordering Provider"),
                    Map.entry(13, "Enterer's Location"),
                    Map.entry(14, "Call Back Phone Number"),
                    Map.entry(15, "Order Effective Date/Time"),
                    Map.entry(16, "Order Control Code Reason"),
                    Map.entry(17, "Entering Organization"),
                    Map.entry(18, "Entering Device"),
                    Map.entry(19, "Action By"),
                    Map.entry(20, "Advanced Beneficiary Notice Code"),
                    Map.entry(21, "Ordering Facility Name"),
                    Map.entry(22, "Ordering Facility Address"),
                    Map.entry(23, "Ordering Facility Phone Number"),
                    Map.entry(24, "Ordering Provider Address"))),
            Map.entry("OBR", Map.ofEntries(
                    Map.entry(1, "Set ID - OBR"),
                    Map.entry(2, "Placer Order Number"),
                    Map.entry(3, "Filler Order Number"),
                    Map.entry(4, "Universal Service Identifier"),
                    Map.entry(5, "Priority"),
                    Map.entry(6, "Requested Date/Time"),
                    Map.entry(7, "Observation Date/Time"),
                    Map.entry(8, "Observation End Date/Time"),
                    Map.entry(9, "Collection Volume"),
                    Map.entry(10, "Collector Identifier"),
                    Map.entry(11, "Specimen Action Code"),
                    Map.entry(12, "Danger Code"),
                    Map.entry(13, "Relevant Clinical Information"),
                    Map.entry(14, "Specimen Received Date/Time"),
                    Map.entry(15, "Specimen Source"),
                    Map.entry(16, "Ordering Provider"),
                    Map.entry(17, "Order Callback Phone Number"),
                    Map.entry(18, "Placer Field 1"),
                    Map.entry(19, "Placer Field 2"),
                    Map.entry(20, "Filler Field 1"),
                    Map.entry(21, "Filler Field 2"),
                    Map.entry(22, "Results Report/Status Change Date/Time"),
                    Map.entry(23, "Charge to Practice"),
                    Map.entry(24, "Diagnostic Service Section ID"),
                    Map.entry(25, "Result Status"),
                    Map.entry(26, "Parent Result"),
                    Map.entry(27, "Quantity/Timing"),
                    Map.entry(28, "Result Copies To"),
                    Map.entry(29, "Parent"),
                    Map.entry(30, "Transportation Mode"),
                    Map.entry(31, "Reason for Study"),
                    Map.entry(32, "Principal Result Interpreter"),
                    Map.entry(33, "Assistant Result Interpreter"),
                    Map.entry(34, "Technician"),
                    Map.entry(35, "Transcriptionist"),
                    Map.entry(36, "Scheduled Date/Time"),
                    Map.entry(37, "Number of Sample Containers"),
                    Map.entry(38, "Transport Logistics of Collected Sample"),
                    Map.entry(39, "Collector's Comment"),
                    Map.entry(40, "Transport Arrangement Responsibility"),
                    Map.entry(41, "Transport Arranged"),
                    Map.entry(42, "Escort Required"),
                    Map.entry(43, "Planned Patient Transport Comment"),
                    Map.entry(44, "Procedure Code"),
                    Map.entry(45, "Procedure Code Modifier"),
                    Map.entry(46, "Placer Supplemental Service Information"),
                    Map.entry(47, "Filler Supplemental Service Information"))),
            Map.entry("OBX", Map.ofEntries(
                    Map.entry(1, "Set ID - OBX"),
                    Map.entry(2, "Value Type"),
                    Map.entry(3, "Observation Identifier"),
                    Map.entry(4, "Observation Sub-ID"),
                    Map.entry(5, "Observation Value"),
                    Map.entry(6, "Units"),
                    Map.entry(7, "References Range"),
                    Map.entry(8, "Abnormal Flags"),
                    Map.entry(9, "Probability"),
                    Map.entry(10, "Nature of Abnormal Test"),
                    Map.entry(11, "Observation Result Status"),
                    Map.entry(12, "Effective Date of Reference Range"),
                    Map.entry(13, "User Defined Access Checks"),
                    Map.entry(14, "Date/Time of the Observation"),
                    Map.entry(15, "Producer's ID"),
                    Map.entry(16, "Responsible Observer"),
                    Map.entry(17, "Observation Method"),
                    Map.entry(18, "Equipment Instance Identifier"),
                    Map.entry(19, "Date/Time of the Analysis"))),
            Map.entry("NTE", Map.ofEntries(
                    Map.entry(1, "Set ID - NTE"),
                    Map.entry(2, "Source of Comment"),
                    Map.entry(3, "Comment"),
                    Map.entry(4, "Comment Type"))),
            Map.entry("DG1", Map.ofEntries(
                    Map.entry(1, "Set ID - DG1"),
                    Map.entry(2, "Diagnosis Coding Method"),
                    Map.entry(3, "Diagnosis Code"),
                    Map.entry(4, "Diagnosis Description"),
                    Map.entry(5, "Diagnosis Date/Time"),
                    Map.entry(6, "Diagnosis Type"),
                    Map.entry(7, "Major Diagnostic Category"),
                    Map.entry(8, "Diagnostic Related Group"),
                    Map.entry(9, "DRG Approval Indicator"),
                    Map.entry(10, "DRG Grouper Review Code"),
                    Map.entry(11, "Outlier Type"),
                    Map.entry(12, "Outlier Days"),
                    Map.entry(13, "Outlier Cost"),
                    Map.entry(14, "Grouper Version and Type"),
                    Map.entry(15, "Diagnosis Priority"),
                    Map.entry(16, "Diagnosing Clinician"),
                    Map.entry(17, "Diagnosis Classification"),
                    Map.entry(18, "Confidential Indicator"),
                    Map.entry(19, "Attestation Date/Time"))),
            Map.entry("TXA", Map.ofEntries(
                    Map.entry(1, "Set ID - TXA"),
                    Map.entry(2, "Document Type"),
                    Map.entry(3, "Document Content Presentation"),
                    Map.entry(4, "Activity Date/Time"),
                    Map.entry(5, "Primary Activity Provider Code/Name"),
                    Map.entry(6, "Origination Date/Time"),
                    Map.entry(7, "Transcription Date/Time"),
                    Map.entry(8, "Edit Date/Time"),
                    Map.entry(9, "Originator Code/Name"),
                    Map.entry(10, "Assigned Document Authenticator"),
                    Map.entry(11, "Transcriptionist Code/Name"),
                    Map.entry(12, "Unique Document Number"),
                    Map.entry(13, "Parent Document Number"),
                    Map.entry(14, "Placer Order Number"),
                    Map.entry(15, "Filler Order Number"),
                    Map.entry(16, "Unique Document File Name"),
                    Map.entry(17, "Document Completion Status"),
                    Map.entry(18, "Document Confidentiality Status"),
                    Map.entry(19, "Document Availability Status"),
                    Map.entry(20, "Document Storage Status"),
                    Map.entry(21, "Document Change Reason"),
                    Map.entry(22, "Authentication Person, Time Stamp"),
                    Map.entry(23, "Distributed Copies"))),
            Map.entry("FT1", Map.ofEntries(
                    Map.entry(1, "Set ID - FT1"),
                    Map.entry(2, "Transaction ID"),
                    Map.entry(3, "Transaction Batch ID"),
                    Map.entry(4, "Transaction Date"),
                    Map.entry(5, "Transaction Posting Date"),
                    Map.entry(6, "Transaction Type"),
                    Map.entry(7, "Transaction Code"),
                    Map.entry(8, "Transaction Description"),
                    Map.entry(9, "Transaction Description Alternate"),
                    Map.entry(10, "Transaction Quantity"),
                    Map.entry(11, "Transaction Amount - Extended"),
                    Map.entry(12, "Transaction Amount - Unit"),
                    Map.entry(13, "Department Code"),
                    Map.entry(14, "Insurance Plan ID"),
                    Map.entry(15, "Insurance Amount"),
                    Map.entry(16, "Assigned Patient Location"),
                    Map.entry(17, "Fee Schedule"),
                    Map.entry(18, "Patient Type"),
                    Map.entry(19, "Diagnosis Code - FT1"),
                    Map.entry(20, "Performed By Code"),
                    Map.entry(21, "Ordered By Code"),
                    Map.entry(22, "Unit Cost"),
                    Map.entry(23, "Filler Order Number"),
                    Map.entry(24, "Entered By Code"),
                    Map.entry(25, "Procedure Code"),
                    Map.entry(26, "Procedure Code Modifier"))),
            Map.entry("MSA", Map.ofEntries(
                    Map.entry(1, "Acknowledgment Code"),
                    Map.entry(2, "Message Control ID"),
                    Map.entry(3, "Text Message"),
                    Map.entry(4, "Expected Sequence Number"),
                    Map.entry(5, "Delayed Acknowledgment Type"),
                    Map.entry(6, "Error Condition"))),
            Map.entry("ERR", Map.ofEntries(
                    Map.entry(1, "Error Code and Location"),
                    Map.entry(2, "Error Location"),
                    Map.entry(3, "HL7 Error Code"),
                    Map.entry(4, "Severity"),
                    Map.entry(5, "Application Error Code"),
                    Map.entry(6, "Application Error Parameter"),
                    Map.entry(7, "Diagnostic Information"),
                    Map.entry(8, "User Message"),
                    Map.entry(9, "Inform Person Indicator"),
                    Map.entry(10, "Override Type"),
                    Map.entry(11, "Override Reason Code"),
                    Map.entry(12, "Help Desk Contact Point")))
    );
    private static final Map<String, Set<Integer>> REQUIRED_FIELDS = Map.of(
            "MSH", Set.of(1, 2, 7, 9, 10, 11, 12),
            "PID", Set.of(3, 5),
            "PV1", Set.of(2),
            "ORC", Set.of(1),
            "OBR", Set.of(4),
            "OBX", Set.of(2, 3, 5, 11)
    );
    private final HapiContext hapiContext = new DefaultHapiContext();

    public Hl7ParseResult parseAndValidate(Hl7Request request) {
        String normalized = normalize(request.message());
        Delimiters delimiters = Delimiters.from(normalized);
        List<ValidationIssue> issues = new ArrayList<>();
        List<Hl7Segment> segments = parseSegments(normalized, delimiters, issues);
        MessageMetadata metadata = metadata(segments);
        validateWithHapi(normalized, issues);
        validateStructure(segments, metadata, request.mode(), issues);
        ValidationSummary summary = summary(issues);
        return new Hl7ParseResult(metadata, segments, issues, summary, normalized, Instant.now());
    }

    private List<Hl7Segment> parseSegments(String message, Delimiters delimiters, List<ValidationIssue> issues) {
        List<Hl7Segment> segments = new ArrayList<>();
        String[] rows = message.split("\\r");
        for (int i = 0; i < rows.length; i++) {
            String row = rows[i];
            if (row.isBlank()) {
                continue;
            }
            String[] pieces = row.split("\\" + delimiters.field(), -1);
            String name = pieces[0].trim().toUpperCase(Locale.ROOT);
            if (name.length() != 3) {
                issues.add(issue(ValidationSeverity.ERROR, name, i + 1, null, null, "SEG-" + (i + 1),
                        "Segment ID must be exactly three characters.", "Check segment delimiters and remove stray line breaks."));
            }
            List<Hl7Field> fields = new ArrayList<>();
            if ("MSH".equals(name)) {
                fields.add(field(name, 1, String.valueOf(delimiters.field()), delimiters));
                for (int f = 1; f < pieces.length; f++) {
                    fields.add(field(name, f + 1, pieces[f], delimiters));
                }
            } else {
                for (int f = 1; f < pieces.length; f++) {
                    fields.add(field(name, f, pieces[f], delimiters));
                }
            }
            segments.add(new Hl7Segment(i + 1, name, SEGMENT_DESCRIPTIONS.getOrDefault(name, "Custom or unsupported segment"),
                    name.startsWith("Z"), fields));
        }
        return segments;
    }

    private Hl7Field field(String segment, int index, String value, Delimiters delimiters) {
        List<List<Hl7Component>> repetitions = new ArrayList<>();
        String[] reps = value.split("\\" + delimiters.repetition(), -1);
        for (String rep : reps) {
            String[] components = rep.split("\\" + delimiters.component(), -1);
            List<Hl7Component> parsedComponents = new ArrayList<>();
            for (int c = 0; c < components.length; c++) {
                parsedComponents.add(new Hl7Component(c + 1, "Component " + (c + 1), components[c]));
            }
            repetitions.add(parsedComponents);
        }
        return new Hl7Field(index, fieldName(segment, index), value, REQUIRED_FIELDS.getOrDefault(segment, Set.of()).contains(index),
                reps.length > 1, datatype(segment, index), repetitions);
    }

    private MessageMetadata metadata(List<Hl7Segment> segments) {
        Hl7Segment msh = segments.stream().filter(segment -> "MSH".equals(segment.name())).findFirst().orElse(null);
        if (msh == null) {
            return new MessageMetadata(null, null, null, null, null, null, null, null, null, null);
        }
        String messageType = value(msh, 9);
        String[] messageParts = messageType == null ? new String[0] : messageType.split("\\^", -1);
        return new MessageMetadata(
                value(msh, 12),
                messageParts.length > 0 ? messageParts[0] : null,
                messageParts.length > 1 ? messageParts[1] : null,
                value(msh, 3),
                value(msh, 4),
                value(msh, 5),
                value(msh, 6),
                value(msh, 7),
                value(msh, 10),
                value(msh, 11)
        );
    }

    private void validateWithHapi(String message, List<ValidationIssue> issues) {
        try {
            GenericParser parser = hapiContext.getGenericParser();
            Message parsed = parser.parse(message);
            parsed.encode();
        } catch (HL7Exception ex) {
            issues.add(issue(ValidationSeverity.WARNING, "MSH", null, null, null, "HAPI",
                    "HAPI parser reported a structural issue: " + ex.getMessage(),
                    "Review delimiters, HL7 version, message type, and required segment groups."));
        }
    }

    private void validateStructure(List<Hl7Segment> segments, MessageMetadata metadata, ValidationMode mode, List<ValidationIssue> issues) {
        if (segments.isEmpty()) {
            issues.add(issue(ValidationSeverity.ERROR, null, null, null, null, "MESSAGE",
                    "Message does not contain any HL7 segments.", "Paste a complete HL7 message beginning with MSH."));
            return;
        }
        if (!"MSH".equals(segments.getFirst().name())) {
            issues.add(issue(ValidationSeverity.ERROR, segments.getFirst().name(), 1, null, null, "SEG-1",
                    "The first segment must be MSH.", "Move MSH to the beginning of the message."));
        }
        if (metadata.hl7Version() == null || metadata.hl7Version().isBlank()) {
            issues.add(issue(ValidationSeverity.ERROR, "MSH", 1, 12, null, "MSH-12",
                    "Missing HL7 version.", "Set MSH-12 to a supported version such as 2.5.1."));
        } else if (!isSupportedVersion(metadata.hl7Version())) {
            issues.add(issue(severityFor(mode), "MSH", 1, 12, null, "MSH-12",
                    "Unsupported HL7 version " + metadata.hl7Version() + ".", "Use HL7 v2.3 or later, or add a custom version profile."));
        }
        Set<String> presentSegments = new HashSet<>();
        for (Hl7Segment segment : segments) {
            presentSegments.add(segment.name());
            validateSegment(segment, mode, issues);
        }
        for (String required : REQUIRED_SEGMENTS.getOrDefault(nullSafe(metadata.messageType()), List.of("MSH"))) {
            if (!presentSegments.contains(required)) {
                issues.add(issue(ValidationSeverity.ERROR, required, null, null, null, required,
                        "Missing required " + required + " segment for " + nullSafe(metadata.messageType()) + " messages.",
                        "Add the " + required + " segment in the expected HL7 order."));
            }
        }
    }

    private void validateSegment(Hl7Segment segment, ValidationMode mode, List<ValidationIssue> issues) {
        if (!KNOWN_SEGMENTS.contains(segment.name()) && !segment.custom()) {
            issues.add(issue(severityFor(mode), segment.name(), segment.index(), null, null, "SEG-" + segment.index(),
                    "Unknown segment " + segment.name() + ".", "Confirm the segment name or define a vendor-specific profile."));
        }
        Map<Integer, Hl7Field> fields = new HashMap<>();
        for (Hl7Field field : segment.fields()) {
            fields.put(field.index(), field);
            if (field.required() && field.value().isBlank()) {
                issues.add(issue(ValidationSeverity.ERROR, segment.name(), segment.index(), field.index(), null,
                        segment.name() + "-" + field.index(), "Missing required field " + field.name() + ".",
                        "Populate " + segment.name() + "-" + field.index() + " with a valid value."));
            }
            if (field.value().contains(String.valueOf('\n'))) {
                issues.add(issue(ValidationSeverity.ERROR, segment.name(), segment.index(), field.index(), null,
                        segment.name() + "-" + field.index(), "Field contains an LF character.",
                        "Use carriage return segment separators and remove embedded line feeds."));
            }
        }
        for (Integer required : REQUIRED_FIELDS.getOrDefault(segment.name(), Set.of())) {
            if (!fields.containsKey(required)) {
                issues.add(issue(ValidationSeverity.ERROR, segment.name(), segment.index(), required, null,
                        segment.name() + "-" + required, "Missing required field " + fieldName(segment.name(), required) + ".",
                        "Add " + segment.name() + "-" + required + " to the segment."));
            }
        }
        validateTimestamp(segment, 7, fields.get(7), issues);
    }

    private void validateTimestamp(Hl7Segment segment, int fieldIndex, Hl7Field field, List<ValidationIssue> issues) {
        if (field == null || field.value().isBlank() || !"MSH".equals(segment.name())) {
            return;
        }
        String value = field.value();
        if (!value.matches("\\d{4,14}([+-]\\d{4})?")) {
            issues.add(issue(ValidationSeverity.WARNING, segment.name(), segment.index(), fieldIndex, null,
                    segment.name() + "-" + fieldIndex, "Timestamp is not in a recognized HL7 TS format.",
                    "Use YYYYMMDDHHMMSS with an optional timezone offset."));
            return;
        }
        try {
            String padded = (value.length() >= 14 ? value.substring(0, 14) : String.format("%-14s", value).replace(' ', '0'));
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").parse(padded);
        } catch (DateTimeParseException ex) {
            issues.add(issue(ValidationSeverity.WARNING, segment.name(), segment.index(), fieldIndex, null,
                    segment.name() + "-" + fieldIndex, "Timestamp contains invalid calendar values.",
                    "Check month, day, hour, minute, and second values."));
        }
    }

    private ValidationSummary summary(List<ValidationIssue> issues) {
        int errors = 0;
        int warnings = 0;
        int info = 0;
        for (ValidationIssue issue : issues) {
            if (issue.severity() == ValidationSeverity.ERROR) {
                errors++;
            } else if (issue.severity() == ValidationSeverity.WARNING) {
                warnings++;
            } else {
                info++;
            }
        }
        return new ValidationSummary(errors, warnings, info, errors == 0);
    }

    private String normalize(String message) {
        return message.replace("\r\n", "\r").replace('\n', '\r').trim();
    }

    private ValidationIssue issue(ValidationSeverity severity, String segment, Integer segmentIndex, Integer fieldIndex,
                                  Integer componentIndex, String location, String description, String suggestedFix) {
        return new ValidationIssue(severity, segment, segmentIndex, fieldIndex, componentIndex, location, description, suggestedFix);
    }

    private ValidationSeverity severityFor(ValidationMode mode) {
        return mode == ValidationMode.LENIENT ? ValidationSeverity.INFO : mode == ValidationMode.STANDARD ? ValidationSeverity.WARNING : ValidationSeverity.ERROR;
    }

    private boolean isSupportedVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            return false;
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            Integer.parseInt(parts.length == 3 ? parts[2] : "0");
            return major == 2 && minor >= 3;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String fieldName(String segment, int index) {
        return FIELD_NAMES.getOrDefault(segment, Map.of()).getOrDefault(index, "Field " + index);
    }

    private String datatype(String segment, int index) {
        if ("MSH".equals(segment) && (index == 7)) {
            return "TS";
        }
        if (Set.of(3, 4, 5, 9).contains(index)) {
            return "CWE/CE/XPN composite";
        }
        return "ST";
    }

    private String value(Hl7Segment segment, int fieldIndex) {
        return segment.fields().stream().filter(field -> field.index() == fieldIndex).findFirst().map(Hl7Field::value).orElse(null);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private record Delimiters(char field, char component, char repetition, char escape, char subcomponent) {
        static Delimiters from(String message) {
            char field = message.length() > 3 ? message.charAt(3) : '|';
            String encoding = message.length() > 8 ? message.substring(4, 8) : "^~\\&";
            return new Delimiters(field, encoding.charAt(0), encoding.charAt(1), encoding.charAt(2), encoding.charAt(3));
        }
    }
}
