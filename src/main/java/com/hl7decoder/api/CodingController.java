package com.hl7decoder.api;

import com.hl7decoder.api.dto.cpt.IcdCptCompatibilityRequest;
import com.hl7decoder.model.cpt.IcdCptMatchResult;
import com.hl7decoder.service.IcdCptCompatibilityService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coding")
public class CodingController {
    private final IcdCptCompatibilityService compatibilityService;

    public CodingController(IcdCptCompatibilityService compatibilityService) {
        this.compatibilityService = compatibilityService;
    }

    @PostMapping("/compatibility")
    public IcdCptMatchResult compatibility(@RequestBody IcdCptCompatibilityRequest request) {
        return compatibilityService.check(request);
    }
}
