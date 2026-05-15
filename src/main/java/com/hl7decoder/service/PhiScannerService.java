package com.hl7decoder.service;

import com.hl7decoder.model.compliance.PhiFinding;
import com.hl7decoder.model.compliance.PhiScanResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PhiScannerService {
    private static final String POLICY = "PHI-safe logging policy: do not log raw HL7, clinical notes, diagnosis text, patient identifiers, tokens, or unredacted exports. Log event metadata, counts, IDs, and redaction status only.";
    private static final List<Rule> RULES = List.of(
            new Rule("EMAIL", Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", Pattern.CASE_INSENSITIVE)),
            new Rule("PHONE", Pattern.compile("\\b(?:\\+?1[-.\\s]?)?(?:\\(?\\d{3}\\)?[-.\\s]?)\\d{3}[-.\\s]?\\d{4}\\b")),
            new Rule("SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b")),
            new Rule("MRN", Pattern.compile("\\b(?:MRN|MEDICAL\\s+RECORD|PATIENT\\s+ID)[:#\\s-]*[A-Z0-9-]{4,}\\b", Pattern.CASE_INSENSITIVE)),
            new Rule("HL7_PID", Pattern.compile("(?m)^PID\\|[^\\r\\n]*")),
            new Rule("HL7_NK1", Pattern.compile("(?m)^NK1\\|[^\\r\\n]*")),
            new Rule("CLINICAL_NOTE", Pattern.compile("\\b(?:diagnosis|assessment|impression|history of present illness|chief complaint|discharge summary)\\b", Pattern.CASE_INSENSITIVE))
    );

    public PhiScanResponse scan(String text) {
        String value = text == null ? "" : text;
        List<PhiFinding> findings = new ArrayList<>();
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(value);
            while (matcher.find()) {
                findings.add(new PhiFinding(rule.type(), matcher.start(), matcher.end(), preview(value, matcher.start(), matcher.end())));
                if (findings.size() >= 100) {
                    break;
                }
            }
        }
        findings.sort(Comparator.comparingInt(PhiFinding::start));
        return new PhiScanResponse(!findings.isEmpty(), findings.size(), findings, redact(value, findings), POLICY);
    }

    public String redact(String text) {
        return scan(text).redactedText();
    }

    public String policy() {
        return POLICY;
    }

    private String redact(String value, List<PhiFinding> findings) {
        if (findings.isEmpty()) {
            return value;
        }
        StringBuilder redacted = new StringBuilder(value);
        List<PhiFinding> reversed = findings.stream()
                .sorted(Comparator.comparingInt(PhiFinding::start).reversed())
                .toList();
        for (PhiFinding finding : reversed) {
            redacted.replace(finding.start(), finding.end(), "[" + finding.type() + "_REDACTED]");
        }
        return redacted.toString();
    }

    private String preview(String value, int start, int end) {
        String match = value.substring(start, Math.min(end, value.length())).replaceAll("[\\r\\n]+", " ");
        if (match.length() <= 32) {
            return match;
        }
        return match.substring(0, 29) + "...";
    }

    private record Rule(String type, Pattern pattern) {
    }
}
