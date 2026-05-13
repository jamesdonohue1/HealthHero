package com.hl7decoder.service;

import com.hl7decoder.api.dto.platform.MedicalNecessityRequest;
import com.hl7decoder.api.dto.platform.SyntheticDataRequest;
import com.hl7decoder.model.platform.FhirConversionResponse;
import com.hl7decoder.model.platform.MedicalNecessityResponse;
import com.hl7decoder.model.platform.SyntheticDataResponse;
import com.hl7decoder.model.platform.X12DecodeResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformServicesTest {
    private final Hl7Service hl7Service = new Hl7Service();

    @Test
    void repairsMissingHl7Segments() {
        Hl7RepairService service = new Hl7RepairService(hl7Service);

        var response = service.repair(new com.hl7decoder.api.dto.Hl7Request(
                "MSH|^~\\&|LAB|HOSP|EHR|CLINIC|20260101123000||ORU^R01|MSG00001|P|2.5.1",
                com.hl7decoder.model.ValidationMode.STRICT
        ));

        assertThat(response.changed()).isTrue();
        assertThat(response.repairedMessage()).contains("\rPID|");
        assertThat(response.repairedMessage()).contains("\rOBR|");
        assertThat(response.repairedMessage()).contains("\rOBX|");
    }

    @Test
    void convertsHl7ToFhirBundle() {
        FhirConversionService service = new FhirConversionService(hl7Service);
        String message = """
                MSH|^~\\&|LAB|HOSP|EHR|CLINIC|20260101123000||ORU^R01|MSG00001|P|2.5.1
                PID|1||12345^^^HOSP^MR||DOE^JANE||19800101|F
                OBR|1||ORD001|CBC^Complete Blood Count
                OBX|1|NM|WBC^White Blood Cells||7.0|10*3/uL|||||F
                """;

        FhirConversionResponse response = service.hl7ToFhir(message);

        assertThat(response.bundle()).containsEntry("resourceType", "Bundle");
        assertThat(response.mappingNotes()).anyMatch(note -> note.contains("PID"));
        assertThat(response.mappingNotes()).anyMatch(note -> note.contains("OBX"));
    }

    @Test
    void generatesSyntheticHealthcarePayloads() {
        SyntheticDataService service = new SyntheticDataService();

        SyntheticDataResponse response = service.generate(new SyntheticDataRequest(2, 30, 40, "M25.562"));

        assertThat(response.hl7Messages()).hasSize(2);
        assertThat(response.fhirBundles()).hasSize(2);
        assertThat(response.x12Claims()).hasSize(2);
        assertThat(response.patients()).allMatch(patient -> patient.contains("M25.562"));
    }

    @Test
    void decodesX12Transaction() {
        X12Service service = new X12Service();
        String x12 = "ISA*00*          *00*          *ZZ*SENDER*ZZ*RECEIVER*260101*1230*^*00501*000000001*0*T*:~"
                + "GS*HC*SENDER*RECEIVER*20260101*1230*1*X*005010X222A1~"
                + "ST*837*0001*005010X222A1~CLM*ABC123*125.00***11:B:1*Y*A*Y*I~SE*3*0001~";

        X12DecodeResponse response = service.decode(x12);

        assertThat(response.transactionType()).isEqualTo("837 Healthcare Claim");
        assertThat(response.segments()).anyMatch(segment -> "CLM".equals(segment.segmentId()) && "2300 Claim".equals(segment.loop()));
    }

    @Test
    void checksMedicalNecessityRules() {
        MedicalNecessityService service = new MedicalNecessityService();

        MedicalNecessityResponse response = service.check(new MedicalNecessityRequest("83036", List.of("E11.9"), "Medicare"));

        assertThat(response.likelyCovered()).isTrue();
        assertThat(response.riskLevel()).isEqualTo("LOW");
        assertThat(response.matchedRules()).isNotEmpty();
    }

    @Test
    void convertsFhirToHl7Message() {
        FhirConversionService service = new FhirConversionService(hl7Service);
        String fhir = """
                {
                  "resourceType": "Bundle",
                  "entry": [
                    {"resource": {"resourceType": "Patient", "id": "P1", "identifier": [{"value": "123"}], "name": [{"family": "Doe", "given": ["Jane"]}], "birthDate": "1980-01-01", "gender": "female"}},
                    {"resource": {"resourceType": "Observation", "code": {"text": "White Blood Cells"}, "valueString": "7.0"}}
                  ]
                }
                """;

        @SuppressWarnings("unchecked")
        var response = service.fhirToHl7(fhir);

        assertThat(response.get("message").toString()).contains("MSH|^~\\&");
        assertThat(response.get("message").toString()).contains("\rPID|1||123^^^FHIR^MR||Doe^Jane||19800101|F");
        assertThat(response.get("message").toString()).contains("\rOBX|1|ST|White^Blood^Cells||7.0");
    }

    @Test
    void profileValidationAddsAdvancedIssues() {
        PlatformRoadmapService service = new PlatformRoadmapService(hl7Service);

        var response = service.profileValidate("""
                MSH|^~\\&|LAB|HOSP|EHR|CLINIC|BADDATE||ACK^A01|MSG00001|P|2.5.1
                """, com.hl7decoder.model.ValidationMode.STANDARD);

        assertThat(response.get("advancedIssues").toString()).contains("ACK message is missing MSA");
        assertThat(response.get("repairSuggestions").toString()).contains("MSA");
    }

    @Test
    void roadmapAnalyzersReturnActionableOutputs() {
        PlatformRoadmapService service = new PlatformRoadmapService(hl7Service);

        assertThat(service.priorAuth("MRI knee urgent").get("missingInfo").toString()).contains("Conservative therapy");
        assertThat(service.denial("denied for medical necessity").get("rootCauses").toString()).contains("Medical necessity");
        assertThat(service.cdi("knee pain").get("specificityPrompts").toString()).contains("laterality");
        assertThat(service.terminology("left knee pain").get("candidates").toString()).contains("M25.56");
        assertThat(service.compliance("DOE^JANE 123-45-6789").get("phiDetected")).isEqualTo(true);
        assertThat(service.eligibility("ABC123").get("generated270").toString()).contains("ABC123");
    }
}
