package com.hl7decoder.service;

import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.Hl7Field;
import com.hl7decoder.model.Hl7ParseResult;
import com.hl7decoder.model.Hl7Segment;
import com.hl7decoder.model.ValidationMode;
import com.hl7decoder.model.platform.FhirConversionResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FhirConversionService {
    private static final DateTimeFormatter HL7_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Hl7Service hl7Service;

    public FhirConversionService(Hl7Service hl7Service) {
        this.hl7Service = hl7Service;
    }

    public FhirConversionResponse hl7ToFhir(String message) {
        Hl7ParseResult parsed = hl7Service.parseAndValidate(new Hl7Request(message, ValidationMode.STANDARD));
        List<Map<String, Object>> entries = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        String patientId = "patient-" + safe(parsed.metadata().controlId(), "unknown");

        segment(parsed, "PID").ifPresent(pid -> {
            entries.add(entry(patientResource(patientId, value(pid, 3), value(pid, 5), value(pid, 7), value(pid, 8))));
            notes.add("Mapped PID to Patient.");
        });
        segment(parsed, "PV1").ifPresent(pv1 -> {
            entries.add(entry(resource("Encounter", "encounter-" + safe(parsed.metadata().controlId(), "unknown"),
                    Map.of("status", "finished", "class", Map.of("code", safe(value(pv1, 2), "O")), "subject", reference(patientId)))));
            notes.add("Mapped PV1 to Encounter.");
        });
        for (Hl7Segment obx : parsed.segments().stream().filter(segment -> "OBX".equals(segment.name())).toList()) {
            entries.add(entry(observationResource(obx, patientId)));
            notes.add("Mapped OBX-" + obx.index() + " to Observation.");
        }
        segment(parsed, "OBR").ifPresent(obr -> {
            entries.add(entry(resource("DiagnosticReport", "diagnostic-report-" + safe(parsed.metadata().controlId(), "unknown"),
                    Map.of("status", "final", "code", codeable(value(obr, 4)), "subject", reference(patientId)))));
            notes.add("Mapped OBR to DiagnosticReport.");
        });

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("resourceType", "Bundle");
        bundle.put("type", "collection");
        bundle.put("timestamp", Instant.now().toString());
        bundle.put("entry", entries);
        return new FhirConversionResponse("HL7v2", "FHIR R4-like JSON", bundle, notes);
    }

    public Map<String, Object> fhirToHl7(String fhirJson) {
        Map<String, Object> root = readJson(fhirJson);
        List<Map<String, Object>> resources = resources(root);
        Map<String, Object> patient = firstResource(resources, "Patient");
        Map<String, Object> encounter = firstResource(resources, "Encounter");
        List<Map<String, Object>> observations = resources.stream()
                .filter(resource -> "Observation".equals(resource.get("resourceType")))
                .toList();

        String controlId = "FHIR" + System.currentTimeMillis();
        List<String> rows = new ArrayList<>();
        rows.add("MSH|^~\\&|FHIR|HEALTHCAREHERO|HL7|LOCAL|" + LocalDateTime.now().format(HL7_TS) + "||ORU^R01|" + controlId + "|P|2.5.1");
        rows.add(patientToPid(patient));
        rows.add(encounterToPv1(encounter));
        rows.add("OBR|1||" + controlId + "|FHIR^FHIR Converted Result");

        int obxIndex = 1;
        for (Map<String, Object> observation : observations) {
            rows.add(observationToObx(observation, obxIndex++));
        }
        if (observations.isEmpty()) {
            rows.add("OBX|1|ST|FHIR^FHIR Payload||Converted FHIR bundle contained no Observation resources||||||F");
        }

        return Map.of(
                "sourceType", "FHIR R4-like JSON",
                "targetType", "HL7v2 ORU^R01",
                "message", String.join("\r", rows),
                "mappingNotes", List.of(
                        "Mapped Patient to PID.",
                        "Mapped Encounter to PV1 when present.",
                        "Mapped Observation resources to OBX segments."
                )
        );
    }

    private Map<String, Object> patientResource(String id, String identifier, String name, String birthDate, String sex) {
        Map<String, Object> patient = resource("Patient", id, new LinkedHashMap<>());
        patient.put("identifier", List.of(Map.of("value", safe(identifier, id))));
        patient.put("name", List.of(Map.of("text", safe(name, "Unknown").replace('^', ' '))));
        if (birthDate != null && birthDate.length() >= 8) {
            patient.put("birthDate", birthDate.substring(0, 4) + "-" + birthDate.substring(4, 6) + "-" + birthDate.substring(6, 8));
        }
        patient.put("gender", switch (safe(sex, "unknown").toUpperCase()) {
            case "M" -> "male";
            case "F" -> "female";
            default -> "unknown";
        });
        return patient;
    }

    private Map<String, Object> observationResource(Hl7Segment obx, String patientId) {
        Map<String, Object> observation = resource("Observation", "observation-" + obx.index(), new LinkedHashMap<>());
        observation.put("status", "final");
        observation.put("code", codeable(value(obx, 3)));
        observation.put("subject", reference(patientId));
        observation.put("valueString", safe(value(obx, 5), ""));
        observation.put("interpretation", List.of(Map.of("text", safe(value(obx, 8), ""))));
        return observation;
    }

    private Map<String, Object> resource(String type, String id, Map<String, Object> values) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("resourceType", type);
        resource.put("id", id);
        resource.putAll(values);
        return resource;
    }

    private Map<String, Object> entry(Map<String, Object> resource) {
        return Map.of("resource", resource);
    }

    private Map<String, Object> reference(String id) {
        return Map.of("reference", "Patient/" + id);
    }

    private Map<String, Object> codeable(String value) {
        return Map.of("text", safe(value, "Unknown").replace('^', ' '));
    }

    private java.util.Optional<Hl7Segment> segment(Hl7ParseResult parsed, String name) {
        return parsed.segments().stream().filter(segment -> name.equals(segment.name())).findFirst();
    }

    private String value(Hl7Segment segment, int index) {
        return segment.fields().stream().filter(field -> field.index() == index).findFirst().map(Hl7Field::value).orElse(null);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("FHIR JSON could not be parsed.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resources(Map<String, Object> root) {
        if (root == null) {
            return List.of();
        }
        if ("Bundle".equals(root.get("resourceType")) && root.get("entry") instanceof List<?> entries) {
            List<Map<String, Object>> resources = new ArrayList<>();
            for (Object entry : entries) {
                if (entry instanceof Map<?, ?> entryMap && entryMap.get("resource") instanceof Map<?, ?> resource) {
                    resources.add((Map<String, Object>) resource);
                }
            }
            return resources;
        }
        return List.of(root);
    }

    private Map<String, Object> firstResource(List<Map<String, Object>> resources, String type) {
        return resources.stream()
                .filter(resource -> type.equals(resource.get("resourceType")))
                .findFirst()
                .orElse(Map.of());
    }

    @SuppressWarnings("unchecked")
    private String patientToPid(Map<String, Object> patient) {
        String id = string(patient.get("id"), "FHIRPATIENT");
        if (patient.get("identifier") instanceof List<?> identifiers && !identifiers.isEmpty() && identifiers.getFirst() instanceof Map<?, ?> identifier) {
            id = string(identifier.get("value"), id);
        }
        String name = "UNKNOWN^PATIENT";
        if (patient.get("name") instanceof List<?> names && !names.isEmpty() && names.getFirst() instanceof Map<?, ?> nameMap) {
            String family = string(nameMap.get("family"), "UNKNOWN");
            String given = "PATIENT";
            if (nameMap.get("given") instanceof List<?> givenNames && !givenNames.isEmpty()) {
                given = string(givenNames.getFirst(), given);
            } else {
                given = string(nameMap.get("text"), given).replace(' ', '^');
            }
            name = family + "^" + given;
        }
        String birthDate = string(patient.get("birthDate"), "").replace("-", "");
        String sex = switch (string(patient.get("gender"), "unknown")) {
            case "male" -> "M";
            case "female" -> "F";
            default -> "U";
        };
        return "PID|1||" + id + "^^^FHIR^MR||" + name + "||" + birthDate + "|" + sex;
    }

    private String encounterToPv1(Map<String, Object> encounter) {
        String code = "O";
        if (encounter.get("class") instanceof Map<?, ?> classMap) {
            code = string(classMap.get("code"), code);
        }
        return "PV1|1|" + code;
    }

    private String observationToObx(Map<String, Object> observation, int index) {
        String code = codeText(observation.get("code"));
        String value = string(observation.get("valueString"), null);
        if (value == null && observation.get("valueQuantity") instanceof Map<?, ?> quantity) {
            value = string(quantity.get("value"), "") + " " + string(quantity.get("unit"), "");
        }
        return "OBX|" + index + "|ST|" + code + "||" + safe(value, "") + "||||||F";
    }

    private String codeText(Object code) {
        if (code instanceof Map<?, ?> map) {
            return string(map.get("text"), "FHIR^FHIR Observation").replace(' ', '^');
        }
        return "FHIR^FHIR Observation";
    }

    private String string(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }
}
