package com.hl7decoder.api;

import com.hl7decoder.api.dto.workspace.WorkspaceCreateRequest;
import com.hl7decoder.api.dto.workspace.WorkspaceUpdateRequest;
import com.hl7decoder.model.workspace.WorkspaceActivityResponse;
import com.hl7decoder.model.workspace.WorkspaceRecordResponse;
import com.hl7decoder.security.AuthenticatedPrincipal;
import com.hl7decoder.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public WorkspaceRecordResponse create(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                          @Valid @RequestBody WorkspaceCreateRequest request) {
        return workspaceService.create(principal.organizationId(), principal.userId(), request);
    }

    @GetMapping
    public List<WorkspaceRecordResponse> list(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) String q) {
        return workspaceService.list(principal.organizationId(), type, q);
    }

    @GetMapping("/{id}")
    public WorkspaceRecordResponse get(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID id) {
        return workspaceService.get(principal.organizationId(), id);
    }

    @PatchMapping("/{id}")
    public WorkspaceRecordResponse update(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                          @PathVariable UUID id,
                                          @RequestBody WorkspaceUpdateRequest request) {
        return workspaceService.update(principal.organizationId(), principal.userId(), id, request);
    }

    @PostMapping("/{id}/duplicate")
    public WorkspaceRecordResponse duplicate(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID id) {
        return workspaceService.duplicate(principal.organizationId(), principal.userId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID id) {
        workspaceService.delete(principal.organizationId(), principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/activity")
    public List<WorkspaceActivityResponse> activity(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID id) {
        return workspaceService.activity(principal.organizationId(), id);
    }
}
