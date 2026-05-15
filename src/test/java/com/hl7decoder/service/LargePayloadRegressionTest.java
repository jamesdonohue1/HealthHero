package com.hl7decoder.service;

import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.ValidationMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LargePayloadRegressionTest {
    @Test
    void parsesLargeHl7PayloadWithoutDroppingSegments() {
        StringBuilder message = new StringBuilder("MSH|^~\\&|LAB|HOSP|EHR|CLINIC|20260101123000||ORU^R01|MSG00001|P|2.5.1\r");
        message.append("PID|1||12345^^^HOSP^MR||DOE^JANE||19800101|F\r");
        for (int i = 1; i <= 250; i++) {
            message.append("OBX|").append(i).append("|NM|WBC^White Blood Cells||7.0|10*3/uL|||||F\r");
        }

        var result = new Hl7Service().parseAndValidate(new Hl7Request(message.toString(), ValidationMode.STANDARD));

        assertThat(result.segments()).hasSizeGreaterThan(240);
        assertThat(result.normalizedMessage()).contains("OBX|250");
    }

    @Test
    void decodesLargeX12PayloadWithoutLosingClaimSegments() {
        StringBuilder x12 = new StringBuilder("ISA*00*          *00*          *ZZ*SENDER*ZZ*RECEIVER*260101*1230*^*00501*000000001*0*T*:~GS*HC*SENDER*RECEIVER*20260101*1230*1*X*005010X222A1~ST*837*0001*005010X222A1~");
        for (int i = 0; i < 120; i++) {
            x12.append("CLM*ABC").append(i).append("*125.00***11:B:1*Y*A*Y*I~");
        }
        x12.append("SE*123*0001~");

        var response = new X12Service().decode(x12.toString());

        assertThat(response.segments()).filteredOn(segment -> "CLM".equals(segment.segmentId())).hasSize(120);
    }
}
