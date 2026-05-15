# AI-Assisted Workflows

Phase 7 adds a local-first AI assistance layer for coding and revenue-cycle workflows.

## Capabilities

- Provider abstraction exposed through `/api/ai` and `/api/v1/ai`.
- Local-only mode by default with `app.ai.local-only=true`.
- Configurable PHI masking with `app.ai.mask-phi=true`.
- Prompt types for ICD refinement, CPT suggestions, prior authorization summaries, denial root-cause summaries, and documentation specificity prompts.
- Human review status on every coding-oriented response.
- Metadata-only AI audit events. Prompt text and generated output are not stored by default.

## Endpoints

- `POST /api/v1/ai/assist`
- `POST /api/v1/ai/icd-refine`
- `POST /api/v1/ai/cpt-suggest`
- `POST /api/v1/ai/prior-auth-summary`
- `POST /api/v1/ai/denial-root-cause`
- `POST /api/v1/ai/documentation-specificity`
- `GET /api/v1/ai/audit`

All endpoints require authentication. Audit rows include organization, user, prompt type, provider, model, token estimate, redaction status, external-call status, and approval status.

## Configuration

```yaml
app:
  ai:
    provider: local
    local-only: true
    mask-phi: true
    default-model: healthcare-hero-local-rules
```

Production external-provider integration should keep PHI masking enabled unless a signed deployment policy explicitly allows unmasked prompts.
