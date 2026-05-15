package com.hl7decoder.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hl7decoder.config.ApiExceptionHandler;
import com.hl7decoder.model.icd10.Icd10DiagnosisGroup;
import com.hl7decoder.model.icd10.Icd10SearchResponse;
import com.hl7decoder.model.icd10.Icd10SearchResult;
import com.hl7decoder.service.Icd10Service;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Icd10ControllerTest {
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new Icd10Controller(new StubIcd10Service()))
            .setControllerAdvice(new ApiExceptionHandler(new com.hl7decoder.config.ErrorTrackingService()))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()))
            .build();

    @Test
    void searchResponseIncludesMatchAndAuditFields() throws Exception {
        mockMvc.perform(post("/api/icd10/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputText\":\"chronic left knee pain\",\"resultLimit\":5,\"includeClarifyingQuestions\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosisGroups[0].queryTerms[0]").value("left knee pain"))
                .andExpect(jsonPath("$.diagnosisGroups[0].fallbackMatchCount").value(1))
                .andExpect(jsonPath("$.diagnosisGroups[0].results[0].matchPercentage").value(90))
                .andExpect(jsonPath("$.diagnosisGroups[0].results[0].queryTerm").value("left knee pain"))
                .andExpect(jsonPath("$.diagnosisGroups[0].results[0].fallbackMatch").value(true))
                .andExpect(jsonPath("$.diagnosisGroups[0].results[0].source").value(containsString("NLM")));
    }

    private static class StubIcd10Service extends Icd10Service {
        StubIcd10Service() {
            super(RestClient.builder(), null, new ObjectMapper().findAndRegisterModules(), "https://example.test/icd10");
        }

        @Override
        public Icd10SearchResponse search(com.hl7decoder.api.dto.icd10.Icd10SearchRequest request) {
            return new Icd10SearchResponse(
                    "chronic left knee pain",
                    "chronic left knee pain",
                    Instant.parse("2026-05-13T00:00:00Z"),
                    "disclaimer",
                    List.of(new Icd10DiagnosisGroup(
                            "chronic left knee pain",
                            false,
                            List.of(),
                            List.of(),
                            List.of("left knee pain"),
                            0,
                            1,
                            List.of(new Icd10SearchResult(
                                    "M25.562",
                                    "Pain in left knee",
                                    "Pain in left knee",
                                    1,
                                    0.9,
                                    90,
                                    true,
                                    "Diseases of the musculoskeletal system and connective tissue",
                                    "Matched fallback",
                                    "left knee pain",
                                    true,
                                    "NLM Clinical Tables ICD-10-CM")))));
        }
    }
}
