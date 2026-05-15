# Healthcare Hero: Future Roadmap

This roadmap lists the next practical work after the current MVP. Priorities are ordered by security risk, platform maturity, and product value.

## Phase 2: PHI, Audit, and Compliance Controls

- [ ] Add audit log entity and service.
- [ ] Capture audit events for login, logout, save, export, delete, import, and admin actions.
- [ ] Add PHI-safe structured logging policy.
- [ ] Prevent raw HL7, clinical note text, diagnosis text, and patient identifiers from being written to application logs by default.
- [ ] Add PHI scanner middleware for saved content and exports.
- [ ] Add redaction preview before saving/exporting suspicious PHI.
- [ ] Encrypt saved payloads at rest with managed keys.
- [ ] Add key rotation strategy.
- [ ] Add user-controlled delete for saved records.
- [ ] Add organization data-retention settings.
- [ ] Add audit export for compliance review.
- [ ] Add terms of use, privacy notice, and coding/disclaimer acknowledgment.
- [ ] Add production HIPAA checklist and deployment guidance.

## Phase 3: Coding Data and Import Management

- [x] Add admin import screen for ICD-10-CM files.
- [x] Add admin import screen for HCPCS files from CMS.
- [x] Add support for licensed CPT data import once an authorized source is available.
- [x] Add code-set versioning and effective-date filtering.
- [x] Add retired/deleted/replaced code display.
- [x] Add background import jobs with validation summaries.
- [x] Add import rollback.
- [x] Add database full-text search indexes for ICD-10 and procedure codes.
- [x] Add synonym dictionary management.
- [x] Add payer-rule import for ICD/CPT compatibility.
- [x] Add LCD/NCD policy source tracking.
- [x] Add rule provenance and confidence scoring.

## Phase 4: User Workspaces and Saved Work

- [x] Convert short-lived saved searches into durable authenticated workspaces.
- [x] Add saved HL7 projects.
- [x] Add saved ICD-10 code lists.
- [x] Add saved CPT/HCPCS code lists.
- [x] Add saved ICD/CPT compatibility checks.
- [x] Add notes on saved records.
- [x] Add tags and folders.
- [x] Add global search across saved HL7, ICD, CPT, X12, FHIR, and exports.
- [x] Add team sharing and permissions.
- [x] Add duplicate/copy workspace flow.
- [x] Add activity history per workspace.

## Phase 5: HL7 and FHIR Depth

- [x] Add configurable HL7 validation profiles for ADT, ORM, ORU, SIU, DFT, MDM, VXU, and custom profiles.
- [x] Add richer datatype validation.
- [x] Add field length validation by segment/profile.
- [x] Add message sequencing checks.
- [x] Add ACK/NACK workflow analysis.
- [x] Add visual side-by-side repair comparison.
- [x] Add inline issue highlights in raw HL7 editor.
- [x] Add DG1 diagnosis extraction and one-click ICD-10 lookup from HL7.
- [x] Add OBX lab extraction into the Lab Interpreter module.
- [x] Add real FHIR library integration for validation.
- [x] Add FHIR profile validation.
- [x] Add configurable FHIR mapping templates in the UI.
- [x] Add batch HL7/FHIR conversion.

## Phase 6: X12, Eligibility, and Revenue Cycle

- [x] Expand X12 parser with required loop validation.
- [x] Add 837 professional/institutional/dental profile handling.
- [x] Add 835 denial/payment interpretation.
- [x] Add 270 generator UI.
- [x] Add 271 eligibility response decoder.
- [x] Add copay, coinsurance, deductible, and coverage summary extraction.
- [x] Add denial reason trend dashboard.
- [x] Add appeal letter packet builder.
- [x] Add prior authorization packet builder with documentation checklist.
- [x] Add payer-specific requirements database.
- [x] Add claim readiness score combining ICD specificity, CPT validity, modifiers, payer rules, and documentation gaps.

## Phase 7: AI-Assisted Workflows

- [x] Add AI provider abstraction.
- [x] Add PHI-safe prompt templates.
- [x] Add local-only/no-external-AI mode.
- [x] Add configurable PHI masking before AI calls.
- [x] Add AI audit logs for prompt type, model, token usage, and redaction status without storing raw PHI by default.
- [x] Add AI-assisted ICD refinement.
- [x] Add AI-assisted CPT suggestions.
- [x] Add AI-generated prior authorization summaries.
- [x] Add AI denial root-cause summaries.
- [x] Add AI documentation specificity prompts.
- [x] Add human review/approval UX for all AI-generated coding suggestions.

## Phase 8: Product UI and Workflow Improvements

- [x] Add unified project dashboard after login.
- [x] Add recent work and pinned work.
- [x] Add keyboard shortcuts for power users.
- [x] Add better mobile layouts for dense tables.
- [x] Add dark mode.
- [x] Add CSV/JSON upload flows.
- [x] Add batch results table with filtering and sorting.
- [x] Add copy/export presets.
- [x] Add in-app notifications for long-running jobs.
- [x] Add help drawer with examples and disclaimers.
- [x] Add onboarding samples for each module.

## Phase 9: API, Infrastructure, and Operations

- [x] Add API versioning.
- [x] Add OpenAPI examples for every endpoint.
- [x] Add request/response contract tests.
- [x] Add database migrations with Flyway or Liquibase.
- [x] Add Redis cache option for shared deployments.
- [x] Add background job queue for imports and batch conversions.
- [x] Add health checks and readiness/liveness probes.
- [x] Add metrics with Micrometer/Prometheus.
- [x] Add centralized error tracking.
- [x] Add rate limits by authenticated plan/API key.
- [x] Add Docker production profile.
- [x] Add deployment documentation for cloud environments.
- [x] Add backup and restore procedure.

## Phase 10: Testing and Quality

- [x] Add controller tests for auth, CPT/HCPCS, and coding compatibility APIs.
- [x] Add frontend tests for tab state persistence.
- [x] Add frontend tests for ICD/CPT Check clear behavior.
- [x] Add end-to-end tests for HL7, ICD-10, CPT, auth, and Platform Tools workflows.
- [x] Add accessibility checks.
- [x] Add security tests for authorization boundaries.
- [x] Add performance tests for large HL7/X12 payloads.
- [x] Add import tests for large code-set files.
- [x] Add regression fixtures for common HL7, FHIR, X12, ICD, and CPT examples.

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
