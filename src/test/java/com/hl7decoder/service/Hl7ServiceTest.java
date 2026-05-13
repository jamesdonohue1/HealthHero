package com.hl7decoder.service;

import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.Hl7ParseResult;
import com.hl7decoder.model.ValidationMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Hl7ServiceTest {
    private final Hl7Service service = new Hl7Service();

    @Test
    void parsesMetadataAndZSegments() {
        String message = """
                MSH|^~\\&|LAB|HOSP|EHR|CLINIC|20260101123000||ORU^R01|MSG00001|P|2.5.1
                PID|1||12345^^^HOSP^MR||DOE^JANE
                OBR|1||ORD001|CBC^Complete Blood Count
                OBX|1|NM|WBC^White Blood Cells||7.0|10*3/uL|||||F
                ZVN|alpha^beta|custom~repeat
                """;

        Hl7ParseResult result = service.parseAndValidate(new Hl7Request(message, ValidationMode.STANDARD));

        assertThat(result.metadata().hl7Version()).isEqualTo("2.5.1");
        assertThat(result.metadata().messageType()).isEqualTo("ORU");
        assertThat(result.segments()).anyMatch(segment -> segment.name().equals("ZVN") && segment.custom());
        assertThat(result.summary().valid()).isTrue();
    }

    @Test
    void reportsMissingRequiredSegments() {
        String message = "MSH|^~\\&|LAB|HOSP|EHR|CLINIC|20260101123000||ORU^R01|MSG00001|P|2.5.1";

        Hl7ParseResult result = service.parseAndValidate(new Hl7Request(message, ValidationMode.STRICT));

        assertThat(result.summary().errors()).isGreaterThan(0);
        assertThat(result.issues()).anyMatch(issue -> issue.description().contains("Missing required PID"));
    }

    @Test
    void supportsHl7Version23AndLater() {
        String message = """
                MSH|^~\\&|ADTAPP|HOSP|EHR|CLINIC|20260101123000||ADT^A01|MSG00002|P|2.3
                EVN|A01|20260101123000
                PID|1||12345^^^HOSP^MR||DOE^JANE
                PV1|1|I
                """;

        Hl7ParseResult result = service.parseAndValidate(new Hl7Request(message, ValidationMode.STRICT));

        assertThat(result.metadata().hl7Version()).isEqualTo("2.3");
        assertThat(result.issues()).noneMatch(issue -> issue.description().contains("Unsupported HL7 version"));
    }

    @Test
    void rejectsVersionsBefore23() {
        String message = "MSH|^~\\&|ADTAPP|HOSP|EHR|CLINIC|20260101123000||ADT^A01|MSG00003|P|2.2";

        Hl7ParseResult result = service.parseAndValidate(new Hl7Request(message, ValidationMode.STRICT));

        assertThat(result.issues()).anyMatch(issue -> issue.description().contains("Unsupported HL7 version 2.2"));
    }
}
