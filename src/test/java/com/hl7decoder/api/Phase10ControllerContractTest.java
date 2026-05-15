package com.hl7decoder.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hl7decoder.config.ApiExceptionHandler;
import com.hl7decoder.config.ErrorTrackingService;
import com.hl7decoder.api.dto.auth.RegisterRequest;
import com.hl7decoder.model.auth.AuthResponse;
import com.hl7decoder.model.auth.UserRole;
import com.hl7decoder.service.AuthService;
import com.hl7decoder.service.CptSearchService;
import com.hl7decoder.service.IcdCptCompatibilityService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Phase10ControllerContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void authRegisterContractReturnsTokenAndCapabilities() throws Exception {
        MockMvc mockMvc = mockMvc(new AuthController(new StubAuthService(), null));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"coder@example.com\",\"password\":\"secret123\",\"displayName\":\"Coder\",\"organizationName\":\"Clinic\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.user.email").value("coder@example.com"))
                .andExpect(jsonPath("$.capabilities[0]").value("local_login"));
    }

    @Test
    void cptSearchContractReturnsLicensingAndResults() throws Exception {
        MockMvc mockMvc = mockMvc(new CptSearchController(new CptSearchService(null, null)));

        mockMvc.perform(get("/api/cpt/search?q=chest%20x-ray%202%20views&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensingNotice", containsString("CPT")))
                .andExpect(jsonPath("$.results[0].code", containsString("7104")));
    }

    @Test
    void codingCompatibilityContractReturnsReviewStatus() throws Exception {
        MockMvc mockMvc = mockMvc(new CodingController(new IcdCptCompatibilityService(new CptSearchService(null, null))));

        mockMvc.perform(post("/api/coding/compatibility")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosisText\":\"cough\",\"icd10Code\":\"R05.9\",\"procedureText\":\"chest x-ray 2 views\",\"procedureCode\":\"71046\",\"payer\":\"Medicare\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUPPORTED"))
                .andExpect(jsonPath("$.recommendations[0]", containsString("Medicare")));
    }

    private MockMvc mockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler(new ErrorTrackingService()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private static final class StubAuthService extends AuthService {
        private StubAuthService() {
            super(null, null, null, null, 5, 15, 30);
        }

        @Override
        public AuthResponse register(RegisterRequest request) {
            return new AuthResponse(
                    "token",
                    Instant.parse("2026-05-14T16:00:00Z"),
                    new AuthResponse.AuthUser(
                            UUID.fromString("00000000-0000-0000-0000-000000000001"),
                            request.email(),
                            request.displayName(),
                            UUID.fromString("00000000-0000-0000-0000-000000000002"),
                            request.organizationName(),
                            List.of(UserRole.ORGANIZATION_ADMIN, UserRole.CODER)
                    ),
                    List.of("local_login", "api_keys")
            );
        }
    }
}
