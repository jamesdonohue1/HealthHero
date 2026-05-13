package com.hl7decoder.service;

import com.hl7decoder.api.dto.cpt.ProcedureSearchRequest;
import com.hl7decoder.model.cpt.ProcedureCode;
import com.hl7decoder.model.cpt.ProcedureSearchResponse;
import com.hl7decoder.model.cpt.ProcedureSearchResult;
import com.hl7decoder.persistence.ProcedureCodeEntity;
import com.hl7decoder.persistence.ProcedureCodeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class CptSearchService {
    public static final String LICENSING_NOTICE = "CPT descriptions are copyrighted by the AMA. Healthcare Hero ships only a small sample/index-ready dataset; production deployments require an authorized CPT/HCPCS data source.";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;
    private static final Pattern RANGE = Pattern.compile("\\d{5}\\s*-\\s*\\d{5}");
    private static final List<ProcedureCode> BUILT_IN_CODES = List.of(
            new ProcedureCode("71046", "CPT", "Radiologic exam chest, 2 views", "Radiologic examination, chest; 2 views", "Radiology", LocalDate.of(2024, 1, 1), null, true, "Healthcare Hero sample index", List.of("chest xray", "chest x-ray", "chest radiograph", "cxr 2 views")),
            new ProcedureCode("71045", "CPT", "Radiologic exam chest, single view", "Radiologic examination, chest; single view", "Radiology", LocalDate.of(2024, 1, 1), null, true, "Healthcare Hero sample index", List.of("portable chest xray", "single view chest")),
            new ProcedureCode("73562", "CPT", "Radiologic exam knee, 3 views", "Radiologic examination, knee; 3 views", "Radiology", LocalDate.of(2024, 1, 1), null, true, "Healthcare Hero sample index", List.of("knee xray", "knee x-ray", "knee radiograph")),
            new ProcedureCode("73564", "CPT", "Radiologic exam knee, 4 or more views", "Radiologic examination, knee; 4 or more views", "Radiology", LocalDate.of(2024, 1, 1), null, true, "Healthcare Hero sample index", List.of("knee xray 4 views", "bilateral knee xray")),
            new ProcedureCode("93000", "CPT", "Electrocardiogram complete", "Routine ECG with at least 12 leads; with interpretation and report", "Cardiology", LocalDate.of(2024, 1, 1), null, true, "Healthcare Hero sample index", List.of("ekg", "ecg", "12 lead")),
            new ProcedureCode("83036", "CPT", "Hemoglobin A1c", "Hemoglobin; glycosylated (A1C)", "Pathology and Laboratory", LocalDate.of(2024, 1, 1), null, true, "Healthcare Hero sample index", List.of("a1c", "hba1c", "diabetes lab")),
            new ProcedureCode("80053", "CPT", "Comprehensive metabolic panel", "Comprehensive metabolic panel", "Pathology and Laboratory", LocalDate.of(2024, 1, 1), null, true, "Healthcare Hero sample index", List.of("cmp", "metabolic panel")),
            new ProcedureCode("99213", "CPT", "Office visit established patient, low", "Office or other outpatient visit for established patient, low medical decision making", "Evaluation and Management", LocalDate.of(2024, 1, 1), null, true, "Healthcare Hero sample index", List.of("office visit", "follow up", "established patient")),
            new ProcedureCode("J1100", "HCPCS Level II", "Dexamethasone sodium phosphate", "Injection, dexamethasone sodium phosphate, 1 mg", "Drugs Administered Other Than Oral Method", LocalDate.of(2024, 1, 1), null, true, "Healthcare Hero sample index", List.of("dexamethasone", "decadron")),
            new ProcedureCode("25", "modifier", "Significant separately identifiable E/M service", "Significant, separately identifiable evaluation and management service by the same physician on the same day", "Modifier", LocalDate.of(2024, 1, 1), null, true, "Healthcare Hero sample index", List.of("separate e/m", "same day modifier"))
    );

    private final ProcedureCodeRepository repository;
    private final Map<String, CachedSearch> cache = new ConcurrentHashMap<>();

    public CptSearchService(ProcedureCodeRepository repository) {
        this.repository = repository;
    }

    CptSearchService() {
        this.repository = null;
    }

    public ProcedureSearchResponse search(ProcedureSearchRequest request) {
        String query = normalize(request.query());
        int limit = limit(request.resultLimit());
        if (query.length() < 2) {
            return new ProcedureSearchResponse(request.query(), Instant.now(), LICENSING_NOTICE, List.of());
        }
        String cacheKey = query + "|" + limit + "|" + request.effectiveDate();
        CachedSearch cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return new ProcedureSearchResponse(request.query(), Instant.now(), LICENSING_NOTICE, cached.results());
        }

        Map<String, ProcedureSearchResult> results = new LinkedHashMap<>();
        for (ProcedureCode code : candidateCodes(query)) {
            if (!effectiveOn(code, request.effectiveDate())) {
                continue;
            }
            int confidence = confidence(query, code);
            if (confidence > 0 || matchesRange(query, code.code())) {
                ProcedureSearchResult result = toResult(code, Math.max(confidence, 75), reason(query, code));
                results.putIfAbsent(result.code(), result);
            }
        }

        List<ProcedureSearchResult> ranked = results.values().stream()
                .sorted(Comparator.comparingInt(ProcedureSearchResult::confidence).reversed().thenComparing(ProcedureSearchResult::code))
                .limit(limit)
                .toList();
        cache.put(cacheKey, new CachedSearch(Instant.now().plus(java.time.Duration.ofHours(24)), ranked));
        return new ProcedureSearchResponse(request.query(), Instant.now(), LICENSING_NOTICE, ranked);
    }

    public ProcedureSearchResult lookup(String code) {
        String normalized = normalize(code).toUpperCase(Locale.ROOT);
        return candidateCodes(normalized).stream()
                .filter(candidate -> candidate.code().equalsIgnoreCase(normalized))
                .findFirst()
                .map(candidate -> toResult(candidate, 100, "Exact code match."))
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Procedure code not found: " + code));
    }

    public Optional<ProcedureCode> findCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return candidateCodes(code).stream().filter(candidate -> candidate.code().equalsIgnoreCase(code.trim())).findFirst();
    }

    private List<ProcedureCode> candidateCodes(String query) {
        Map<String, ProcedureCode> codes = new LinkedHashMap<>();
        if (repository != null) {
            repository.findFirstByCodeIgnoreCase(query).map(this::fromEntity).ifPresent(code -> codes.put(code.code(), code));
            for (ProcedureCodeEntity entity : repository.findTop25ByCodeContainingIgnoreCaseOrShortDescriptionContainingIgnoreCaseOrLongDescriptionContainingIgnoreCase(query, query, query)) {
                ProcedureCode code = fromEntity(entity);
                codes.putIfAbsent(code.code(), code);
            }
        }
        for (ProcedureCode code : BUILT_IN_CODES) {
            codes.putIfAbsent(code.code(), code);
        }
        return new ArrayList<>(codes.values());
    }

    private ProcedureCode fromEntity(ProcedureCodeEntity entity) {
        return new ProcedureCode(entity.getCode(), entity.getCodeType(), entity.getShortDescription(), entity.getLongDescription(), entity.getCategory(), entity.getEffectiveDate(), entity.getTerminationDate(), entity.isActive(), entity.getSource(), List.of());
    }

    private ProcedureSearchResult toResult(ProcedureCode code, int confidence, String reason) {
        return new ProcedureSearchResult(code.code(), code.codeType(), code.shortDescription(), code.longDescription(), code.category(), Math.min(100, confidence), code.active(), code.effectiveDate(), code.terminationDate(), code.source(), reason);
    }

    private int confidence(String query, ProcedureCode code) {
        String haystack = normalize(String.join(" ", code.code(), code.shortDescription(), code.longDescription(), code.category(), String.join(" ", code.synonyms())));
        if (code.code().equalsIgnoreCase(query)) {
            return 100;
        }
        if (haystack.contains(query)) {
            return code.code().equalsIgnoreCase(query) ? 100 : 92;
        }
        int score = 0;
        for (String token : query.split("\\s+")) {
            if (token.length() > 1 && haystack.contains(token)) {
                score += 18;
            }
        }
        if (query.contains("xray") || query.contains("x-ray")) {
            score += haystack.contains("radiologic") ? 22 : 0;
        }
        return Math.min(score, 89);
    }

    private String reason(String query, ProcedureCode code) {
        if (code.code().equalsIgnoreCase(query)) {
            return "Exact code match.";
        }
        if (matchesRange(query, code.code())) {
            return "Code falls inside requested range.";
        }
        return "Keyword, synonym, or category match.";
    }

    private boolean matchesRange(String query, String code) {
        if (!RANGE.matcher(query).matches() || !code.chars().allMatch(Character::isDigit)) {
            return false;
        }
        String[] parts = query.replace(" ", "").split("-");
        int start = Integer.parseInt(parts[0]);
        int end = Integer.parseInt(parts[1]);
        int value = Integer.parseInt(code);
        return value >= start && value <= end;
    }

    private boolean effectiveOn(ProcedureCode code, LocalDate date) {
        if (date == null) {
            return true;
        }
        boolean afterStart = code.effectiveDate() == null || !date.isBefore(code.effectiveDate());
        boolean beforeEnd = code.terminationDate() == null || !date.isAfter(code.terminationDate());
        return afterStart && beforeEnd;
    }

    private int limit(Integer requested) {
        return Math.max(1, Math.min(requested == null ? DEFAULT_LIMIT : requested, MAX_LIMIT));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("x-ray", "xray").replaceAll("[^a-z0-9\\- ]", " ").replaceAll("\\s+", " ").trim();
    }

    private record CachedSearch(Instant expiresAt, List<ProcedureSearchResult> results) {
    }
}
