package com.hl7decoder.api;

import com.hl7decoder.api.dto.admin.CodeSetImportRequest;
import com.hl7decoder.model.admin.CodeSetImportJobResponse;
import com.hl7decoder.security.AuthenticatedPrincipal;
import com.hl7decoder.service.CodeSetImportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/imports")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORGANIZATION_ADMIN')")
public class AdminImportController {
    private final CodeSetImportService importService;

    public AdminImportController(CodeSetImportService importService) {
        this.importService = importService;
    }

    @PostMapping
    public CodeSetImportJobResponse importContent(@Valid @RequestBody CodeSetImportRequest request,
                                                  @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return importService.importContent(request, principal.organizationId(), principal.userId());
    }

    @GetMapping
    public List<CodeSetImportJobResponse> history() {
        return importService.history();
    }

    @PostMapping("/{id}/rollback")
    public CodeSetImportJobResponse rollback(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return importService.rollback(id, principal.organizationId(), principal.userId());
    }
}
