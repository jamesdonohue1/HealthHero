package com.hl7decoder.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_activity", indexes = {
        @Index(name = "idx_workspace_activity_record", columnList = "workspaceId,occurredAt")
})
public class WorkspaceActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private UUID actorUserId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private String action;

    private String detail;

    protected WorkspaceActivity() {
    }

    public WorkspaceActivity(UUID workspaceId, UUID actorUserId, String action, String detail) {
        this.workspaceId = workspaceId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.detail = detail;
        this.occurredAt = Instant.now();
    }

    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getActorUserId() { return actorUserId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
}
