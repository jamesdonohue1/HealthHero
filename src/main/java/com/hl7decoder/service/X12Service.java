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
            Map.entry("EB", "Eligibility or Benefit Information"),
            Map.entry("EQ", "Eligibility or Benefit Inquiry"),
            Map.entry("TRN", "Trace"),
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
        validateRequiredLoops(transactionType, ids, issues);

        return new X12DecodeResponse(transactionType, segments, issues);
    }

    public Map<String, Object> revenueCycle(String x12) {
        X12DecodeResponse decoded = decode(x12);
        List<String> ids = decoded.segments().stream().map(X12DecodeResponse.X12Segment::segmentId).toList();
        List<String> denials = denialReasons(decoded);
        return Map.of(
                "transactionType", decoded.transactionType(),
                "profile", claimProfile(decoded),
                "requiredLoopIssues", decoded.issues(),
                "eligibility", eligibilitySummary(decoded),
                "payment", paymentSummary(decoded),
                "denialReasons", denials,
                "claimReadinessScore", claimReadiness(decoded, denials),
                "claimReadinessFactors", List.of(
                        ids.contains("HI") ? "Diagnosis present" : "Missing diagnosis loop",
                        ids.contains("SV1") || ids.contains("SVC") ? "Service line present" : "Missing service line",
                        denials.isEmpty() ? "No denial adjustment reason detected" : "Denial/payment adjustment needs review"
                )
        );
    }

    public Map<String, Object> generate270(String memberId) {
        String member = memberId == null || memberId.isBlank() ? "SYN000001" : memberId.trim();
        String inquiry = "ISA*00*          *00*          *ZZ*HEALTHHERO    *ZZ*PAYER          *260101*1230*^*00501*000000001*0*T*:~"
                + "GS*HS*HEALTHHERO*PAYER*20260101*1230*1*X*005010X279A1~ST*270*0001*005010X279A1~"
                + "BHT*0022*13*ELIG" + member + "*20260101*1230~HL*1**20*1~NM1*PR*2*PAYER*****PI*PAYER~"
                + "HL*2*1*21*1~NM1*1P*2*HEALTHCARE HERO*****XX*1234567893~HL*3*2*22*0~TRN*1*ELIG" + member + "~"
                + "NM1*IL*1*DOE*JANE****MI*" + member + "~EQ*30~SE*10*0001~GE*1*1~IEA*1*000000001~";
        return Map.of("memberId", member, "transactionType", "270 Eligibility Inquiry", "x12", inquiry);
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

    private void validateRequiredLoops(String transactionType, List<String> ids, List<String> issues) {
        if (transactionType.startsWith("837")) {
            require(ids, issues, "BHT", "837 claim is missing BHT beginning segment.");
            require(ids, issues, "NM1", "837 claim is missing NM1 provider/subscriber loops.");
            require(ids, issues, "CLM", "837 claim is missing CLM claim information.");
            require(ids, issues, "HI", "837 claim is missing HI diagnosis information.");
            require(ids, issues, "SV1", "837 professional claim is missing SV1 service line.");
        } else if (transactionType.startsWith("835")) {
            require(ids, issues, "SVC", "835 payment is missing SVC service payment loop.");
            require(ids, issues, "CAS", "835 payment is missing CAS adjustment details.");
        } else if (transactionType.startsWith("270") || transactionType.startsWith("271")) {
            require(ids, issues, "NM1", transactionType + " is missing NM1 subscriber/dependent identity.");
            if (transactionType.startsWith("271")) {
                require(ids, issues, "EB", "271 eligibility response is missing EB benefit information.");
            }
        }
    }

    private void require(List<String> ids, List<String> issues, String segment, String message) {
        if (!ids.contains(segment)) {
            issues.add(message);
        }
    }

    private String claimProfile(X12DecodeResponse decoded) {
        String text = decoded.segments().stream().map(segment -> String.join("*", segment.elements())).reduce("", (a, b) -> a + " " + b).toLowerCase();
        if (text.contains("005010x223")) {
            return "837 Institutional";
        }
        if (text.contains("005010x224")) {
            return "837 Dental";
        }
        if (decoded.transactionType().startsWith("837")) {
            return "837 Professional";
        }
        return decoded.transactionType();
    }

    private Map<String, Object> eligibilitySummary(X12DecodeResponse decoded) {
        List<String> benefits = decoded.segments().stream()
                .filter(segment -> "EB".equals(segment.segmentId()))
                .map(segment -> String.join(" | ", segment.elements()))
                .toList();
        return Map.of(
                "coverageStatus", benefits.stream().anyMatch(value -> value.startsWith("1")) ? "ACTIVE" : "UNKNOWN",
                "benefits", benefits,
                "copayCoinsuranceDeductible", decoded.segments().stream()
                        .filter(segment -> "EB".equals(segment.segmentId()))
                        .map(segment -> Map.of("raw", String.join("*", segment.elements())))
                        .toList()
        );
    }

    private Map<String, Object> paymentSummary(X12DecodeResponse decoded) {
        List<String> payments = decoded.segments().stream()
                .filter(segment -> "SVC".equals(segment.segmentId()))
                .map(segment -> String.join(" | ", segment.elements()))
                .toList();
        return Map.of("servicePayments", payments, "paymentLineCount", payments.size());
    }

    private List<String> denialReasons(X12DecodeResponse decoded) {
        return decoded.segments().stream()
                .filter(segment -> "CAS".equals(segment.segmentId()))
                .map(segment -> "Adjustment: " + String.join(" | ", segment.elements()))
                .toList();
    }

    private int claimReadiness(X12DecodeResponse decoded, List<String> denialReasons) {
        int score = 100 - decoded.issues().size() * 8 - denialReasons.size() * 10;
        return Math.max(0, Math.min(100, score));
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
