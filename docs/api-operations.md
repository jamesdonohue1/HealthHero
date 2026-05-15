# API, Infrastructure, and Operations

Phase 9 adds operational hooks for production hardening.

## API Versioning

New stable endpoints use `/api/v1`. Legacy `/api` routes remain available for current clients.

- `GET /api/v1/version`
- `GET /api/v1/health`

OpenAPI advertises both `/api/v1` and `/api` server prefixes and includes reusable examples for HL7, text-tool, and AI-assist requests.

## Feature Flags

`GET /api/v1/features` returns public release switches consumed by the frontend. Disabled backend feature routes return `404`, so staged releases remain enforced server-side. See `docs/feature-flags.md` for the environment variables and rollout example.

## Migrations

Flyway is enabled with `baseline-on-migrate=true`. `V1__baseline.sql` is a history marker while local/test profiles continue using Hibernate-managed schema creation.

## Health and Metrics

Actuator endpoints:

- `/actuator/health`
- `/actuator/health/readiness`
- `/actuator/health/liveness`
- `/actuator/metrics`
- `/actuator/prometheus`

## Background Jobs and Cache

The app now exposes a bounded Spring `TaskExecutor` for imports and batch conversions. Shared deployments can select a Redis-backed implementation later with:

```yaml
app:
  cache:
    provider: redis
    redis-url: redis://redis:6379
```

## Error Tracking and Rate Limits

Handled API errors receive a `correlationId` and are logged through a centralized sanitized error tracker.

Rate limits apply per bearer token, API key, or IP:

```yaml
app:
  api:
    rate-limit-per-minute: 120
```

## Production Profile

Use the `prod` profile for deployment:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar target/hl7-decoder-0.1.0-SNAPSHOT.jar
```

Required production secrets:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `ENCRYPTION_KEY`
- `SECURE_COOKIES=true`
