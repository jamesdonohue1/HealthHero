package com.hl7decoder.service;

import com.hl7decoder.api.dto.cpt.IcdCptCompatibilityRequest;
import com.hl7decoder.api.dto.cpt.ProcedureSearchRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CptSearchServiceTest {
    @Test
    void searchesProcedureByLayTermsAndSynonyms() {
        CptSearchService service = new CptSearchService();

        var response = service.search(new ProcedureSearchRequest("chest x-ray 2 views", 10, null));

        assertThat(response.results()).isNotEmpty();
        assertThat(response.results().getFirst().code()).isEqualTo("71046");
        assertThat(response.licensingNotice()).contains("AMA");
    }

    @Test
    void searchesProcedureByCodeRange() {
        CptSearchService service = new CptSearchService();

        var response = service.search(new ProcedureSearchRequest("71045-71046", 10, null));

        assertThat(response.results()).extracting("code").contains("71045", "71046");
    }

    @Test
    void checksSupportedIcdCptPair() {
        CptSearchService cptSearchService = new CptSearchService();
        IcdCptCompatibilityService service = new IcdCptCompatibilityService(cptSearchService);

        var response = service.check(new IcdCptCompatibilityRequest("cough", "R05.9", "chest x-ray 2 views", "71046", "Medicare"));

        assertThat(response.status()).isEqualTo("SUPPORTED");
        assertThat(response.confidence()).isGreaterThan(0.8);
    }

    @Test
    void flagsLikelyDenialForUnsupportedPair() {
        CptSearchService cptSearchService = new CptSearchService();
        IcdCptCompatibilityService service = new IcdCptCompatibilityService(cptSearchService);

        var response = service.check(new IcdCptCompatibilityRequest("knee pain", "M25.562", "chest x-ray 2 views", "71046", "Medicare"));

        assertThat(response.status()).isEqualTo("LIKELY_DENIAL");
        assertThat(response.warnings()).isNotEmpty();
    }
}
