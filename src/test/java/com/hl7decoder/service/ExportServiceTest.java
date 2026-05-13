package com.hl7decoder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hl7decoder.api.dto.ExportRequest;
import com.hl7decoder.model.ValidationMode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ExportServiceTest {
    private final ExportService service = new ExportService(new Hl7Service(), new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    void exportsParsedHl7ResultAsXml() {
        String message = """
                MSH|^~\\&|LAB|HOSP|EHR|CLINIC|20260101123000||ORU^R01|MSG00001|P|2.5.1
                PID|1||12345^^^HOSP^MR||DOE^JANE
                OBR|1||ORD001|CBC^Complete Blood Count
                OBX|1|NM|WBC^White Blood Cells||7.0|10*3/uL|||||F
                """;

        String xml = new String(service.exportXml(new ExportRequest(message, ValidationMode.STANDARD)), StandardCharsets.UTF_8);

        assertThat(xml).contains("<Hl7ParseResult>");
        assertThat(xml).contains("<hl7Version>2.5.1</hl7Version>");
        assertThat(xml).contains("<normalizedMessage>");
    }
}
