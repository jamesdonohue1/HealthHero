package com.hl7decoder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hl7decoder.api.dto.icd10.Icd10AutocompleteRequest;
import com.hl7decoder.api.dto.icd10.Icd10ExportRequest;
import com.hl7decoder.api.dto.icd10.Icd10SearchRequest;
import com.hl7decoder.model.icd10.Icd10AutocompleteResponse;
import com.hl7decoder.model.icd10.Icd10AutocompleteSuggestion;
import com.hl7decoder.model.icd10.Icd10DiagnosisGroup;
import com.hl7decoder.model.icd10.Icd10RefineResponse;
import com.hl7decoder.model.icd10.Icd10SavedSearchResponse;
import com.hl7decoder.model.icd10.Icd10SearchResponse;
import com.hl7decoder.model.icd10.Icd10SearchResult;
import com.hl7decoder.model.icd10.Icd10SelectedCode;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
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
import java.util.Optional;
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
    private static final Duration CACHE_TTL = Duration.ofHours(6);
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

    private final RestClient restClient;
    private final String apiBaseUrl;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Icd10SavedSearchResponse> savedSearches = new ConcurrentHashMap<>();

    public Icd10Service(RestClient.Builder restClientBuilder,
                        @Value("${app.icd10.api-base-url:https://clinicaltables.nlm.nih.gov/api/icd10cm/v3/search}") String apiBaseUrl) {
        this.restClient = restClientBuilder.build();
        this.apiBaseUrl = apiBaseUrl;
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
                        searchConcept(concept, limit)))
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
        Icd10SearchResponse search = search(request);
        Icd10SavedSearchResponse saved = new Icd10SavedSearchResponse(UUID.randomUUID().toString(), Instant.now().plus(SAVE_TTL), search);
        savedSearches.put(saved.id(), saved);
        return saved;
    }

    public List<Icd10SavedSearchResponse> history() {
        purgeExpiredSavedSearches();
        return savedSearches.values().stream()
                .sorted(Comparator.comparing(Icd10SavedSearchResponse::expiresAt).reversed())
                .toList();
    }

    public Icd10SavedSearchResponse saved(String id) {
        purgeExpiredSavedSearches();
        return Optional.ofNullable(savedSearches.get(id))
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Saved ICD-10 search not found."));
    }

    public void deleteSaved(String id) {
        savedSearches.remove(id);
    }

    public byte[] exportJson(Icd10ExportRequest request) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"inputText\": ").append(jsonString(request.inputText())).append(",\n");
        json.append("  \"selectedOnly\": ").append(Boolean.TRUE.equals(request.selectedOnly())).append(",\n");
        json.append("  \"selectedCodes\": [");
        List<Icd10SelectedCode> selectedCodes = request.selectedCodes() == null ? List.of() : request.selectedCodes();
        Icd10SearchResponse search = Boolean.TRUE.equals(request.selectedOnly())
                ? null
                : search(new Icd10SearchRequest(request.inputText(), request.resultLimit(), true, false));
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
                            .append(", \"chapter\": ").append(jsonString(result.chapter())).append('}');
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
        List<Icd10SelectedCode> selectedCodes = request.selectedCodes() == null ? List.of() : request.selectedCodes();
        StringBuilder csv = new StringBuilder("diagnosisText,code,description,longDescription,rank,score,matchPercentage,billable,chapter\n");
        if (Boolean.TRUE.equals(request.selectedOnly())) {
            for (Icd10SelectedCode code : selectedCodes) {
                csv.append(escape("selected")).append(',')
                        .append(escape(code.code())).append(',')
                        .append(escape(code.description())).append(',')
                        .append(escape(code.longDescription())).append(',')
                        .append(',').append(',').append(',')
                        .append(Boolean.TRUE.equals(code.billable())).append(',')
                        .append(escape(code.chapter())).append('\n');
            }
        } else {
            Icd10SearchResponse search = search(new Icd10SearchRequest(request.inputText(), request.resultLimit(), true, false));
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
                            .append(escape(result.chapter())).append('\n');
                }
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportText(Icd10ExportRequest request) {
        StringBuilder text = new StringBuilder();
        text.append("ICD-10 Search\n");
        text.append("Input: ").append(request.inputText()).append("\n\n");
        if (Boolean.TRUE.equals(request.selectedOnly())) {
            for (Icd10SelectedCode code : request.selectedCodes() == null ? List.<Icd10SelectedCode>of() : request.selectedCodes()) {
                text.append(code.code()).append(" - ").append(code.description()).append('\n');
            }
        } else {
            Icd10SearchResponse search = search(new Icd10SearchRequest(request.inputText(), request.resultLimit(), true, false));
            text.append("Normalized: ").append(search.normalizedInput()).append("\n\n");
            for (Icd10DiagnosisGroup group : search.diagnosisGroups()) {
                text.append(group.diagnosisText()).append('\n');
                for (Icd10SearchResult result : group.results()) {
                    text.append(result.rank()).append(". ").append(result.code()).append(" - ")
                            .append(result.longDescription()).append(" (")
                            .append(result.matchPercentage()).append("% match)\n");
                }
                text.append('\n');
            }
        }
        text.append("\n").append(DISCLAIMER);
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(Icd10ExportRequest request) {
        Icd10SearchResponse search = Boolean.TRUE.equals(request.selectedOnly())
                ? null
                : search(new Icd10SearchRequest(request.inputText(), request.resultLimit(), true, false));
        List<Icd10SelectedCode> selectedCodes = request.selectedCodes() == null ? List.of() : request.selectedCodes();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("ICD-10 Search Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Timestamp: " + Instant.now()));
            document.add(new Paragraph("Search input: " + request.inputText()));
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
                                + " | Match: " + result.matchPercentage() + "% | Billable: " + result.billable()));
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

        if (merged.isEmpty() && lastFailure != null) {
            throw new IllegalStateException("ICD-10 search is temporarily unavailable. Please refine the diagnosis text and try again.", lastFailure);
        }

        List<Icd10SearchResult> results = rerank(merged.values().stream().limit(limit).toList());
        cache.put(key, new CacheEntry(Instant.now().plus(CACHE_TTL), results));
        return results;
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
            } catch (RuntimeException ex) {
                lastFailure = ex;
                backoff(attempt);
            }
        }
        throw lastFailure == null
                ? new IllegalStateException("ICD-10 search is temporarily unavailable.")
                : lastFailure;
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
                    matchReason(originalConcept, queryConcept, description)));
        }
        return results;
    }

    private List<Icd10SearchResult> rerank(List<Icd10SearchResult> results) {
        List<Icd10SearchResult> reranked = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Icd10SearchResult result = results.get(i);
            reranked.add(new Icd10SearchResult(
                    result.code(),
                    result.shortDescription(),
                    result.longDescription(),
                    i + 1,
                    result.score(),
                    result.matchPercentage(),
                    result.billable(),
                    result.chapter(),
                    result.matchReason()));
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
        return Math.round(Math.max(originalScore, queryScore) * 100.0) / 100.0;
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

    private void purgeExpiredSavedSearches() {
        Instant now = Instant.now();
        savedSearches.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
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
}
