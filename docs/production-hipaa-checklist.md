# Production HIPAA Checklist

Healthcare Hero is not production HIPAA-ready by deployment alone. Before processing real PHI, the operator must complete and own the following controls.

## Required Controls

- Execute BAAs with all infrastructure, hosting, logging, monitoring, email, analytics, AI, and support vendors that can access PHI.
- Set `ENCRYPTION_KEY`, `JWT_SECRET`, database credentials, and TLS certificates from a managed secrets system.
- Rotate encryption by deploying a new `app.encryption-key-id` and key material, re-encrypting saved records, then retiring old key material after verification.
- Disable request/response payload logging at ingress, proxy, application, database, and error-tracking layers.
- Keep application logs limited to event metadata, record IDs, counts, status, and redaction flags.
- Restrict audit export, retention settings, and administration to organization or platform admins.
- Configure retention settings per organization and run retention cleanup as an operational job.
- Back up encrypted databases, test restore procedures, and document restore access approvals.
- Enforce least-privilege access for database, cloud console, runtime shell, and production logs.
- Confirm TLS, HSTS, secure cookies, and private network rules in the deployed environment.
- Review generated coding suggestions before any billing, clinical, or compliance use.

## Deployment Guidance

- Use a managed PostgreSQL service with encryption at rest and point-in-time recovery.
- Put the application behind a TLS-terminating load balancer or reverse proxy.
- Send logs to a PHI-safe sink with payload capture disabled.
- Monitor audit export, delete, admin, login failure, and retention-apply events.
- Run dependency and container scanning before every production release.
- Treat AI integrations as disabled until PHI masking, vendor BAA, and audit controls are verified.
