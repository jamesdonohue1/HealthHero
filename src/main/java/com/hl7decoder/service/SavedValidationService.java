package com.hl7decoder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.Hl7ParseResult;
import com.hl7decoder.model.SavedValidationResponse;
import com.hl7decoder.persistence.SavedValidation;
import com.hl7decoder.persistence.SavedValidationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class SavedValidationService {
    private final SavedValidationRepository repository;
    private final Hl7Service hl7Service;
    private final ObjectMapper objectMapper;
    private final String encryptionKey;

    public SavedValidationService(SavedValidationRepository repository, Hl7Service hl7Service, ObjectMapper objectMapper,
                                  @Value("${app.encryption-key:local-dev-key-change-me}") String encryptionKey) {
        this.repository = repository;
        this.hl7Service = hl7Service;
        this.objectMapper = objectMapper;
        this.encryptionKey = encryptionKey;
    }

    public SavedValidationResponse saveAnonymous(Hl7Request request) {
        Hl7ParseResult result = hl7Service.parseAndValidate(request);
        Instant created = Instant.now();
        Instant expires = created.plus(24, ChronoUnit.HOURS);
        SavedValidation saved = new SavedValidation(UUID.randomUUID(), created, expires, encode(request.message()), write(result));
        repository.save(saved);
        return new SavedValidationResponse(saved.getId(), created, expires, result);
    }

    public SavedValidationResponse get(UUID id) {
        SavedValidation saved = repository.findById(id).filter(item -> item.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new EntityNotFoundException("Validation not found or expired"));
        try {
            return new SavedValidationResponse(saved.getId(), saved.getCreatedAt(), saved.getExpiresAt(),
                    objectMapper.readValue(saved.getResultJson(), Hl7ParseResult.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Saved validation could not be decoded", ex);
        }
    }

    @Scheduled(fixedDelayString = "${app.saved-message-cleanup-ms:3600000}")
    public void deleteExpired() {
        repository.deleteAll(repository.findByExpiresAtBefore(Instant.now()));
    }

    private String write(Hl7ParseResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Validation result could not be serialized", ex);
        }
    }

    private String encode(String message) {
        String material = encryptionKey + ":" + message;
        return Base64.getEncoder().encodeToString(material.getBytes(StandardCharsets.UTF_8));
    }
}
