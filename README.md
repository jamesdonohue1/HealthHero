# Healthcare Hero

Healthcare Hero is a SaaS MVP for healthcare integration debugging, QA, and coding-support workflows. The app opens to a landing page where users can choose between the HL7 Decoder, ICD-10 Search, and Platform Tools modules.

## What is Included

### Platform

- React/TypeScript landing page for selecting the desired solution
- Java 21 Spring Boot API with HAPI HL7 dependencies
- PostgreSQL-ready JPA persistence plus H2 test/local profile support
- Swagger/OpenAPI at `/swagger-ui.html`
- Dockerfile, Docker Compose, and GitHub Actions CI

### HL7 Decoder

- HL7 v2.3+ parsing, decoding, metadata extraction, and configurable validation modes
- HL7 repair assistant that normalizes segment separators, repairs core MSH fields, and inserts placeholder required segments
- Advanced profile validation output for timestamps, field length thresholds, escape sequences, and ACK/NACK structure
- Generic custom Z-segment parsing with field/component/repetition preservation
- Decoded tree, grid, JSON, issues, exports, and save controls
- JSON, XML, PDF, pretty HL7, and CSV exports
- Anonymous saved validations with 24-hour expiration

### ICD-10 Search

- Plain-English ICD-10-CM diagnosis search module
- Backend-mediated calls to the NLM Clinical Tables ICD-10-CM API
- Input cleanup, abbreviation normalization, and multiple-diagnosis grouping
- Ranked results with code, descriptions, match percentage, billable indicator, chapter/category, source, query term, and match reason
- Fallback search variants for overly specific phrases, such as `chronic left knee pain` falling back to `left knee pain`
- Body-site-aware ranking so anatomical matches stay ahead of broad chronic-pain matches
- Clarifying questions and refinement suggestions for vague or underspecified diagnosis text
- Autocomplete suggestions while typing
- One-click copy buttons for ICD-10 codes plus selected-code copy formats
- Selected-code panel with copy, remove, clear, and export actions
- JSON, CSV, PDF, and plain-text ICD-10 exports with match and fallback audit fields
- Anonymous rate limiting, cached search results, retries, and 24-hour anonymous saved searches
- JPA persistence for saved ICD-10 searches
- Safety disclaimer and PHI warning in the UI

### Platform Tools

- HL7 to FHIR R4-like bundle conversion for Patient, Encounter, Observation, and DiagnosticReport resources
- FHIR R4-like bundle to HL7 ORU message conversion
- Mapping template API for visual mapping/editor workflows
- Synthetic healthcare data generator for fake patients, HL7 messages, FHIR bundles, and X12 claim samples
- Synthetic export manifest for `.hl7`, JSON, XML, and CSV batch workflows
- X12/EDI decoder for healthcare transaction envelopes, loops, segments, and common 837/835/270/271/276/277 transaction types
- Medical necessity checker that compares CPT codes to configured ICD-10 prefix rules and returns denial-risk recommendations
- MVP roadmap engines for prior authorization, denial analysis, CDI, terminology normalization, lab interpretation, interface monitoring, AI-assisted coding, API sandbox planning, eligibility inquiry generation, compliance scanning, and global search
- Copy-friendly JSON and text outputs in the React UI

## Local Development

Run the API with an in-memory H2 database:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

The Vite frontend proxies `/api`, `/swagger-ui.html`, and `/v3` to `http://localhost:8080`.

The default Spring profile expects PostgreSQL at `jdbc:postgresql://localhost:5432/hl7decoder`. Use the `test` profile for local H2 development unless PostgreSQL is running.

Run tests:

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
- `SAVED_MESSAGE_CLEANUP_MS`: saved HL7/ICD-10 cleanup interval
- `ICD10_API_BASE_URL`: ICD-10-CM API URL, default `https://clinicaltables.nlm.nih.gov/api/icd10cm/v3/search`

## Docker

```bash
docker compose up --build
```

The API will run at `http://localhost:8080` and use PostgreSQL from Docker Compose.

## ICD-10 Matching Notes

Healthcare Hero first searches the normalized diagnosis phrase exactly. If that returns too few matches, it tries controlled fallback variants and deduplicates results by ICD-10 code. Fallback results are marked with `fallbackMatch`, retain the `queryTerm` that produced the result, and receive a scoring penalty so exact and body-site-specific matches remain preferred.

Example fallback chain:

```text
chronic left knee pain
left knee pain
knee pain
chronic pain
```

## API

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

ICD-10 results are suggestions only and may be incomplete or inaccurate. Always verify codes with official coding guidelines, payer requirements, and a certified medical coder or qualified healthcare professional. Results are not medical advice or billing advice.
