# Healthcare Hero

Healthcare Hero is a Spring Boot and React application for healthcare interoperability, coding support, validation, and revenue-cycle workflows.

The app has two local processes:

- Backend API: Java 21 Spring Boot on `http://localhost:8080`
- Frontend: Vite React app on `http://localhost:5173`

The Vite dev server proxies `/api`, `/swagger-ui.html`, and `/v3` to the backend.

## Prerequisites

Install these before running locally:

- Java 21
- Maven 3.9+
- Node.js 20+ and npm
- Docker Desktop, optional, for the Postgres/Docker workflow

Check your local versions:

```bash
java -version
mvn -version
node -v
npm -v
docker --version
```

## Quick Start: API With In-Memory H2

This is the fastest local setup because it does not require Postgres.

From the repository root:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the app:

```text
http://localhost:5173
```

Useful backend URLs:

```text
http://localhost:8080/actuator/health
http://localhost:8080/swagger-ui.html
```

The `test` profile uses an in-memory H2 database:

```yaml
jdbc:h2:mem:hl7decoder;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```

Data is reset when the backend process stops.

## Local Postgres Setup

Use this when you want data to persist across backend restarts.

Start Postgres:

```bash
docker compose up -d postgres
```

Run the API with the local profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Then run the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

Default local Postgres settings from `docker-compose.yml`:

```text
Database: hl7decoder
Username: hl7decoder
Password: hl7decoder
Host: localhost
Port: 5432
JDBC URL: jdbc:postgresql://localhost:5432/hl7decoder
```

## Docker Compose API Setup

The included `Dockerfile` copies a prebuilt JAR from `target/`, so package the backend before starting the API container.

```bash
mvn package
docker compose up --build
```

This starts:

- Postgres on host port `5432`
- Backend API on host port `8080`

The Compose file does not start the React dev server. Run the frontend separately:

```bash
cd frontend
npm install
npm run dev
```

## Frontend Configuration

By default, the frontend calls relative API paths such as `/api/hl7/parse`. In local development, `frontend/vite.config.ts` proxies those requests to:

```text
http://localhost:8080
```

If you need the frontend to call a different backend URL directly, set:

```bash
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

Most local development should not need this because the Vite proxy is already configured.

## Backend Configuration

Common environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | Backend HTTP port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/hl7decoder` | JDBC database URL |
| `DATABASE_USERNAME` | `hl7decoder` | Database username |
| `DATABASE_PASSWORD` | `hl7decoder` | Database password |
| `ENCRYPTION_KEY` | `local-dev-key-change-me` | Local encryption key for saved HL7 messages |
| `JWT_SECRET` | `dev-change-me-healthcare-hero` | HMAC token signing secret |
| `TOKEN_TTL_MINUTES` | `480` | Bearer token lifetime |
| `IDLE_TIMEOUT_MINUTES` | `30` | Advertised frontend idle timeout |
| `MAX_FAILED_ATTEMPTS` | `5` | Failed logins before lockout |
| `LOCKOUT_MINUTES` | `15` | Account lockout duration |
| `SECURE_COOKIES` | `false` | Secure cookie flag for local/prod cookie metadata |
| `ICD10_API_BASE_URL` | NLM Clinical Tables ICD-10-CM API | ICD-10 lookup API base URL |
| `API_RATE_LIMIT_PER_MINUTE` | `120` | API rate limit |
| `FLYWAY_ENABLED` | `true` | Enable database migrations |

Example local override:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/hl7decoder \
DATABASE_USERNAME=hl7decoder \
DATABASE_PASSWORD=hl7decoder \
ENCRYPTION_KEY=local-dev-only \
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Verification

Run backend tests:

```bash
mvn test
```

Build the frontend:

```bash
cd frontend
npm run build
```

Run the frontend regression test:

```bash
cd frontend
npm test
```

## Main API Endpoints

OpenAPI documentation is available locally at:

```text
http://localhost:8080/swagger-ui.html
```

Common endpoints:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/hl7/parse`
- `POST /api/hl7/validate`
- `POST /api/hl7/repair`
- `POST /api/icd10/search`
- `POST /api/icd10/autocomplete`
- `GET /api/cpt/search?q={query}`
- `POST /api/coding/compatibility`
- `POST /api/platform/x12/decode`
- `POST /api/platform/necessity/check`

Authentication is required for saved validations, saved ICD-10 searches, workspaces, AI endpoints, and admin imports.

## Troubleshooting

If the frontend loads but API calls fail, confirm the backend is running on `8080`:

```bash
curl http://localhost:8080/actuator/health
```

If `npm run dev` reports that port `5173` is already in use, stop the existing Vite process or run Vite on another port:

```bash
npm run dev -- --port 5174
```

If the backend cannot connect to Postgres, confirm the container is running:

```bash
docker compose ps
```

If the Docker API build fails because the JAR is missing, package the backend first:

```bash
mvn package
docker compose up --build
```

If dependencies are missing, reinstall them:

```bash
cd frontend
npm install
```

## Project Layout

```text
src/main/java/com/hl7decoder      Spring Boot API source
src/main/resources                Spring configuration and Flyway migrations
src/test                          Backend tests and fixtures
frontend/src                      React frontend source
frontend/scripts                  Frontend regression tests
docs                              Feature and operations documentation
docker-compose.yml                Local Postgres and API services
Dockerfile                        Backend container image
CURRENT.md                        Current product/module state
FUTURE.md                         Future roadmap
```
