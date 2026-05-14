package com.hl7decoder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hl7decoder.api.dto.icd10.Icd10AutocompleteRequest;
import com.hl7decoder.api.dto.icd10.Icd10ExportRequest;
import com.hl7decoder.api.dto.icd10.Icd10SearchRequest;
import com.hl7decoder.model.compliance.AuditAction;
import com.hl7decoder.model.icd10.Icd10AutocompleteResponse;
import com.hl7decoder.model.icd10.Icd10AutocompleteSuggestion;
import com.hl7decoder.model.icd10.Icd10DiagnosisGroup;
import com.hl7decoder.model.icd10.Icd10RefineResponse;
import com.hl7decoder.model.icd10.Icd10SavedSearchResponse;
import com.hl7decoder.model.icd10.Icd10SearchResponse;
import com.hl7decoder.model.icd10.Icd10SearchResult;
import com.hl7decoder.model.icd10.Icd10SelectedCode;
import com.hl7decoder.persistence.SavedIcd10Search;
import com.hl7decoder.persistence.SavedIcd10SearchRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class Icd10Service {
    public static final String DISCLAIMER = "ICD-10 results are suggestions only and may be incomplete or inaccurate. Always verify codes with official coding guidelines, payer requirements, and a certified medical coder or qualified healthcare professional. Do not submit PHI unless authorized.";

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;
    private static final double FALLBACK_SCORE_PENALTY = 0.9;
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Duration SAVE_TTL = Duration.ofHours(24);
    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}&&[^/\\-\\n]]+");
    private static final Pattern SPACE = Pattern.compile("[ \\t\\x0B\\f]+");
    private static final List<String> COMMON_PHRASES = List.of(
            "chest pain",
            "shortness of breath",
            "type 2 diabetes mellitus",
            "left ankle sprain initial encounter",
            "chronic left knee pain",
            "hypertension",
            "acute upper respiratory infection",
            "low back pain",
            "abdominal pain",
            "urinary tract infection"
    );
    private static final Map<String, String> ABBREVIATIONS = Map.ofEntries(
            Map.entry("\\bsob\\b", "shortness of breath"),
            Map.entry("\\bdm2\\b", "type 2 diabetes mellitus"),
            Map.entry("\\bt2dm\\b", "type 2 diabetes mellitus"),
            Map.entry("\\bhtn\\b", "hypertension"),
            Map.entry("\\bcopd\\b", "chronic obstructive pulmonary disease"),
            Map.entry("\\buri\\b", "upper respiratory infection"),
            Map.entry("\\buti\\b", "urinary tract infection")
    );
    private static final List<LocalIcd10Code> LOCAL_CODES = List.of(
            new LocalIcd10Code("R05.9", "Cough, unspecified", "Cough, unspecified", "Symptoms, signs and abnormal clinical and laboratory findings", List.of("cough")),
            new LocalIcd10Code("R07.9", "Chest pain, unspecified", "Chest pain, unspecified", "Symptoms, signs and abnormal clinical and laboratory findings", List.of("chest pain")),
            new LocalIcd10Code("M25.562", "Pain in left knee", "Pain in left knee", "Diseases of the musculoskeletal system and connective tissue", List.of("left knee pain", "chronic left knee pain")),
            new LocalIcd10Code("M25.561", "Pain in right knee", "Pain in right knee", "Diseases of the musculoskeletal system and connective tissue", List.of("right knee pain")),
            new LocalIcd10Code("E11.9", "Type 2 diabetes mellitus without complications", "Type 2 diabetes mellitus without complications", "Endocrine, nutritional and metabolic diseases", List.of("diabetes", "type 2 diabetes", "dm2")),
            new LocalIcd10Code("R06.02", "Shortness of breath", "Shortness of breath", "Symptoms, signs and abnormal clinical and laboratory findings", List.of("shortness of breath", "sob"))
    );

    private final RestClient restClient;
    private final String apiBaseUrl;
    private final SavedIcd10SearchRepository savedSearchRepository;
    private final ObjectMapper objectMapper;
    private final PayloadEncryptionService encryptionService;
    private final PhiScannerService phiScannerService;
    private final AuditService auditService;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Instant> pausedQueries = new ConcurrentHashMap<>();

    @Autowired
    public Icd10Service(RestClient.Builder restClientBuilder,
                        SavedIcd10SearchRepository savedSearchRepository,
                        ObjectMapper objectMapper,
                        PayloadEncryptionService encryptionService,
                        PhiScannerService phiScannerService,
                        AuditService auditService,
                        @Value("${app.icd10.api-base-url:https://clinicaltables.nlm.nih.gov/api/icd10cm/v3/search}") String apiBaseUrl) {
        this.restClient = restClientBuilder.build();
        this.apiBaseUrl = apiBaseUrl;
        this.savedSearchRepository = savedSearchRepository;
        this.objectMapper = objectMapper;
        this.encryptionService = encryptionService;
        this.phiScannerService = phiScannerService;
        this.auditService = auditService;
    }

    public Icd10Service(RestClient.Builder restClientBuilder,
                        SavedIcd10SearchRepository savedSearchRepository,
                        ObjectMapper objectMapper,
                        String apiBaseUrl) {
        this(restClientBuilder, savedSearchRepository, objectMapper,
                new PayloadEncryptionService("local-dev-key-change-me", "v1"), new PhiScannerService(), null, apiBaseUrl);
    }

    Icd10Service(RestClient.Builder restClientBuilder, String apiBaseUrl) {
        this(restClientBuilder, null, new ObjectMapper().findAndRegisterModules(),
                new PayloadEncryptionService("local-dev-key-change-me", "v1"), new PhiScannerService(), null, apiBaseUrl);
    }

    public Icd10SearchResponse search(Icd10SearchRequest request) {
        List<String> concepts = detectConcepts(request.inputText());
        int limit = resultLimit(request.resultLimit());
        List<Icd10DiagnosisGroup> groups = concepts.stream()
                .map(concept -> new Icd10DiagnosisGroup(
                        concept,
                        needsMoreInformation(concept),
                        Boolean.FALSE.equals(request.includeClarifyingQuestions()) ? List.of() : clarifyingQuestions(concept),
                        Boolean.FALSE.equals(request.includeClarifyingQuestions()) ? List.of() : refinementSuggestions(concept),
                        queryVariants(concept),
                        0,
                        0,
                        searchConcept(concept, limit)))
                .map(group -> new Icd10DiagnosisGroup(
                        group.diagnosisText(),
                        group.needsMoreInformation(),
                        group.clarifyingQuestions(),
                        group.refinementSuggestions(),
                        group.results().stream().map(Icd10SearchResult::queryTerm).distinct().toList(),
                        (int) group.results().stream().filter(result -> !result.fallbackMatch()).count(),
                        (int) group.results().stream().filter(Icd10SearchResult::fallbackMatch).count(),
                        group.results()))
                .toList();
        return new Icd10SearchResponse(
                request.inputText().trim(),
                String.join("; ", concepts),
                Instant.now(),
                DISCLAIMER,
                groups);
    }

    public Icd10AutocompleteResponse autocomplete(Icd10AutocompleteRequest request) {
        String normalized = normalize(request.inputText());
        int limit = Math.min(resultLimit(request.resultLimit()), 10);
        Map<String, Icd10AutocompleteSuggestion> suggestions = new LinkedHashMap<>();
        COMMON_PHRASES.stream()
                .filter(phrase -> phrase.contains(normalized))
                .limit(limit)
                .forEach(phrase -> suggestions.put("phrase:" + phrase, new Icd10AutocompleteSuggestion(null, phrase)));
        for (Icd10SearchResult result : searchConcept(normalized, limit)) {
            suggestions.put(result.code(), new Icd10AutocompleteSuggestion(result.code(), result.longDescription()));
        }
        return new Icd10AutocompleteResponse(request.inputText(), suggestions.values().stream().limit(limit).toList());
    }

    public Icd10RefineResponse refine(Icd10SearchRequest request) {
        List<String> concepts = detectConcepts(request.inputText());
        List<String> questions = concepts.stream()
                .flatMap(concept -> clarifyingQuestions(concept).stream())
                .distinct()
                .toList();
        return new Icd10RefineResponse(request.inputText(), String.join("; ", concepts), concepts, questions);
    }

    public Icd10SavedSearchResponse save(Icd10SearchRequest request) {
        return save(request, null);
    }

    public Icd10SavedSearchResponse save(Icd10SearchRequest request, UUID organizationId) {
        return save(request, organizationId, null);
    }

    public Icd10SavedSearchResponse save(Icd10SearchRequest request, UUID organizationId, UUID userId) {
        ensureSavedSearchRepository();
        Icd10SearchRequest effectiveRequest = redactedSearchRequest(request);
        Icd10SearchResponse search = search(effectiveRequest);
        UUID id = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(SAVE_TTL);
        SavedIcd10Search saved = new SavedIcd10Search(id, Instant.now(), expiresAt, organizationId, encryptionService.encrypt(writeSearch(search)));
        savedSearchRepository.save(saved);
        audit(AuditAction.SAVE, organizationId, userId, "ICD10_SEARCH", id.toString(), "saved ICD-10 search; redacted=" + Boolean.TRUE.equals(request.redactPhi()));
        return new Icd10SavedSearchResponse(id.toString(), expiresAt, search);
    }

    public List<Icd10SavedSearchResponse> history() {
        return history(null);
    }

    public List<Icd10SavedSearchResponse> history(UUID organizationId) {
        ensureSavedSearchRepository();
        deleteExpiredSavedSearches();
        List<SavedIcd10Search> searches = organizationId == null ? savedSearchRepository.findAll() : savedSearchRepository.findByOrganizationId(organizationId);
        return searches.stream()
                .map(this::savedResponse)
                .sorted(Comparator.comparing(Icd10SavedSearchResponse::expiresAt).reversed())
                .toList();
    }

    public Icd10SavedSearchResponse saved(String id) {
        return saved(id, null);
    }

    public Icd10SavedSearchResponse saved(String id, UUID organizationId) {
        ensureSavedSearchRepository();
        deleteExpiredSavedSearches();
        UUID uuid = UUID.fromString(id);
        SavedIcd10Search saved = (organizationId == null ? savedSearchRepository.findById(uuid) : savedSearchRepository.findByIdAndOrganizationId(uuid, organizationId))
                .filter(search -> search.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new EntityNotFoundException("Saved ICD-10 search not found."));
        return savedResponse(saved);
    }

    public void deleteSaved(String id) {
        deleteSaved(id, null);
    }

    public void deleteSaved(String id, UUID organizationId) {
        deleteSaved(id, organizationId, null);
    }

    public void deleteSaved(String id, UUID organizationId, UUID userId) {
        ensureSavedSearchRepository();
        UUID uuid = UUID.fromString(id);
        if (organizationId == null) {
            savedSearchRepository.deleteById(uuid);
            audit(AuditAction.DELETE, null, userId, "ICD10_SEARCH", id, "user-controlled saved ICD-10 delete");
            return;
        }
        SavedIcd10Search saved = savedSearchRepository.findByIdAndOrganizationId(uuid, organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Saved ICD-10 search not found."));
        savedSearchRepository.delete(saved);
        audit(AuditAction.DELETE, organizationId, userId, "ICD10_SEARCH", id, "user-controlled saved ICD-10 delete");
    }

    public byte[] exportJson(Icd10ExportRequest request) {
        Icd10ExportRequest effectiveRequest = redactedExportRequest(request);
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"inputText\": ").append(jsonString(effectiveRequest.inputText())).append(",\n");
        json.append("  \"selectedOnly\": ").append(Boolean.TRUE.equals(request.selectedOnly())).append(",\n");
        json.append("  \"selectedCodes\": [");
        List<Icd10SelectedCode> selectedCodes = effectiveRequest.selectedCodes() == null ? List.of() : effectiveRequest.selectedCodes();
        Icd10SearchResponse search = Boolean.TRUE.equals(request.selectedOnly())
                ? null
                : search(new Icd10SearchRequest(effectiveRequest.inputText(), effectiveRequest.resultLimit(), true, false));
        for (int i = 0; i < selectedCodes.size(); i++) {
            Icd10SelectedCode code = selectedCodes.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("\n    {\"code\": ").append(jsonString(code.code()))
                    .append(", \"description\": ").append(jsonString(code.description()))
                    .append(", \"longDescription\": ").append(jsonString(code.longDescription()))
                    .append(", \"billable\": ").append(Boolean.TRUE.equals(code.billable()))
                    .append(", \"chapter\": ").append(jsonString(code.chapter())).append('}');
        }
        if (!selectedCodes.isEmpty()) {
            json.append('\n');
        }
        json.append("  ]");
        if (search != null) {
            json.append(",\n  \"normalizedInput\": ").append(jsonString(search.normalizedInput())).append(",\n");
            json.append("  \"diagnosisGroups\": [");
            for (int i = 0; i < search.diagnosisGroups().size(); i++) {
                Icd10DiagnosisGroup group = search.diagnosisGroups().get(i);
                if (i > 0) {
                    json.append(',');
                }
                json.append("\n    {\"diagnosisText\": ").append(jsonString(group.diagnosisText()))
                        .append(", \"needsMoreInformation\": ").append(group.needsMoreInformation())
                        .append(", \"queryTerms\": [");
                for (int j = 0; j < group.queryTerms().size(); j++) {
                    if (j > 0) {
                        json.append(',');
                    }
                    json.append(jsonString(group.queryTerms().get(j)));
                }
                json.append("]")
                        .append(", \"exactMatchCount\": ").append(group.exactMatchCount())
                        .append(", \"fallbackMatchCount\": ").append(group.fallbackMatchCount())
                        .append(", \"refinementSuggestions\": [");
                for (int j = 0; j < group.refinementSuggestions().size(); j++) {
                    if (j > 0) {
                        json.append(',');
                    }
                    json.append(jsonString(group.refinementSuggestions().get(j)));
                }
                json.append("]")
                        .append(", \"results\": [");
                for (int j = 0; j < group.results().size(); j++) {
                    Icd10SearchResult result = group.results().get(j);
                    if (j > 0) {
                        json.append(',');
                    }
                    json.append("{\"code\": ").append(jsonString(result.code()))
                            .append(", \"longDescription\": ").append(jsonString(result.longDescription()))
                            .append(", \"rank\": ").append(result.rank())
                            .append(", \"score\": ").append(result.score())
                            .append(", \"matchPercentage\": ").append(result.matchPercentage())
                            .append(", \"billable\": ").append(result.billable())
                            .append(", \"chapter\": ").append(jsonString(result.chapter()))
                            .append(", \"queryTerm\": ").append(jsonString(result.queryTerm()))
                            .append(", \"fallbackMatch\": ").append(result.fallbackMatch())
                            .append(", \"source\": ").append(jsonString(result.source())).append('}');
                }
                json.append("]}");
            }
            if (!search.diagnosisGroups().isEmpty()) {
                json.append('\n');
            }
            json.append("  ]");
        }
        json.append("\n}");
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportCsv(Icd10ExportRequest request) {
        Icd10ExportRequest effectiveRequest = redactedExportRequest(request);
        List<Icd10SelectedCode> selectedCodes = effectiveRequest.selectedCodes() == null ? List.of() : effectiveRequest.selectedCodes();
        StringBuilder csv = new StringBuilder("diagnosisText,code,description,longDescription,rank,score,matchPercentage,billable,chapter,queryTerm,fallbackMatch,source\n");
        if (Boolean.TRUE.equals(request.selectedOnly())) {
            for (Icd10SelectedCode code : selectedCodes) {
                csv.append(escape("selected")).append(',')
                        .append(escape(code.code())).append(',')
                        .append(escape(code.description())).append(',')
                        .append(escape(code.longDescription())).append(',')
                        .append(',').append(',').append(',')
                        .append(Boolean.TRUE.equals(code.billable())).append(',')
                        .append(escape(code.chapter())).append(',')
                        .append(',').append(',').append(',')
                        .append('\n');
            }
        } else {
            Icd10SearchResponse search = search(new Icd10SearchRequest(effectiveRequest.inputText(), effectiveRequest.resultLimit(), true, false));
            for (Icd10DiagnosisGroup group : search.diagnosisGroups()) {
                for (Icd10SearchResult result : group.results()) {
                    csv.append(escape(group.diagnosisText())).append(',')
                            .append(escape(result.code())).append(',')
                            .append(escape(result.shortDescription())).append(',')
                            .append(escape(result.longDescription())).append(',')
                            .append(result.rank()).append(',')
                            .append(result.score()).append(',')
                            .append(result.matchPercentage()).append(',')
                            .append(result.billable()).append(',')
                            .append(escape(result.chapter())).append(',')
                            .append(escape(result.queryTerm())).append(',')
                            .append(result.fallbackMatch()).append(',')
                            .append(escape(result.source())).append('\n');
                }
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportText(Icd10ExportRequest request) {
        Icd10ExportRequest effectiveRequest = redactedExportRequest(request);
        StringBuilder text = new StringBuilder();
        text.append("ICD-10 Search\n");
        text.append("Input: ").append(effectiveRequest.inputText()).append("\n\n");
        if (Boolean.TRUE.equals(request.selectedOnly())) {
            for (Icd10SelectedCode code : effectiveRequest.selectedCodes() == null ? List.<Icd10SelectedCode>of() : effectiveRequest.selectedCodes()) {
                text.append(code.code()).append(" - ").append(code.description()).append('\n');
            }
        } else {
            Icd10SearchResponse search = search(new Icd10SearchRequest(effectiveRequest.inputText(), effectiveRequest.resultLimit(), true, false));
            text.append("Normalized: ").append(search.normalizedInput()).append("\n\n");
            for (Icd10DiagnosisGroup group : search.diagnosisGroups()) {
                text.append(group.diagnosisText()).append('\n');
                for (Icd10SearchResult result : group.results()) {
                    text.append(result.rank()).append(". ").append(result.code()).append(" - ")
                            .append(result.longDescription()).append(" (")
                            .append(result.matchPercentage()).append("% match");
                    if (result.fallbackMatch()) {
                        text.append(", fallback: ").append(result.queryTerm());
                    }
                    text.append(")\n");
                }
                text.append('\n');
            }
        }
        text.append("\n").append(DISCLAIMER);
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(Icd10ExportRequest request) {
        Icd10ExportRequest effectiveRequest = redactedExportRequest(request);
        Icd10SearchResponse search = Boolean.TRUE.equals(request.selectedOnly())
                ? null
                : search(new Icd10SearchRequest(effectiveRequest.inputText(), effectiveRequest.resultLimit(), true, false));
        List<Icd10SelectedCode> selectedCodes = effectiveRequest.selectedCodes() == null ? List.of() : effectiveRequest.selectedCodes();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("ICD-10 Search Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Timestamp: " + Instant.now()));
            document.add(new Paragraph("Search input: " + effectiveRequest.inputText()));
            document.add(new Paragraph(DISCLAIMER));
            document.add(new Paragraph("Selected codes", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            for (Icd10SelectedCode code : selectedCodes) {
                document.add(new Paragraph(nullSafe(code.code()) + " - " + nullSafe(code.description()) + " | Billable: " + Boolean.TRUE.equals(code.billable())));
            }
            if (search != null) {
                document.add(new Paragraph("Normalized search terms: " + search.normalizedInput()));
                for (Icd10DiagnosisGroup group : search.diagnosisGroups()) {
                    document.add(new Paragraph("Diagnosis: " + group.diagnosisText(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                    for (Icd10SearchResult result : group.results()) {
                        document.add(new Paragraph(result.rank() + ". " + result.code() + " - " + result.longDescription()
                                + " | Match: " + result.matchPercentage() + "% | Query: " + result.queryTerm()
                                + " | Fallback: " + result.fallbackMatch() + " | Billable: " + result.billable()));
                    }
                }
            }
            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Unable to export ICD-10 PDF", ex);
        }
    }

    private List<Icd10SearchResult> searchConcept(String concept, int limit) {
        if (concept.length() < 3) {
            return List.of();
        }
        String key = concept + "|" + limit + "|nlm-v3|code-name|fallback-v1";
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.results();
        }

        Map<String, Icd10SearchResult> merged = new LinkedHashMap<>();
        RuntimeException lastFailure = null;
        List<String> variants = queryVariants(concept);
        for (int i = 0; i < variants.size() && merged.size() < limit; i++) {
            String query = variants.get(i);
            double penalty = i == 0 ? 1.0 : Math.max(0.72, FALLBACK_SCORE_PENALTY - ((i - 1) * 0.05));
            if (queryPaused(query)) {
                continue;
            }
            try {
                for (Icd10SearchResult result : searchApiConcept(concept, query, limit, penalty)) {
                    merged.putIfAbsent(result.code(), result);
                    if (merged.size() >= limit) {
                        break;
                    }
                }
            } catch (RuntimeException ex) {
                lastFailure = ex;
            }
        }

        for (int i = 0; i < variants.size() && merged.isEmpty(); i++) {
            String query = variants.get(i);
            double penalty = i == 0 ? 1.0 : Math.max(0.72, FALLBACK_SCORE_PENALTY - ((i - 1) * 0.05));
            for (Icd10SearchResult result : searchLocalConcept(concept, query, limit, penalty)) {
                merged.putIfAbsent(result.code(), result);
            }
        }

        if (merged.isEmpty() && lastFailure != null) {
            throw new Icd10LookupException("ICD-10 search is temporarily unavailable. Please refine the diagnosis text and try again.", lastFailure);
        }

        List<Icd10SearchResult> results = rerank(merged.values().stream().limit(limit).toList());
        cache.put(key, new CacheEntry(Instant.now().plus(CACHE_TTL), results));
        return results;
    }

    private boolean queryPaused(String query) {
        Instant pausedUntil = pausedQueries.get(normalize(query));
        if (pausedUntil == null) {
            return false;
        }
        if (pausedUntil.isBefore(Instant.now())) {
            pausedQueries.remove(normalize(query));
            return false;
        }
        return true;
    }

    private List<Icd10SearchResult> searchLocalConcept(String originalConcept, String queryConcept, int limit, double scorePenalty) {
        String query = normalize(queryConcept);
        List<Icd10SearchResult> results = new ArrayList<>();
        for (LocalIcd10Code code : LOCAL_CODES) {
            String haystack = normalize(String.join(" ", code.code(), code.shortDescription(), code.longDescription(), String.join(" ", code.synonyms())));
            if (haystack.contains(query) || query.contains(normalize(code.shortDescription())) || code.synonyms().stream().anyMatch(synonym -> query.contains(normalize(synonym)))) {
                double score = Math.min(0.98, 0.88 * scorePenalty);
                results.add(new Icd10SearchResult(
                        code.code(),
                        code.shortDescription(),
                        code.longDescription(),
                        0,
                        score,
                        (int) Math.round(score * 100),
                        true,
                        code.chapter(),
                        "Matched local ICD-10 safety index before external lookup.",
                        queryConcept,
                        !queryConcept.equals(originalConcept),
                        "Healthcare Hero local ICD-10 index"
                ));
            }
        }
        return results.stream().limit(limit).toList();
    }

    private List<Icd10SearchResult> searchApiConcept(String originalConcept, String queryConcept, int limit, double scorePenalty) {
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                JsonNode response = restClient.get()
                        .uri(apiBaseUrl, uri -> icd10Uri(uri, queryConcept, limit))
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(JsonNode.class);
                return parseNlmResponse(originalConcept, queryConcept, response, scorePenalty);
            } catch (RestClientResponseException ex) {
                lastFailure = ex;
                if (ex.getStatusCode().value() == 429) {
                    pausedQueries.put(normalize(queryConcept), Instant.now().plus(retryAfter(ex)));
                    throw new Icd10LookupException("ICD-10 source rate limit reached. This query is paused temporarily; cached/local results will continue to be used when available.", ex);
                }
                backoff(attempt);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                backoff(attempt);
            }
        }
        throw lastFailure == null
                ? new IllegalStateException("ICD-10 search is temporarily unavailable.")
                : lastFailure;
    }

    private Duration retryAfter(RestClientResponseException ex) {
        String retryAfter = ex.getResponseHeaders() == null ? null : ex.getResponseHeaders().getFirst("Retry-After");
        if (retryAfter == null || retryAfter.isBlank()) {
            return Duration.ofMinutes(5);
        }
        try {
            return Duration.ofSeconds(Math.max(1, Long.parseLong(retryAfter.trim())));
        } catch (NumberFormatException ignored) {
            return Duration.ofMinutes(5);
        }
    }

    private java.net.URI icd10Uri(UriBuilder uri, String concept, int limit) {
        return uri.queryParam("sf", "code,name")
                .queryParam("df", "code,name")
                .queryParam("terms", concept)
                .queryParam("count", limit)
                .build();
    }

    private List<Icd10SearchResult> parseNlmResponse(String originalConcept, String queryConcept, JsonNode response, double scorePenalty) {
        if (response == null || !response.isArray() || response.size() < 4 || !response.get(3).isArray()) {
            return List.of();
        }
        List<Icd10SearchResult> results = new ArrayList<>();
        JsonNode displayRows = response.get(3);
        for (int i = 0; i < displayRows.size(); i++) {
            JsonNode row = displayRows.get(i);
            String code = row.size() > 0 ? row.get(0).asText() : "";
            String description = row.size() > 1 ? row.get(1).asText() : "";
            int rank = i + 1;
            double score = adjustedScore(originalConcept, queryConcept, description, rank, scorePenalty);
            results.add(new Icd10SearchResult(
                    code,
                    description,
                    description,
                    rank,
                    score,
                    matchPercentage(score),
                    isLikelyBillable(code),
                    chapter(code),
                    matchReason(originalConcept, queryConcept, description),
                    queryConcept,
                    !originalConcept.equals(queryConcept),
                    "NLM Clinical Tables ICD-10-CM"));
        }
        return results;
    }

    private List<Icd10SearchResult> rerank(List<Icd10SearchResult> results) {
        List<Icd10SearchResult> reranked = new ArrayList<>();
        List<Icd10SearchResult> ranked = results.stream()
                .sorted(Comparator.comparing(Icd10SearchResult::score).reversed()
                        .thenComparing(Icd10SearchResult::fallbackMatch)
                        .thenComparing(Icd10SearchResult::rank))
                .toList();
        for (int i = 0; i < ranked.size(); i++) {
            Icd10SearchResult result = ranked.get(i);
            reranked.add(new Icd10SearchResult(
                    result.code(),
                    result.shortDescription(),
                    result.longDescription(),
                    i + 1,
                    result.score(),
                    result.matchPercentage(),
                    result.billable(),
                    result.chapter(),
                    result.matchReason(),
                    result.queryTerm(),
                    result.fallbackMatch(),
                    result.source()));
        }
        return reranked;
    }

    private List<String> queryVariants(String concept) {
        Set<String> variants = new LinkedHashSet<>();
        String base = normalize(concept);
        variants.add(base);

        String withoutPainQualifiers = normalize(base
                .replaceAll("\\b(acute|chronic)\\b", " ")
                .replaceAll("\\bdue to documented condition\\b", " ")
                .replaceAll("\\binjury related\\b", " "));
        variants.add(withoutPainQualifiers);

        String withoutEncounter = normalize(withoutPainQualifiers
                .replaceAll("\\b(initial encounter|subsequent encounter|sequela)\\b", " "));
        variants.add(withoutEncounter);

        if (withoutPainQualifiers.matches(".*\\b(left|right|bilateral)\\b.*\\bpain\\b.*")) {
            variants.add(normalize(withoutPainQualifiers.replaceAll("\\b(left|right|bilateral)\\b", " ")));
        }
        if (base.matches(".*\\b(chronic|acute)\\b.*\\bpain\\b.*")) {
            variants.add(base.contains("chronic") ? "chronic pain" : "acute pain");
        }

        return variants.stream()
                .filter(variant -> variant.length() >= 3)
                .toList();
    }

    private List<String> detectConcepts(String input) {
        String normalized = normalize(input);
        List<String> concepts = new ArrayList<>();
        for (String line : normalized.split("\\n")) {
            String cleaned = removeFiller(line);
            for (String part : cleaned.split("\\s*(?:;|,|\\band\\b)\\s*")) {
                String concept = removeFiller(part);
                if (concept.length() >= 3) {
                    concepts.add(concept);
                }
            }
        }
        return concepts.stream().distinct().limit(12).toList();
    }

    private String normalize(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFKC)
                .toLowerCase(Locale.US)
                .replace("\r", "\n");
        for (Map.Entry<String, String> abbreviation : ABBREVIATIONS.entrySet()) {
            normalized = normalized.replaceAll(abbreviation.getKey(), abbreviation.getValue());
        }
        normalized = PUNCTUATION.matcher(normalized).replaceAll(" ");
        normalized = SPACE.matcher(normalized).replaceAll(" ");
        normalized = normalized
                .replaceAll("\\b(due to documented condition)(?:\\s+due to documented condition)+\\b", "$1")
                .replaceAll("\\b(injury related)(?:\\s+injury related)+\\b", "$1")
                .replaceAll("\\b(initial encounter)(?:\\s+initial encounter)+\\b", "$1")
                .replaceAll("\\b(subsequent encounter)(?:\\s+subsequent encounter)+\\b", "$1");
        return normalized.replaceAll(" *\\n+ *", "\n").trim();
    }

    private String removeFiller(String text) {
        return SPACE.matcher(text
                .replaceAll("\\b(patient|pt|complains of|complaining of|has|have|history of|diagnosis of|dx of|reports|presenting with)\\b", " ")
                .replaceAll("\\b(the|a|an)\\b", " "))
                .replaceAll(" ")
                .trim();
    }

    private boolean needsMoreInformation(String concept) {
        return !clarifyingQuestions(concept).isEmpty();
    }

    private List<String> clarifyingQuestions(String concept) {
        List<String> questions = new ArrayList<>();
        boolean hasLaterality = concept.matches(".*\\b(left|right|bilateral|unspecified)\\b.*");
        boolean hasEncounter = concept.matches(".*\\b(initial encounter|subsequent encounter|sequela)\\b.*");
        boolean hasPainSpecificity = concept.matches(".*\\b(acute|chronic|injury related|due to documented condition)\\b.*");
        boolean hasConditionSpecificity = concept.matches(".*\\b(with|without|complication|complications|mild|moderate|severe|acute|chronic|type 1|type 2)\\b.*");

        if (!concept.matches(".*\\b(left|right|bilateral|unspecified)\\b.*") && concept.matches(".*\\b(ankle|knee|arm|leg|shoulder|hip|eye|ear|hand|foot|wrist)\\b.*")) {
            questions.add("Is laterality left, right, bilateral, or unspecified?");
        }
        if (concept.matches(".*\\b(sprain|fracture|injury|wound|burn)\\b.*") && !hasEncounter) {
            questions.add("Is this the initial encounter, subsequent encounter, or sequela?");
        }
        if (concept.matches(".*\\b(sprain|fracture|injury|wound|burn)\\b.*")
                && !concept.matches(".*\\b(ankle|knee|arm|leg|shoulder|hip|eye|ear|hand|foot|wrist|head|neck|back|chest|abdomen)\\b.*")) {
            questions.add("What anatomical site and injury cause are documented?");
        }
        if (concept.matches(".*\\bpain\\b.*") && !hasPainSpecificity) {
            questions.add("Is the pain acute, chronic, injury-related, or due to a documented condition?");
        }
        if (concept.matches(".*\\b(diabetes|infection|asthma|failure)\\b.*") && !hasConditionSpecificity) {
            questions.add("Are complications, severity, etiology, and manifestations documented?");
        }
        if (questions.isEmpty() && concept.length() < 8 && !hasLaterality) {
            questions.add("Is there additional clinical specificity documented for site, acuity, cause, severity, or complications?");
        }
        return questions;
    }

    private List<String> refinementSuggestions(String concept) {
        Set<String> suggestions = new LinkedHashSet<>();
        String base = removeFiller(concept);
        if (clarifyingQuestions(base).isEmpty()) {
            return List.of();
        }

        if (!base.matches(".*\\b(left|right|bilateral|unspecified)\\b.*")
                && base.matches(".*\\b(ankle|knee|arm|leg|shoulder|hip|eye|ear|hand|foot|wrist)\\b.*")) {
            suggestions.add(base + " left");
            suggestions.add(base + " right");
            suggestions.add(base + " bilateral");
        }
        if (base.matches(".*\\b(sprain|fracture|injury|wound|burn)\\b.*")
                && !base.matches(".*\\b(initial encounter|subsequent encounter|sequela)\\b.*")) {
            suggestions.add(base + " initial encounter");
            suggestions.add(base + " subsequent encounter");
            suggestions.add(base + " sequela");
        }
        if (base.matches(".*\\bpain\\b.*") && !base.matches(".*\\b(acute|chronic|injury related|due to documented condition)\\b.*")) {
            suggestions.add("acute " + base);
            suggestions.add("chronic " + base);
            suggestions.add(base + " due to documented condition");
            suggestions.add(base + " injury related");
        }
        if (base.matches(".*\\bdiabetes\\b.*") && !base.matches(".*\\b(type 1|type 2|with|without)\\b.*")) {
            suggestions.add("type 1 " + base);
            suggestions.add("type 2 " + base);
            suggestions.add(base + " with complications");
        }
        if (base.matches(".*\\binfection\\b.*") && !base.matches(".*\\b(acute|chronic|site|organism)\\b.*")) {
            suggestions.add("acute " + base);
            suggestions.add(base + " with documented organism");
        }
        if (suggestions.isEmpty() && !base.matches(".*\\b(acute|chronic)\\b.*")) {
            suggestions.add(base + " acute");
            suggestions.add(base + " chronic");
            suggestions.add(base + " unspecified");
        } else if (suggestions.isEmpty()) {
            suggestions.add(base + " unspecified");
            suggestions.add(base + " with complications");
        }

        return suggestions.stream()
                .map(this::normalize)
                .filter(suggestion -> !suggestion.equals(base) && suggestion.length() >= 3)
                .limit(6)
                .toList();
    }

    private double score(String concept, String description, int rank) {
        String[] terms = concept.split(" ");
        long matches = java.util.Arrays.stream(terms)
                .filter(term -> term.length() > 2 && description.toLowerCase(Locale.US).contains(term))
                .count();
        double lexical = terms.length == 0 ? 0.0 : (double) matches / terms.length;
        double ranked = Math.max(0.0, 1.0 - ((rank - 1) * 0.04));
        return Math.round(((lexical * 0.65) + (ranked * 0.35)) * 100.0) / 100.0;
    }

    private double adjustedScore(String originalConcept, String queryConcept, String description, int rank, double scorePenalty) {
        double originalScore = score(originalConcept, description, rank);
        double queryScore = score(queryConcept, description, rank) * scorePenalty;
        double siteBoost = bodySiteOverlap(originalConcept, description) ? 0.08 : 0.0;
        double broadPainPenalty = isBroadPainResult(originalConcept, description) ? 0.14 : 0.0;
        double adjusted = Math.max(originalScore, queryScore) + siteBoost - broadPainPenalty;
        return Math.round(Math.max(0.0, Math.min(1.0, adjusted)) * 100.0) / 100.0;
    }

    private boolean bodySiteOverlap(String concept, String description) {
        String normalizedDescription = normalize(description);
        return List.of("knee", "ankle", "shoulder", "hip", "wrist", "hand", "foot", "back", "chest", "abdomen", "eye", "ear")
                .stream()
                .anyMatch(site -> concept.matches(".*\\b" + site + "\\b.*") && normalizedDescription.matches(".*\\b" + site + "\\b.*"));
    }

    private boolean isBroadPainResult(String concept, String description) {
        String normalizedDescription = normalize(description);
        return concept.contains("pain")
                && !bodySiteOverlap(concept, normalizedDescription)
                && normalizedDescription.matches(".*\\b(chronic pain|acute pain|pain syndrome)\\b.*");
    }

    private int matchPercentage(double score) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, score)) * 100.0);
    }

    private boolean isLikelyBillable(String code) {
        String compact = code.replace(".", "");
        return compact.length() > 3;
    }

    private String chapter(String code) {
        if (code == null || code.isBlank()) {
            return "Unknown";
        }
        char first = Character.toUpperCase(code.charAt(0));
        return switch (first) {
            case 'A', 'B' -> "Certain infectious and parasitic diseases";
            case 'C', 'D' -> "Neoplasms and diseases of blood or immune mechanism";
            case 'E' -> "Endocrine, nutritional and metabolic diseases";
            case 'F' -> "Mental, behavioral and neurodevelopmental disorders";
            case 'G' -> "Diseases of the nervous system";
            case 'H' -> "Diseases of the eye, adnexa, ear and mastoid process";
            case 'I' -> "Diseases of the circulatory system";
            case 'J' -> "Diseases of the respiratory system";
            case 'K' -> "Diseases of the digestive system";
            case 'L' -> "Diseases of the skin and subcutaneous tissue";
            case 'M' -> "Diseases of the musculoskeletal system and connective tissue";
            case 'N' -> "Diseases of the genitourinary system";
            case 'O' -> "Pregnancy, childbirth and the puerperium";
            case 'P' -> "Certain conditions originating in the perinatal period";
            case 'Q' -> "Congenital malformations, deformations and chromosomal abnormalities";
            case 'R' -> "Symptoms, signs and abnormal clinical and laboratory findings";
            case 'S', 'T' -> "Injury, poisoning and certain other consequences of external causes";
            case 'V', 'W', 'X', 'Y' -> "External causes of morbidity";
            case 'Z' -> "Factors influencing health status and contact with health services";
            default -> "Unknown";
        };
    }

    private String matchReason(String originalConcept, String queryConcept, String description) {
        if (originalConcept.equals(queryConcept)) {
            return "Matched government ICD-10-CM code/name search for normalized term \"" + originalConcept + "\" against \"" + description + "\".";
        }
        return "Matched government ICD-10-CM code/name search for fallback term \"" + queryConcept
                + "\" after normalized term \"" + originalConcept + "\" returned too few direct matches.";
    }

    private int resultLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep((long) Math.pow(2, attempt) * 150L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void deleteExpiredSavedSearches() {
        ensureSavedSearchRepository();
        savedSearchRepository.deleteAll(savedSearchRepository.findByExpiresAtBefore(Instant.now()));
    }

    private Icd10SavedSearchResponse savedResponse(SavedIcd10Search saved) {
        try {
            return new Icd10SavedSearchResponse(
                    saved.getId().toString(),
                    saved.getExpiresAt(),
                    objectMapper.readValue(encryptionService.decrypt(saved.getSearchJson()), Icd10SearchResponse.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Saved ICD-10 search could not be decoded", ex);
        }
    }

    private Icd10SearchRequest redactedSearchRequest(Icd10SearchRequest request) {
        if (!Boolean.TRUE.equals(request.redactPhi())) {
            return request;
        }
        return new Icd10SearchRequest(phiScannerService.redact(request.inputText()), request.resultLimit(),
                request.includeClarifyingQuestions(), request.includeAiRefinement(), false);
    }

    private Icd10ExportRequest redactedExportRequest(Icd10ExportRequest request) {
        if (!Boolean.TRUE.equals(request.redactPhi())) {
            return request;
        }
        return new Icd10ExportRequest(phiScannerService.redact(request.inputText()), request.resultLimit(),
                request.selectedCodes(), request.selectedOnly(), false);
    }

    private void audit(AuditAction action, UUID organizationId, UUID userId, String resourceType, String resourceId, String details) {
        if (auditService != null) {
            auditService.record(action, organizationId, userId, resourceType, resourceId, true, details);
        }
    }

    private String writeSearch(Icd10SearchResponse search) {
        try {
            return objectMapper.writeValueAsString(search);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("ICD-10 search could not be serialized", ex);
        }
    }

    private void ensureSavedSearchRepository() {
        if (savedSearchRepository == null) {
            throw new IllegalStateException("Saved ICD-10 search repository is not configured.");
        }
    }

    private String escape(String value) {
        return "\"" + nullSafe(value).replace("\"", "\"\"") + "\"";
    }

    private String jsonString(String value) {
        return "\"" + nullSafe(value).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private record CacheEntry(Instant expiresAt, List<Icd10SearchResult> results) {
    }

    private record LocalIcd10Code(String code, String shortDescription, String longDescription, String chapter, List<String> synonyms) {
    }
}
