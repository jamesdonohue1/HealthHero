package com.hl7decoder.service;

import com.hl7decoder.api.dto.icd10.Icd10SearchRequest;
import com.hl7decoder.model.icd10.Icd10SearchResponse;
import com.hl7decoder.persistence.SavedIcd10Search;
import com.hl7decoder.persistence.SavedIcd10SearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Proxy;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Icd10ServiceTest {

    @Test
    void fallsBackWhenSpecificPainQualifierReturnsNoRows() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Icd10Service service = new Icd10Service(builder, "https://example.test/icd10");

        server.expect(once(), requestWithTerm("chronic left knee pain"))
                .andRespond(withSuccess(nlmRows(), MediaType.APPLICATION_JSON));
        server.expect(once(), requestWithTerm("left knee pain"))
                .andRespond(withSuccess(nlmRows(
                        row("M25.562", "Pain in left knee"),
                        row("M25.561", "Pain in right knee")
                ), MediaType.APPLICATION_JSON));

        Icd10SearchResponse response = service.search(new Icd10SearchRequest(
                "chronic left knee pain",
                2,
                true,
                false));

        assertThat(response.diagnosisGroups()).hasSize(1);
        assertThat(response.diagnosisGroups().getFirst().needsMoreInformation()).isFalse();
        assertThat(response.diagnosisGroups().getFirst().refinementSuggestions()).isEmpty();
        assertThat(response.diagnosisGroups().getFirst().results())
                .extracting("code")
                .containsExactly("M25.562", "M25.561");
        assertThat(response.diagnosisGroups().getFirst().results().getFirst().matchPercentage()).isLessThan(100);
        assertThat(response.diagnosisGroups().getFirst().results().getFirst().matchReason()).contains("fallback term \"left knee pain\"");
        server.verify();
    }

    @Test
    void vaguePainStillRequestsSpecificityAndSuggestions() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Icd10Service service = new Icd10Service(builder, "https://example.test/icd10");

        server.expect(once(), requestWithTerm("knee pain"))
                .andRespond(withSuccess(nlmRows(row("M25.569", "Pain in unspecified knee")), MediaType.APPLICATION_JSON));

        Icd10SearchResponse response = service.search(new Icd10SearchRequest(
                "knee pain",
                1,
                true,
                false));

        assertThat(response.diagnosisGroups().getFirst().needsMoreInformation()).isTrue();
        assertThat(response.diagnosisGroups().getFirst().clarifyingQuestions())
                .contains("Is laterality left, right, bilateral, or unspecified?");
        assertThat(response.diagnosisGroups().getFirst().refinementSuggestions())
                .contains("knee pain left", "chronic knee pain");
        server.verify();
    }

    @Test
    void savePersistsIcd10SearchJson() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AtomicReference<SavedIcd10Search> savedSearch = new AtomicReference<>();
        SavedIcd10SearchRepository repository = repositoryProxy(savedSearch);
        PayloadEncryptionService encryptionService = new PayloadEncryptionService("local-dev-key-change-me", "v1");
        Icd10Service service = new Icd10Service(
                builder,
                repository,
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules(),
                encryptionService,
                new PhiScannerService(),
                null,
                "https://example.test/icd10");

        server.expect(once(), requestWithTerm("hypertension"))
                .andRespond(withSuccess(nlmRows(row("I10", "Essential (primary) hypertension")), MediaType.APPLICATION_JSON));

        String id = service.save(new Icd10SearchRequest("hypertension", 1, true, false)).id();

        assertThat(savedSearch.get()).isNotNull();
        assertThat(savedSearch.get().getId()).isEqualTo(UUID.fromString(id));
        assertThat(savedSearch.get().getSearchJson()).startsWith("aesgcm:v1:");
        assertThat(encryptionService.decrypt(savedSearch.get().getSearchJson())).contains("Essential (primary) hypertension");
        server.verify();
    }

    @SuppressWarnings("unchecked")
    private static SavedIcd10SearchRepository repositoryProxy(AtomicReference<SavedIcd10Search> savedSearch) {
        return (SavedIcd10SearchRepository) Proxy.newProxyInstance(
                SavedIcd10SearchRepository.class.getClassLoader(),
                new Class<?>[]{SavedIcd10SearchRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        savedSearch.set((SavedIcd10Search) args[0]);
                        yield args[0];
                    }
                    case "findById" -> Optional.ofNullable(savedSearch.get());
                    case "findAll", "findByExpiresAtBefore" -> java.util.List.of();
                    case "deleteById", "deleteAll" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static RequestMatcher requestWithTerm(String expectedTerm) {
        return request -> {
            String decoded = URLDecoder.decode(request.getURI().getQuery(), StandardCharsets.UTF_8);
            assertThat(request.getURI().getHost()).isEqualTo("example.test");
            assertThat(decoded).contains("terms=" + expectedTerm);
        };
    }

    private static String row(String code, String description) {
        return "[\"" + code + "\",\"" + description + "\"]";
    }

    private static String nlmRows(String... rows) {
        return "[0,[],null,[" + String.join(",", rows) + "]]";
    }
}
