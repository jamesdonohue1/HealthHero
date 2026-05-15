package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkspaceActivityRepository extends JpaRepository<WorkspaceActivity, Long> {
    List<WorkspaceActivity> findTop50ByWorkspaceIdOrderByOccurredAtDesc(UUID workspaceId);

    void deleteByWorkspaceId(UUID workspaceId);
}
