package com.hl7decoder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hl7decoder.api.dto.workspace.WorkspaceCreateRequest;
import com.hl7decoder.api.dto.workspace.WorkspaceUpdateRequest;
import com.hl7decoder.model.workspace.WorkspaceActivityResponse;
import com.hl7decoder.model.workspace.WorkspaceRecordResponse;
import com.hl7decoder.persistence.WorkspaceActivity;
import com.hl7decoder.persistence.WorkspaceActivityRepository;
import com.hl7decoder.persistence.WorkspaceRecord;
import com.hl7decoder.persistence.WorkspaceRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {
    private final WorkspaceRecordRepository recordRepository;
    private final WorkspaceActivityRepository activityRepository;
    private final ObjectMapper objectMapper;

    public WorkspaceService(WorkspaceRecordRepository recordRepository, WorkspaceActivityRepository activityRepository, ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.activityRepository = activityRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkspaceRecordResponse create(UUID organizationId, UUID userId, WorkspaceCreateRequest request) {
        WorkspaceRecord record = new WorkspaceRecord(
                UUID.randomUUID(),
                organizationId,
                userId,
                normalizeType(request.recordType()),
                request.title().trim(),
                request.folder(),
                request.tags(),
                request.notes(),
                visibility(request.visibility()),
                write(request.payload())
        );
        recordRepository.save(record);
        activityRepository.save(new WorkspaceActivity(record.getId(), userId, "CREATED", "Created " + record.getRecordType()));
        return response(record);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceRecordResponse> list(UUID organizationId, String type, String query) {
        List<WorkspaceRecord> records;
        if (query != null && !query.isBlank()) {
            records = recordRepository.findTop50ByOrganizationIdAndSearchTextContainingOrderByUpdatedAtDesc(organizationId, query.trim().toLowerCase());
        } else if (type != null && !type.isBlank()) {
            records = recordRepository.findTop50ByOrganizationIdAndRecordTypeOrderByUpdatedAtDesc(organizationId, normalizeType(type));
        } else {
            records = recordRepository.findTop50ByOrganizationIdOrderByUpdatedAtDesc(organizationId);
        }
        return records.stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceRecordResponse get(UUID organizationId, UUID id) {
        return response(find(organizationId, id));
    }

    @Transactional
    public WorkspaceRecordResponse update(UUID organizationId, UUID userId, UUID id, WorkspaceUpdateRequest request) {
        WorkspaceRecord record = find(organizationId, id);
        record.updateMetadata(request.title(), request.folder(), request.tags(), request.notes(), request.visibility());
        if (request.payload() != null) {
            record.updatePayload(write(request.payload()));
        }
        activityRepository.save(new WorkspaceActivity(id, userId, "UPDATED", "Updated metadata or payload"));
        return response(record);
    }

    @Transactional
    public WorkspaceRecordResponse duplicate(UUID organizationId, UUID userId, UUID id) {
        WorkspaceRecord source = find(organizationId, id);
        WorkspaceRecord copy = recordRepository.save(source.copy(UUID.randomUUID(), userId));
        activityRepository.save(new WorkspaceActivity(source.getId(), userId, "DUPLICATED", "Copied to " + copy.getId()));
        activityRepository.save(new WorkspaceActivity(copy.getId(), userId, "CREATED", "Duplicated from " + source.getId()));
        return response(copy);
    }

    @Transactional
    public void delete(UUID organizationId, UUID userId, UUID id) {
        WorkspaceRecord record = find(organizationId, id);
        activityRepository.deleteByWorkspaceId(id);
        recordRepository.delete(record);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceActivityResponse> activity(UUID organizationId, UUID id) {
        find(organizationId, id);
        return activityRepository.findTop50ByWorkspaceIdOrderByOccurredAtDesc(id).stream()
                .map(item -> new WorkspaceActivityResponse(item.getWorkspaceId(), item.getActorUserId(), item.getOccurredAt(), item.getAction(), item.getDetail()))
                .toList();
    }

    private WorkspaceRecord find(UUID organizationId, UUID id) {
        return recordRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Workspace record not found."));
    }

    private WorkspaceRecordResponse response(WorkspaceRecord record) {
        return new WorkspaceRecordResponse(record.getId(), record.getRecordType(), record.getTitle(), record.getFolder(),
                record.getTags(), record.getNotes(), record.getVisibility(), record.getOwnerUserId(), record.getCreatedAt(),
                record.getUpdatedAt(), read(record.getPayloadJson()));
    }

    private String normalizeType(String value) {
        return value == null ? "NOTE" : value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    }

    private String visibility(String value) {
        return value == null || value.isBlank() ? "TEAM" : value.trim().toUpperCase();
    }

    private String write(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Workspace payload could not be serialized.", ex);
        }
    }

    private JsonNode read(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (Exception ex) {
            throw new IllegalStateException("Workspace payload could not be decoded.", ex);
        }
    }
}
