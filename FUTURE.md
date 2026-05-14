# Healthcare Hero: Future Roadmap

This roadmap lists the next practical work after the current MVP. Priorities are ordered by security risk, platform maturity, and product value.

## Phase 2: PHI, Audit, and Compliance Controls

- [x] Add audit log entity and service.
- [x] Capture audit events for login, logout, save, export, delete, import, and admin actions.
- [x] Add PHI-safe structured logging policy.
- [x] Prevent raw HL7, clinical note text, diagnosis text, and patient identifiers from being written to application logs by default.
- [x] Add PHI scanner middleware for saved content and exports.
- [x] Add redaction preview before saving/exporting suspicious PHI.
- [x] Encrypt saved payloads at rest with managed keys.
- [x] Add key rotation strategy.
- [x] Add user-controlled delete for saved records.
- [x] Add organization data-retention settings.
- [x] Add audit export for compliance review.
- [x] Add terms of use, privacy notice, and coding/disclaimer acknowledgment.
- [x] Add production HIPAA checklist and deployment guidance.

## Phase 3: Coding Data and Import Management

- [ ] Add admin import screen for ICD-10-CM files.
- [ ] Add admin import screen for HCPCS files from CMS.
- [ ] Add support for licensed CPT data import once an authorized source is available.
- [ ] Add code-set versioning and effective-date filtering.
- [ ] Add retired/deleted/replaced code display.
- [ ] Add background import jobs with validation summaries.
- [ ] Add import rollback.
- [ ] Add database full-text search indexes for ICD-10 and procedure codes.
- [ ] Add synonym dictionary management.
- [ ] Add payer-rule import for ICD/CPT compatibility.
- [ ] Add LCD/NCD policy source tracking.
- [ ] Add rule provenance and confidence scoring.

## Phase 4: User Workspaces and Saved Work

- [ ] Convert short-lived saved searches into durable authenticated workspaces.
- [ ] Add saved HL7 projects.
- [ ] Add saved ICD-10 code lists.
- [ ] Add saved CPT/HCPCS code lists.
- [ ] Add saved ICD/CPT compatibility checks.
- [ ] Add notes on saved records.
- [ ] Add tags and folders.
- [ ] Add global search across saved HL7, ICD, CPT, X12, FHIR, and exports.
- [ ] Add team sharing and permissions.
- [ ] Add duplicate/copy workspace flow.
- [ ] Add activity history per workspace.

## Phase 5: HL7 and FHIR Depth

- [ ] Add configurable HL7 validation profiles for ADT, ORM, ORU, SIU, DFT, MDM, VXU, and custom profiles.
- [ ] Add richer datatype validation.
- [ ] Add field length validation by segment/profile.
- [ ] Add message sequencing checks.
- [ ] Add ACK/NACK workflow analysis.
- [ ] Add visual side-by-side repair comparison.
- [ ] Add inline issue highlights in raw HL7 editor.
- [ ] Add DG1 diagnosis extraction and one-click ICD-10 lookup from HL7.
- [ ] Add OBX lab extraction into the Lab Interpreter module.
- [ ] Add real FHIR library integration for validation.
- [ ] Add FHIR profile validation.
- [ ] Add configurable FHIR mapping templates in the UI.
- [ ] Add batch HL7/FHIR conversion.

## Phase 6: X12, Eligibility, and Revenue Cycle

- [ ] Expand X12 parser with required loop validation.
- [ ] Add 837 professional/institutional/dental profile handling.
- [ ] Add 835 denial/payment interpretation.
- [ ] Add 270 generator UI.
- [ ] Add 271 eligibility response decoder.
- [ ] Add copay, coinsurance, deductible, and coverage summary extraction.
- [ ] Add denial reason trend dashboard.
- [ ] Add appeal letter packet builder.
- [ ] Add prior authorization packet builder with documentation checklist.
- [ ] Add payer-specific requirements database.
- [ ] Add claim readiness score combining ICD specificity, CPT validity, modifiers, payer rules, and documentation gaps.

## Phase 7: AI-Assisted Workflows

- [ ] Add AI provider abstraction.
- [ ] Add PHI-safe prompt templates.
- [ ] Add local-only/no-external-AI mode.
- [ ] Add configurable PHI masking before AI calls.
- [ ] Add AI audit logs for prompt type, model, token usage, and redaction status without storing raw PHI by default.
- [ ] Add AI-assisted ICD refinement.
- [ ] Add AI-assisted CPT suggestions.
- [ ] Add AI-generated prior authorization summaries.
- [ ] Add AI denial root-cause summaries.
- [ ] Add AI documentation specificity prompts.
- [ ] Add human review/approval UX for all AI-generated coding suggestions.

## Phase 8: Product UI and Workflow Improvements

- [ ] Add unified project dashboard after login.
- [ ] Add recent work and pinned work.
- [ ] Add keyboard shortcuts for power users.
- [ ] Add better mobile layouts for dense tables.
- [ ] Add dark mode.
- [ ] Add CSV/JSON upload flows.
- [ ] Add batch results table with filtering and sorting.
- [ ] Add copy/export presets.
- [ ] Add in-app notifications for long-running jobs.
- [ ] Add help drawer with examples and disclaimers.
- [ ] Add onboarding samples for each module.

## Phase 9: API, Infrastructure, and Operations

- [ ] Add API versioning.
- [ ] Add OpenAPI examples for every endpoint.
- [ ] Add request/response contract tests.
- [ ] Add database migrations with Flyway or Liquibase.
- [ ] Add Redis cache option for shared deployments.
- [ ] Add background job queue for imports and batch conversions.
- [ ] Add health checks and readiness/liveness probes.
- [ ] Add metrics with Micrometer/Prometheus.
- [ ] Add centralized error tracking.
- [ ] Add rate limits by authenticated plan/API key.
- [ ] Add Docker production profile.
- [ ] Add deployment documentation for cloud environments.
- [ ] Add backup and restore procedure.

## Phase 10: Testing and Quality

- [ ] Add controller tests for auth, CPT/HCPCS, and coding compatibility APIs.
- [ ] Add frontend tests for tab state persistence.
- [ ] Add frontend tests for ICD/CPT Check clear behavior.
- [ ] Add end-to-end tests for HL7, ICD-10, CPT, auth, and Platform Tools workflows.
- [ ] Add accessibility checks.
- [ ] Add security tests for authorization boundaries.
- [ ] Add performance tests for large HL7/X12 payloads.
- [ ] Add import tests for large code-set files.
- [ ] Add regression fixtures for common HL7, FHIR, X12, ICD, and CPT examples.

## Suggested Near-Term Order

1. Audit logging and PHI-safe logging.
2. Database migrations with Flyway or Liquibase.
3. Email-token password recovery.
4. Full SSO provider integration.
5. Full MFA enrollment/challenge flow.
6. Code-set import admin screens.
7. Durable authenticated saved workspaces.
8. Payer-rule and licensed CPT/HCPCS dataset integration.
9. Frontend end-to-end tests.
10. Production deployment/security hardening.
11. AI provider abstraction with PHI-safe controls.
