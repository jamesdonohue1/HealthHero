# Workspaces and Saved Work

Phase 4 adds durable authenticated workspaces for saved clinical/coding work. Workspaces are organization-scoped records with a type, title, folder, tags, notes, visibility, payload, duplicate flow, delete flow, global search, and activity history.

## Workspace Types

- `HL7_PROJECT`: raw HL7 message, validation mode, and parse/validation result.
- `ICD10_SEARCH`: ICD-10 search input and grouped results.
- `ICD10_CODE_LIST`: selected ICD-10 codes plus source search context.
- `CPT_HCPCS_CODE_LIST`: CPT/HCPCS procedure search results and context.
- `ICD_CPT_CHECK`: diagnosis/procedure compatibility request and result.

The backend accepts other record types as uppercase identifiers, which gives later modules a migration path without schema changes.

## API

- `POST /api/workspaces`: create a workspace.
- `GET /api/workspaces`: list recent workspace records.
- `GET /api/workspaces?q=term`: global search across title, type, folder, tags, notes, and payload.
- `GET /api/workspaces?type=HL7_PROJECT`: filter by workspace type.
- `GET /api/workspaces/{id}`: read one workspace.
- `PATCH /api/workspaces/{id}`: update metadata or payload.
- `POST /api/workspaces/{id}/duplicate`: duplicate a workspace.
- `DELETE /api/workspaces/{id}`: delete a workspace.
- `GET /api/workspaces/{id}/activity`: view activity history.

All endpoints require authentication and are scoped to the authenticated user's organization.

## UI

The Workspaces tab provides search, type filtering, metadata editing, duplicate/delete, payload inspection, and activity history. HL7, ICD-10, CPT/HCPCS, and ICD/CPT Check screens include Workspace buttons to save durable work directly from the active workflow.

## Sharing and Permissions

Each workspace has a visibility value: `PRIVATE`, `TEAM`, or `READ_ONLY`. The current implementation stores and displays this sharing mode and scopes all records to the organization. Fine-grained user ACL enforcement can be layered on top of the same model later.
