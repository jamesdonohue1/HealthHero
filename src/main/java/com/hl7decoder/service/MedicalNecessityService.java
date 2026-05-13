package com.hl7decoder.service;

import com.hl7decoder.api.dto.platform.MedicalNecessityRequest;
import com.hl7decoder.model.platform.MedicalNecessityResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MedicalNecessityService {
    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put("83036", List.of("E08", "E09", "E10", "E11", "R73"));
        RULES.put("80053", List.of("E", "I", "K", "N", "R"));
        RULES.put("71046", List.of("J", "R05", "R06", "I50"));
        RULES.put("93000", List.of("I", "R00", "R07", "Z01.81"));
        RULES.put("72148", List.of("M48", "M51", "M54", "S33"));
        RULES.put("992", List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "R", "S", "Z"));
    }

    public MedicalNecessityResponse check(MedicalNecessityRequest request) {
        String cpt = normalize(request.cptCode());
        List<String> diagnoses = request.icd10Codes() == null ? List.of() : request.icd10Codes().stream()
                .filter(code -> code != null && !code.isBlank())
                .map(this::normalize)
                .toList();
        List<String> allowed = allowedPrefixes(cpt);
        List<String> matchedRules = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        boolean covered = false;
        for (String diagnosis : diagnoses) {
            for (String prefix : allowed) {
                if (diagnosis.startsWith(prefix)) {
                    covered = true;
                    matchedRules.add("CPT " + cpt + " matched ICD-10 prefix " + prefix + " using diagnosis " + diagnosis + ".");
                }
            }
        }

        if (diagnoses.isEmpty()) {
            recommendations.add("Add at least one ICD-10-CM diagnosis code before claim submission.");
        }
        if (allowed.isEmpty()) {
            recommendations.add("No local coverage rule is configured for CPT " + cpt + "; review payer LCD/NCD policy before billing.");
        } else if (!covered) {
            recommendations.add("Current diagnoses do not match configured medical necessity prefixes: " + String.join(", ", allowed) + ".");
            recommendations.add("Check documentation for a more specific diagnosis or payer-required modifier/ABN workflow.");
        } else {
            recommendations.add("Configured diagnosis/procedure rule matched. Keep supporting clinical documentation with the claim.");
        }
        if (request.payer() != null && !request.payer().isBlank()) {
            recommendations.add("Confirm payer-specific edits for " + request.payer().trim() + ".");
        }

        String risk = covered ? "LOW" : (allowed.isEmpty() || diagnoses.isEmpty() ? "MEDIUM" : "HIGH");
        return new MedicalNecessityResponse(cpt, diagnoses, covered, risk, matchedRules, recommendations);
    }

    private List<String> allowedPrefixes(String cpt) {
        if (RULES.containsKey(cpt)) {
            return RULES.get(cpt);
        }
        return RULES.entrySet().stream()
                .filter(entry -> cpt.startsWith(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(List.of());
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9.]", "").toUpperCase(Locale.ROOT);
    }
}
