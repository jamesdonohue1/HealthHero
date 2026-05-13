package com.hl7decoder.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI hl7OpenApi() {
        return new OpenAPI().info(new Info()
                .title("Healthcare Hero API")
                .version("0.1.0")
                .description("REST API for HL7 parsing, validation, decoding, exports, and anonymous saved validations."));
    }
}
