# Deployment and Backup Procedure

## Cloud Deployment

1. Build the application with `mvn clean package` and `npm run build`.
2. Use `SPRING_PROFILES_ACTIVE=prod`.
3. Provide managed PostgreSQL credentials through environment variables.
4. Terminate TLS at the ingress or load balancer and set `SECURE_COOKIES=true`.
5. Expose only application traffic and approved actuator health endpoints.
6. Scrape `/actuator/prometheus` from the private monitoring network.
7. Store `JWT_SECRET` and `ENCRYPTION_KEY` in a managed secrets service.

## Backup

Run a daily encrypted PostgreSQL dump:

```bash
pg_dump "$DATABASE_URL" --format=custom --file=healthcare-hero-$(date +%Y%m%d).dump
```

Store backups in a versioned encrypted bucket with retention that matches the organization retention policy.

## Restore

1. Stop application writers or place the deployment in maintenance mode.
2. Provision an empty PostgreSQL database.
3. Restore the selected dump:

```bash
pg_restore --dbname "$DATABASE_URL" --clean --if-exists healthcare-hero-YYYYMMDD.dump
```

4. Start the app with `SPRING_PROFILES_ACTIVE=prod`.
5. Check `/actuator/health/readiness`.
6. Run smoke tests for auth, HL7 parse, workspace load, and code search.
