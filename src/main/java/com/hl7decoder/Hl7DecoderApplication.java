package com.hl7decoder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.hl7decoder.config.FeatureFlagsProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(FeatureFlagsProperties.class)
@SpringBootApplication
public class Hl7DecoderApplication {
    public static void main(String[] args) {
        SpringApplication.run(Hl7DecoderApplication.class, args);
    }
}
