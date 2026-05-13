package com.hl7decoder.service;

import com.hl7decoder.api.dto.cpt.IcdCptCompatibilityRequest;
import com.hl7decoder.model.cpt.IcdCptMatchResult;
import com.hl7decoder.model.cpt.ModifierSuggestion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class IcdCptCompatibilityService {
    private static final Map<String, List<String>> RULES = Map.of(
            "71046", List.of("R05", "R06", "J", "I50"),
            "71045", List.of("R05", "R06", "J", "I50"),
            "73562", List.of("M25.56", "S83", "M17", "M23"),
            "73564", List.of("M25.56", "S83", "M17", "M23"),
            "93000", List.of("R00", "R07", "I", "Z01.81"),
            "83036", List.of("E08", "E09", "E10", "E11", "R73"),
            "80053", List.of("E", "I", "K", "N", "R"),
            "99213", List.of("A", "B", "C", "D", "E", "F", "G", "I", "J", "K", "M", "N", "R", "S", "Z")
    );

    private final CptSearchService cptSearchService;

    public IcdCptCompatibilityService(CptSearchService cptSearchService) {
        this.cptSearchService = cptSearchService;
    }

    public IcdCptMatchResult check(IcdCptCompatibilityRequest request) {
        String cpt = normalize(request.procedureCode());
        String icd = normalize(request.icd10Code());
        String diagnosis = text(request.diagnosisText());
        String procedure = text(request.procedureText());
        List<String> warnings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        List<ModifierSuggestion> modifiers = new ArrayList<>();

        if (cpt.isBlank() && !procedure.isBlank()) {
            var search = cptSearchService.search(new com.hl7decoder.api.dto.cpt.ProcedureSearchRequest(procedure, 1, null));
            if (!search.results().isEmpty()) {
                cpt = search.results().getFirst().code();
                recommendations.add("Procedure text matched " + cpt + " as the top CPT/HCPCS candidate.");
            }
        }
        if (icd.isBlank() && !diagnosis.isBlank()) {
            icd = inferIcd(diagnosis);
            recommendations.add("Diagnosis text inferred as " + icd + ". Verify against ICD-10-CM coding guidelines.");
        }

        List<String> allowed = RULES.getOrDefault(cpt, List.of());
        boolean supported = allowed.stream().anyMatch(icd::startsWith);
        String status;
        double confidence;
        String reason;

        if (cpt.isBlank() || icd.isBlank()) {
            status = "UNKNOWN_RULE_NOT_FOUND";
            confidence = 0.25;
            reason = "Missing diagnosis or procedure code.";
            warnings.add("Add both ICD-10 and CPT/HCPCS codes before claim readiness review.");
        } else if (supported) {
            status = "SUPPORTED";
            confidence = 0.86;
            reason = "Diagnosis and procedure appear clinically related.";
        } else if (allowed.isEmpty()) {
            status = "UNKNOWN_RULE_NOT_FOUND";
            confidence = 0.45;
            reason = "No local rule is configured for this procedure code.";
            warnings.add("Check payer policy for medical necessity.");
        } else if (needsMoreSpecificDiagnosis(icd)) {
            status = "NEEDS_MORE_SPECIFIC_DIAGNOSIS";
            confidence = 0.62;
            reason = "The diagnosis may be too broad for this procedure.";
            warnings.add("This CPT may require a more specific ICD-10 diagnosis or additional documentation.");
        } else {
            status = "LIKELY_DENIAL";
            confidence = 0.72;
            reason = "The diagnosis does not match configured procedure support prefixes.";
            warnings.add("Likely denial risk based on local ICD/CPT compatibility rules.");
        }

        if (procedure.contains("office") || cpt.startsWith("992")) {
            modifiers.add(new ModifierSuggestion("25", "Consider when a significant, separately identifiable E/M service occurs on the same day as another procedure.", false));
        }
        if (request.payer() != null && !request.payer().isBlank()) {
            recommendations.add("Confirm payer-specific LCD/NCD and modifier rules for " + request.payer().trim() + ".");
        }
        recommendations.add("Confirm documentation supports diagnosis specificity, procedure indication, and medical necessity.");

        return new IcdCptMatchResult(request.diagnosisText(), icd, request.procedureText(), cpt, request.payer(), status, confidence, reason, warnings, recommendations, modifiers);
    }

    private boolean needsMoreSpecificDiagnosis(String icd) {
        return icd.length() <= 3 || List.of("R05", "R07", "M25").contains(icd);
    }

    private String inferIcd(String diagnosis) {
        if (diagnosis.contains("cough")) {
            return "R05.9";
        }
        if (diagnosis.contains("knee")) {
            return "M25.569";
        }
        if (diagnosis.contains("diabetes")) {
            return "E11.9";
        }
        if (diagnosis.contains("chest pain")) {
            return "R07.9";
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9.]", "").toUpperCase(Locale.ROOT);
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
