package com.hl7decoder.service;

import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.Hl7ParseResult;
import com.hl7decoder.model.ValidationIssue;
import com.hl7decoder.model.ValidationMode;
import com.hl7decoder.model.platform.Hl7RepairResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class Hl7RepairService {
    private static final DateTimeFormatter HL7_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
    private final Hl7Service hl7Service;

    public Hl7RepairService(Hl7Service hl7Service) {
        this.hl7Service = hl7Service;
    }

    public Hl7RepairResponse repair(Hl7Request request) {
        String original = request.message() == null ? "" : request.message();
        String repaired = original.replace("\r\n", "\r").replace('\n', '\r').trim();
        List<String> repairs = new ArrayList<>();
        if (!repaired.equals(original.trim())) {
            repairs.add("Normalized segment separators to HL7 carriage returns.");
        }

        if (!repaired.startsWith("MSH|")) {
            repaired = defaultMsh() + (repaired.isBlank() ? "" : "\r" + repaired);
            repairs.add("Inserted a default MSH segment because the message did not start with MSH.");
        }

        repaired = repairMsh(repaired, repairs);
        repaired = insertMissingSegments(repaired, request.mode(), repairs);
        repaired = repaired.replaceAll("\\r{2,}", "\r");

        return new Hl7RepairResponse(original, repaired, !repairs.isEmpty(), repairs);
    }

    private String repairMsh(String message, List<String> repairs) {
        String[] rows = message.split("\\r", -1);
        String[] fields = rows[0].split("\\|", -1);
        List<String> fixed = new ArrayList<>(List.of(fields));
        while (fixed.size() <= 12) {
            fixed.add("");
        }
        if (fixed.get(1).isBlank()) {
            fixed.set(1, "^~\\&");
            repairs.add("Populated MSH-2 encoding characters.");
        }
        if (fixed.get(7).isBlank() || !fixed.get(7).matches("\\d{4,14}([+-]\\d{4})?")) {
            fixed.set(7, HL7_TS.format(Instant.now()));
            repairs.add("Normalized MSH-7 timestamp.");
        }
        if (fixed.get(9).isBlank()) {
            fixed.set(9, "ADT^A01");
            repairs.add("Defaulted MSH-9 message type to ADT^A01.");
        }
        if (fixed.get(10).isBlank()) {
            fixed.set(10, "MSG" + Instant.now().toEpochMilli());
            repairs.add("Populated MSH-10 message control ID.");
        }
        if (fixed.get(11).isBlank()) {
            fixed.set(11, "P");
            repairs.add("Populated MSH-11 processing ID.");
        }
        if (fixed.get(12).isBlank()) {
            fixed.set(12, "2.5.1");
            repairs.add("Populated MSH-12 HL7 version.");
        }
        rows[0] = String.join("|", fixed);
        return String.join("\r", rows);
    }

    private String insertMissingSegments(String message, ValidationMode mode, List<String> repairs) {
        Hl7ParseResult result = hl7Service.parseAndValidate(new Hl7Request(message, mode == null ? ValidationMode.STANDARD : mode));
        Set<String> missing = new LinkedHashSet<>();
        for (ValidationIssue issue : result.issues()) {
            if (issue.description().startsWith("Missing required ") && issue.segment() != null) {
                missing.add(issue.segment());
            }
        }
        if (missing.isEmpty()) {
            return message;
        }
        List<String> rows = new ArrayList<>(List.of(message.split("\\r", -1)));
        for (String segment : missing) {
            rows.add(defaultSegment(segment));
            repairs.add("Inserted placeholder " + segment + " segment.");
        }
        return String.join("\r", rows);
    }

    private String defaultMsh() {
        return "MSH|^~\\&|HEALTHCAREHERO|LOCAL|EHR|LOCAL|" + HL7_TS.format(Instant.now()) + "||ADT^A01|MSG" + Instant.now().toEpochMilli() + "|P|2.5.1";
    }

    private String defaultSegment(String segment) {
        return switch (segment) {
            case "EVN" -> "EVN|A01|" + HL7_TS.format(Instant.now());
            case "PID" -> "PID|1||SYNTHETIC^^^HH^MR||DOE^JANE";
            case "PV1" -> "PV1|1|O";
            case "ORC" -> "ORC|NW";
            case "OBR" -> "OBR|1|||TEST^Test Order";
            case "OBX" -> "OBX|1|ST|NOTE^Note||Placeholder result||||||F";
            case "SCH" -> "SCH|1";
            case "FT1" -> "FT1|1";
            default -> segment + "|1";
        };
    }
}
