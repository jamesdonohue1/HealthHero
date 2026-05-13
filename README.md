# Healthcare Decoder

Healthcare Decoder is a SaaS MVP for healthcare integration debugging, QA, and coding-support workflows. The app now opens to a landing page where users can choose between the HL7 Decoder and ICD-10 Search modules.

## What is Included

### Platform

- React/TypeScript landing page for selecting the desired solution
- Java 21 Spring Boot API with HAPI HL7 dependencies
- PostgreSQL-ready JPA persistence plus H2 test/local profile support
- Swagger/OpenAPI at `/swagger-ui.html`
- Dockerfile, Docker Compose, and GitHub Actions CI

### HL7 Decoder

- HL7 v2.3+ parsing, decoding, metadata extraction, and configurable validation modes
- Generic custom Z-segment parsing with field/component/repetition preservation
- Decoded tree, grid, JSON, issues, exports, and save controls
- JSON, XML, PDF, pretty HL7, and CSV exports
- Anonymous saved validations with 24-hour expiration

### ICD-10 Search

- Plain-English ICD-10-CM diagnosis search module
- Backend-mediated calls to the NLM Clinical Tables ICD-10-CM API
- Input cleanup, abbreviation normalization, and multiple-diagnosis grouping
- Ranked results with code, descriptions, score, billable indicator, chapter/category, and match reason
- Clarifying questions for vague or underspecified diagnosis text
- Selected-code panel with copy, remove, clear, and export actions
- JSON, CSV, PDF, and plain-text ICD-10 exports
- Anonymous rate limiting, cached search results, retries, and optional 24-hour anonymous saved searches
- Safety disclaimer and PHI warning in the UI

## Local Development

Run the API with an in-memory database:

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

## Configuration

Common environment variables:

- `PORT`: API port, default `8080`
- `DATABASE_URL`: PostgreSQL JDBC URL
- `DATABASE_USERNAME`: database username
- `DATABASE_PASSWORD`: database password
- `ENCRYPTION_KEY`: local encryption key for saved HL7 messages
- `SAVED_MESSAGE_CLEANUP_MS`: saved-message cleanup interval
- `ICD10_API_BASE_URL`: ICD-10-CM API URL, default `https://clinicaltables.nlm.nih.gov/api/icd10cm/v3/search`

## Docker

```bash
docker compose up --build
```

The API will run at `http://localhost:8080` and use PostgreSQL from Docker Compose.

## API

### HL7

- `POST /api/hl7/parse`
- `POST /api/hl7/validate`
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

## Safety

Do not submit real PHI unless authorized to do so.

ICD-10 results are suggestions only and may be incomplete or inaccurate. Always verify codes with official coding guidelines, payer requirements, and a certified medical coder or qualified healthcare professional. Results are not medical advice or billing advice.
