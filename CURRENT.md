# Healthcare Hero: Current State

Healthcare Hero is a Spring Boot + React MVP for healthcare interoperability, coding support, validation, and revenue-cycle workflows.

## Application Modules

### Platform

- React/TypeScript web app with persistent module tabs.
- Java 21 Spring Boot API.
- PostgreSQL-ready JPA persistence.
- H2 test/local profile support.
- Swagger/OpenAPI at `/swagger-ui.html`.
- Docker, Docker Compose, and GitHub Actions project support.
- PHI warning banners and coding-safety disclaimers.

### Authentication and Security

- Spring Security configuration.
- Local user registration and login.
- Logout endpoint.
- MVP password reset endpoint.
- Account settings update.
- BCrypt password hashing.
- Failed-login lockout.
- HMAC-signed bearer tokens.
- `Authorization: Bearer <token>` API authentication.
- API key creation.
- `X-API-Key` authentication for programmatic access.
- Organization accounts and user membership.
- Role model:
  - `PLATFORM_ADMIN`
  - `ORGANIZATION_ADMIN`
  - `CODER`
  - `INTERFACE_ANALYST`
  - `READ_ONLY_REVIEWER`
- Organization-scoped saved HL7 validations and ICD-10 searches.
- Protected saved/history endpoints.
- Admin endpoint pattern protected for platform admins.
- Stateless CSRF strategy for bearer-token APIs.
- Security headers for CSP, HSTS, frame options, content-type options, and referrer policy.
- OAuth2/OIDC dependency and capability metadata for future SSO integration.
- MFA capability metadata for future provider/TOTP integration.

Auth caveats:

- Password reset is an MVP direct-reset endpoint and should be replaced with email-token verification before production use.
- SSO and MFA are architecture-ready, but external provider enrollment/challenge flows are not implemented yet.
- Audit logging, PHI redaction middleware, retention controls, and production compliance workflows remain future work.

### HL7 Decoder

- HL7 v2.3+ parsing, decoding, metadata extraction, and configurable validation modes.
- HL7 repair assistant that normalizes segment separators, repairs core MSH fields, and inserts placeholder required segments.
- Advanced profile validation output for timestamps, field length thresholds, escape sequences, and ACK/NACK structure.
- Generic custom Z-segment parsing with field/component/repetition preservation.
- Decoded tree, grid, JSON, issues, exports, and save controls.
- Search/filter across segments and fields.
- Issue navigation with row highlighting.
- JSON, XML, PDF, pretty HL7, and CSV exports.
- Authenticated saved validations with 24-hour expiration and organization scoping.

### ICD-10 Search

- Plain-English ICD-10-CM diagnosis search.
- Backend-mediated calls to NLM Clinical Tables ICD-10-CM API.
- Local cache with 24-hour TTL.
- Small local ICD-10 safety index for common searches.
- External rate-limit handling with Retry-After query pausing.
- Input cleanup, abbreviation normalization, and multiple-diagnosis grouping.
- Ranked results with code, descriptions, match percentage, billable indicator, chapter/category, source, query term, and match reason.
- Fallback search variants for overly specific phrases, such as `chronic left knee pain` falling back to `left knee pain`.
- Body-site-aware ranking.
- Clarifying questions and refinement suggestions.
- Autocomplete while typing.
- Selected-code panel.
- One-click copy buttons for ICD-10 codes and selected-code formats.
- JSON, CSV, PDF, and plain-text exports.
- Anonymous rate limiting for public search/autocomplete.
- Authenticated saved searches with 24-hour expiration and organization scoping.
- PHI warning and coding disclaimer.

### CPT/HCPCS Search

- Free-form procedure, service, code, modifier, and code-range search.
- Ranked CPT/HCPCS/modifier suggestions from a small built-in index plus optional database-backed procedure codes.
- Licensing notice for AMA CPT content and authorized data-source requirements.
- Effective-date and active/retired metadata support in the model.
- ICD-10/CPT compatibility engine with statuses:
  - `SUPPORTED`
  - `NEEDS_MORE_SPECIFIC_DIAGNOSIS`
  - `LIKELY_DENIAL`
  - `UNKNOWN_RULE_NOT_FOUND`
- Claim-readiness recommendations for diagnosis specificity, modifier review, payer checks, and denial risk.
- Frontend ICD/CPT Check panel with clear/reset action.

Licensing caveat:

- CPT descriptions are copyrighted by the AMA. The bundled CPT/HCPCS module contains only a small sample/index-ready dataset for development and requires an authorized CPT/HCPCS data source before production use.

### Platform Tools

- HL7 to FHIR R4-like bundle conversion.
- FHIR R4-like bundle to HL7 ORU conversion.
- FHIR mapping template endpoint.
- Synthetic healthcare data generator for fake patients, HL7 messages, FHIR bundles, and X12 claim samples.
- Synthetic export manifest for `.hl7`, JSON, XML, and CSV batch workflows.
- X12/EDI decoder for healthcare transaction envelopes, loops, segments, and common 837/835/270/271/276/277 transaction types.
- Medical necessity checker comparing CPT codes to configured ICD-10 prefix rules.
- MVP roadmap engines for:
  - Prior authorization
  - Denial analysis
  - CDI
  - Terminology normalization
  - Lab interpretation
  - Interface monitoring
  - AI-assisted coding
  - API sandbox planning
  - Eligibility inquiry generation
  - Compliance scan
  - Global search

## Local Development

Run the API with H2:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

The Vite frontend proxies `/api`, `/swagger-ui.html`, and `/v3` to `http://localhost:8080`.

Run verification:

```bash
mvn test
cd frontend
npm run build
```

## Configuration

Common environment variables:

- `PORT`: API port, default `8080`
- `DATABASE_URL`: PostgreSQL JDBC URL
- `DATABASE_USERNAME`: database username
- `DATABASE_PASSWORD`: database password
- `ENCRYPTION_KEY`: local encryption key for saved HL7 messages
- `SAVED_MESSAGE_CLEANUP_MS`: saved cleanup interval
- `ICD10_API_BASE_URL`: ICD-10-CM API URL
- `JWT_SECRET`: HMAC token signing secret
- `TOKEN_TTL_MINUTES`: bearer token lifetime, default `480`
- `IDLE_TIMEOUT_MINUTES`: advertised frontend idle timeout setting, default `30`
- `MAX_FAILED_ATTEMPTS`: failed login attempts before lockout, default `5`
- `LOCKOUT_MINUTES`: account lockout duration, default `15`
- `SECURE_COOKIES`: secure session cookie metadata, default `false` for local development

## API Summary

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/password-reset`
- `GET /api/auth/me`
- `PATCH /api/auth/me`
- `POST /api/auth/api-keys`
- `GET /api/auth/capabilities`

### HL7

- `POST /api/hl7/parse`
- `POST /api/hl7/validate`
- `POST /api/hl7/repair`
- `POST /api/exports/json`
- `POST /api/exports/xml`
- `POST /api/exports/pdf`
- `POST /api/exports/hl7`
- `POST /api/exports/csv`
- `POST /api/validations`
- `GET /api/validations/{id}`

Authentication is required for saved validation endpoints.

### ICD-10

- `POST /api/icd10/search`
- `POST /api/icd10/autocomplete`
- `POST /api/icd10/refine`
- `POST /api/icd10/export/json`
- `POST /api/icd10/export/csv`
- `POST /api/icd10/export/pdf`
- `POST /api/icd10/export/text`
- `POST /api/icd10/save`
- `GET /api/icd10/history`
- `GET /api/icd10/saved/{id}`
- `DELETE /api/icd10/saved/{id}`

Authentication is required for saved ICD-10 endpoints.

### CPT/HCPCS and Coding

- `GET /api/cpt/search?q={query}`
- `GET /api/cpt/{code}`
- `POST /api/coding/compatibility`

### Platform Tools

- `POST /api/platform/fhir/hl7-to-fhir`
- `POST /api/platform/fhir/fhir-to-hl7`
- `POST /api/platform/fhir/mapping-template`
- `POST /api/platform/hl7/profile-validate`
- `POST /api/platform/synthetic/generate`
- `POST /api/platform/synthetic/export-manifest`
- `POST /api/platform/x12/decode`
- `POST /api/platform/necessity/check`
- `POST /api/platform/prior-auth/analyze`
- `POST /api/platform/denials/analyze`
- `POST /api/platform/cdi/analyze`
- `POST /api/platform/terminology/normalize`
- `POST /api/platform/labs/interpret`
- `POST /api/platform/monitoring/snapshot`
- `POST /api/platform/coding/assist`
- `POST /api/platform/sandbox/plan`
- `POST /api/platform/eligibility/analyze`
- `POST /api/platform/compliance/scan`
- `POST /api/platform/search`

## Safety

Do not submit real PHI unless authorized to do so.

ICD-10, CPT/HCPCS, medical necessity, compatibility, and coding-assistant results are suggestions only and may be incomplete or inaccurate. Always verify codes with official coding guidelines, payer requirements, licensed code sources, and a certified medical coder or qualified healthcare professional. Results are not medical advice or billing advice.

