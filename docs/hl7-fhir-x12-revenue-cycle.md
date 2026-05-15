# HL7/FHIR/X12 and Revenue Cycle Tools

Phase 5 and Phase 6 add deeper interface, FHIR, X12, eligibility, and revenue-cycle capabilities.

## HL7 and FHIR

- `POST /api/platform/hl7/profile-validate`: configurable profile-style validation for ADT, ORM, ORU, SIU, DFT, MDM, VXU, ACK, and custom messages.
- `POST /api/platform/hl7/deep-analysis`: advanced HL7 review including ACK/NACK status, message sequencing, inline issue locations, DG1 diagnosis extraction, OBX lab extraction, and side-by-side repair readiness metadata.
- `POST /api/platform/fhir/validate`: structural FHIR R4-style validation using local JSON resource/profile checks.
- `POST /api/platform/fhir/mapping-template`: configurable HL7/FHIR mapping template metadata.
- `POST /api/platform/fhir/batch-hl7-to-fhir`: batch HL7-to-FHIR conversion for multiple `MSH|` messages.

The Platform Tools UI exposes these controls from the HL7 Repair and HL7 to FHIR panels.

## X12 and Revenue Cycle

- `POST /api/platform/x12/decode`: expanded X12 parser with transaction profile and required-loop checks.
- `POST /api/platform/x12/revenue-cycle`: 837/835/270/271 summary with required-loop issues, claim/payment/eligibility extraction, denial adjustment detection, and claim readiness score.
- `POST /api/platform/x12/generate-270`: creates a 270 eligibility inquiry for a member ID.
- `POST /api/platform/eligibility/analyze`: eligibility helper with generated 270 and decoded 271 benefit hints.
- `POST /api/platform/denials/analyze`: denial trend/root-cause summary and appeal packet checklist.
- `POST /api/platform/prior-auth/analyze`: prior authorization packet builder with documentation checklist.
- `POST /api/platform/payer/requirements`: payer requirement summary backed by imported payer rules and policy source tracking.
- `POST /api/platform/necessity/check`: medical necessity review for CPT/ICD/payer combinations.

The Platform Tools UI exposes 270 generation and revenue-cycle analysis from the X12 Decoder panel, and payer requirements from Roadmap Engines.

## Limits

These are local deterministic workflow tools. They do not replace payer portals, certified coding review, licensed CPT datasets, or production-grade FHIR conformance servers.
