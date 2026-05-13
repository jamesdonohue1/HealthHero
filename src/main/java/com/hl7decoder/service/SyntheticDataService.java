package com.hl7decoder.service;

import com.hl7decoder.api.dto.platform.SyntheticDataRequest;
import com.hl7decoder.model.platform.SyntheticDataResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SyntheticDataService {
    private static final DateTimeFormatter HL7_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter HL7_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final List<String> FIRST_NAMES = List.of("Jane", "Marcus", "Elena", "Noah", "Priya", "Sam", "Ava", "Miles");
    private static final List<String> LAST_NAMES = List.of("Doe", "Carter", "Nguyen", "Patel", "Stone", "Rivera", "Lee", "Morgan");
    private static final List<String> DIAGNOSES = List.of("M25.562", "R07.9", "E11.9", "I10", "J45.909", "N18.31");

    public SyntheticDataResponse generate(SyntheticDataRequest request) {
        int count = clamp(request.count() == null ? 3 : request.count(), 1, 25);
        int minAge = clamp(request.minAge() == null ? 18 : request.minAge(), 0, 120);
        int maxAge = clamp(request.maxAge() == null ? 90 : request.maxAge(), minAge, 120);
        String diagnosis = request.diagnosis() == null || request.diagnosis().isBlank()
                ? DIAGNOSES.get(ThreadLocalRandom.current().nextInt(DIAGNOSES.size()))
                : request.diagnosis().trim();

        List<String> hl7Messages = new ArrayList<>();
        List<Map<String, Object>> fhirBundles = new ArrayList<>();
        List<String> x12Claims = new ArrayList<>();
        List<String> patients = new ArrayList<>();

        for (int index = 1; index <= count; index++) {
            PatientSeed patient = patient(index, minAge, maxAge);
            String controlId = "HH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + String.format("%04d", index);
            hl7Messages.add(hl7Message(patient, diagnosis, controlId));
            fhirBundles.add(fhirBundle(patient, diagnosis, controlId));
            x12Claims.add(x12Claim(patient, diagnosis, controlId));
            patients.add(patient.id() + " " + patient.lastName() + ", " + patient.firstName() + " age " + patient.age() + " diagnosis " + diagnosis);
        }

        return new SyntheticDataResponse(hl7Messages, fhirBundles, x12Claims, patients);
    }

    private PatientSeed patient(int index, int minAge, int maxAge) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String first = FIRST_NAMES.get((index - 1) % FIRST_NAMES.size());
        String last = LAST_NAMES.get((index - 1) % LAST_NAMES.size());
        int age = random.nextInt(minAge, maxAge + 1);
        LocalDate birthDate = LocalDate.now().minusYears(age).minusDays(random.nextInt(0, 365));
        String sex = index % 2 == 0 ? "M" : "F";
        return new PatientSeed("SYN" + String.format("%06d", index), first, last, birthDate, age, sex);
    }

    private String hl7Message(PatientSeed patient, String diagnosis, String controlId) {
        String now = LocalDateTime.now().format(HL7_TS);
        return String.join("\r",
                "MSH|^~\\&|HEALTHCAREHERO|SYNTHETIC|EHR|TEST|" + now + "||ORU^R01|" + controlId + "|P|2.5.1",
                "PID|1||" + patient.id() + "^^^HH^MR||" + patient.lastName() + "^" + patient.firstName() + "||" + patient.birthDate().format(HL7_DATE) + "|" + patient.sex(),
                "PV1|1|O",
                "OBR|1||ORD" + controlId + "|LAB^Synthetic Lab Panel|||"+ now,
                "OBX|1|ST|DX^Diagnosis||" + diagnosis + "||||||F");
    }

    private Map<String, Object> fhirBundle(PatientSeed patient, String diagnosis, String controlId) {
        Map<String, Object> patientResource = new LinkedHashMap<>();
        patientResource.put("resourceType", "Patient");
        patientResource.put("id", patient.id());
        patientResource.put("identifier", List.of(Map.of("value", patient.id())));
        patientResource.put("name", List.of(Map.of("family", patient.lastName(), "given", List.of(patient.firstName()))));
        patientResource.put("birthDate", patient.birthDate().toString());
        patientResource.put("gender", "M".equals(patient.sex()) ? "male" : "female");

        Map<String, Object> condition = new LinkedHashMap<>();
        condition.put("resourceType", "Condition");
        condition.put("id", "condition-" + controlId);
        condition.put("subject", Map.of("reference", "Patient/" + patient.id()));
        condition.put("code", Map.of("coding", List.of(Map.of("system", "http://hl7.org/fhir/sid/icd-10-cm", "code", diagnosis))));

        return Map.of(
                "resourceType", "Bundle",
                "type", "collection",
                "entry", List.of(Map.of("resource", patientResource), Map.of("resource", condition))
        );
    }

    private String x12Claim(PatientSeed patient, String diagnosis, String controlId) {
        return "ISA*00*          *00*          *ZZ*HEALTHHERO    *ZZ*PAYER          *260101*1230*^*00501*" + controlId.substring(Math.max(0, controlId.length() - 9)) + "*0*T*:~"
                + "GS*HC*HEALTHHERO*PAYER*20260101*1230*1*X*005010X222A1~"
                + "ST*837*0001*005010X222A1~"
                + "BHT*0019*00*" + controlId + "*20260101*1230*CH~"
                + "NM1*IL*1*" + patient.lastName() + "*" + patient.firstName() + "****MI*" + patient.id() + "~"
                + "CLM*" + controlId + "*125.00***11:B:1*Y*A*Y*I~"
                + "HI*ABK:" + diagnosis + "~"
                + "SV1*HC:99213*125.00*UN*1***1~"
                + "SE*8*0001~GE*1*1~IEA*1*" + controlId.substring(Math.max(0, controlId.length() - 9)) + "~";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record PatientSeed(String id, String firstName, String lastName, LocalDate birthDate, int age, String sex) {
    }
}
