package com.hl7decoder.api;

import com.hl7decoder.model.FeatureFlagsResponse;
import com.hl7decoder.service.FeatureFlagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/features", "/api/v1/features"})
public class FeatureFlagController {
    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public FeatureFlagsResponse features() {
        return featureFlagService.publicFlags();
    }
}
