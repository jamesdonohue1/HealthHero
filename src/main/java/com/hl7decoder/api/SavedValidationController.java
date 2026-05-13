package com.hl7decoder.api;

import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.SavedValidationResponse;
import com.hl7decoder.service.SavedValidationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/validations")
public class SavedValidationController {
    private final SavedValidationService savedValidationService;

    public SavedValidationController(SavedValidationService savedValidationService) {
        this.savedValidationService = savedValidationService;
    }

    @PostMapping
    public SavedValidationResponse save(@Valid @RequestBody Hl7Request request) {
        return savedValidationService.saveAnonymous(request);
    }

    @GetMapping("/{id}")
    public SavedValidationResponse get(@PathVariable UUID id) {
        return savedValidationService.get(id);
    }
}
