# Code Import Management

Phase 3 adds admin-managed imports for ICD-10-CM, HCPCS, licensed CPT, payer rules, LCD/NCD policy sources, and synonym dictionaries.

## Admin UI

Open the CPT/HCPCS module and use the Code Imports panel. Admins can paste CSV rows, select the import type and version, run the import, view recent jobs, and roll back a job. Rollback removes rows tied to that import job.

## Supported Import Types

- `ICD10_CM`: `code,shortDescription,longDescription,chapter,effectiveDate,terminationDate,active,replacementCode,source,provenance,confidence`
- `HCPCS`: `code,shortDescription,longDescription,category,effectiveDate,terminationDate,active,source,replacementCode,provenance,confidence`
- `CPT`: same format as `HCPCS`; production use requires an authorized licensed CPT source.
- `PAYER_RULE`: `icd10Code,cptCode,payer,ruleType,status,notes,source,effectiveDate,expirationDate,provenance,confidence`
- `LCD_NCD_POLICY`: `payer,policyType,policyId,title,url,source,effectiveDate,expirationDate`
- `SYNONYM`: `codeType,term,synonyms`

Blank lines and lines starting with `#` are ignored. Dates use ISO format, for example `2026-10-01`.

## Versioning and Provenance

Each import stores a code-set version, source name, provenance text, confidence score, effective dates, and active/retired state where applicable. Search results surface retired/deleted status through the existing active flag and include version, replacement, and provenance details in the source field.

## Search and Rules

Imported ICD-10-CM rows are used by local ICD fallback search. Imported HCPCS/CPT rows are available to the procedure search with effective-date filtering. Imported payer rules participate in ICD/CPT compatibility checks and contribute rule notes, provenance, and confidence.

## Rollback

Recent import jobs are available through `GET /api/admin/imports`. Use `POST /api/admin/imports/{id}/rollback` or the UI rollback button to remove rows imported by a job.
