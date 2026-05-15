package com.hl7decoder.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI hl7OpenApi() {
        return new OpenAPI()
                .addServersItem(new Server().url("/api/v1").description("Stable API prefix"))
                .addServersItem(new Server().url("/api").description("Legacy API prefix"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addExamples("hl7ParseRequest", new Example().summary("HL7 parse").value(java.util.Map.of(
                                "message", "MSH|^~\\\\&|LAB|HOSP|EHR|CLINIC|20260101123000||ORU^R01|MSG00001|P|2.5.1",
                                "mode", "STANDARD")))
                        .addExamples("textToolRequest", new Example().summary("Text tool").value(java.util.Map.of("text", "Left knee MRI denied for medical necessity.")))
                        .addExamples("aiAssistRequest", new Example().summary("PHI-safe AI assist").value(java.util.Map.of(
                                "promptType", "DENIAL_ROOT_CAUSE",
                                "inputText", "Denied for medical necessity after MRI request.",
                                "redactPhi", true,
                                "requireHumanApproval", true))))
                .info(new Info()
                .title("Healthcare Hero API")
                .version("0.1.0")
                .description("REST API for HL7, FHIR, X12, coding, workspace, AI-assist, and operations workflows. Endpoints are available under /api/v1 with legacy /api routes retained."));
    }
}
