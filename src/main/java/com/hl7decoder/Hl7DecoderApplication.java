package com.hl7decoder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Hl7DecoderApplication {
    public static void main(String[] args) {
        SpringApplication.run(Hl7DecoderApplication.class, args);
    }
}
