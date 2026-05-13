package com.hl7decoder.service;

import com.hl7decoder.model.platform.X12DecodeResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class X12Service {
    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry("ISA", "Interchange Control Header"),
            Map.entry("GS", "Functional Group Header"),
            Map.entry("ST", "Transaction Set Header"),
            Map.entry("BHT", "Beginning of Hierarchical Transaction"),
            Map.entry("HL", "Hierarchical Level"),
            Map.entry("NM1", "Individual or Organization Name"),
            Map.entry("CLM", "Claim Information"),
            Map.entry("HI", "Healthcare Diagnosis Code"),
            Map.entry("SV1", "Professional Service Line"),
            Map.entry("SVC", "Service Payment Information"),
            Map.entry("CAS", "Claim Adjustment"),
            Map.entry("DMG", "Demographic Information"),
            Map.entry("DTP", "Date or Time or Period"),
            Map.entry("SE", "Transaction Set Trailer"),
            Map.entry("GE", "Functional Group Trailer"),
            Map.entry("IEA", "Interchange Control Trailer")
    );

    public X12DecodeResponse decode(String x12) {
        String normalized = x12 == null ? "" : x12.replace("\n", "").replace("\r", "").trim();
        List<X12DecodeResponse.X12Segment> segments = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        String transactionType = "Unknown";
        String currentLoop = "Interchange";

        String[] rawSegments = normalized.split("~");
        for (int index = 0; index < rawSegments.length; index++) {
            String raw = rawSegments[index].trim();
            if (raw.isBlank()) {
                continue;
            }
            String[] elements = raw.split("\\*", -1);
            String segmentId = elements[0].trim();
            if ("ST".equals(segmentId) && elements.length > 1) {
                transactionType = transactionName(elements[1]);
            }
            currentLoop = loop(segmentId, elements, currentLoop);
            if (!DESCRIPTIONS.containsKey(segmentId) && !segmentId.startsWith("N")) {
                issues.add("Unrecognized segment " + segmentId + " at position " + (index + 1) + ".");
            }
            segments.add(new X12DecodeResponse.X12Segment(
                    index + 1,
                    segmentId,
                    DESCRIPTIONS.getOrDefault(segmentId, "X12 segment"),
                    currentLoop,
                    List.of(elements).subList(1, elements.length)
            ));
        }

        List<String> ids = segments.stream().map(X12DecodeResponse.X12Segment::segmentId).toList();
        if (!ids.contains("ISA")) {
            issues.add("Missing ISA interchange header.");
        }
        if (!ids.contains("ST")) {
            issues.add("Missing ST transaction set header.");
        }
        if (!ids.contains("SE")) {
            issues.add("Missing SE transaction set trailer.");
        }
        if ("Unknown".equals(transactionType)) {
            issues.add("Transaction type could not be determined from ST-01.");
        }

        return new X12DecodeResponse(transactionType, segments, issues);
    }

    private String transactionName(String code) {
        return switch (code) {
            case "837" -> "837 Healthcare Claim";
            case "835" -> "835 Healthcare Claim Payment/Advice";
            case "270" -> "270 Eligibility Inquiry";
            case "271" -> "271 Eligibility Response";
            case "276" -> "276 Claim Status Request";
            case "277" -> "277 Claim Status Response";
            default -> "Unknown";
        };
    }

    private String loop(String segmentId, String[] elements, String currentLoop) {
        if ("ISA".equals(segmentId) || "GS".equals(segmentId) || "ST".equals(segmentId)) {
            return "Envelope";
        }
        if ("HL".equals(segmentId) && elements.length > 3) {
            return switch (elements[3]) {
                case "20" -> "2000A Billing Provider";
                case "22" -> "2000B Subscriber";
                case "23" -> "2000C Patient";
                default -> "Hierarchy " + elements[3];
            };
        }
        if ("CLM".equals(segmentId)) {
            return "2300 Claim";
        }
        if ("SV1".equals(segmentId) || "SVC".equals(segmentId)) {
            return "2400 Service Line";
        }
        if ("HI".equals(segmentId)) {
            return "2300 Diagnosis";
        }
        return currentLoop;
    }
}
