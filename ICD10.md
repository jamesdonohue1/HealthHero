# ICD-10 Search Module Requirements

## Product Overview

Build an ICD-10 diagnosis search module inside the larger healthcare SaaS platform that also includes the HL7 validation and decoding tool.

The ICD-10 module allows users to enter common/plain-English diagnosis text and receive ranked ICD-10 search results from a government ICD-10 API.

The module should support anonymous use initially, with future login, saving, history, teams, and API access aligned with the HL7 tool.

---

# Core Purpose

The tool should help users convert common diagnosis language into possible ICD-10-CM code matches.

Examples:

* “chest pain”
* “patient has chest pain and shortness of breath”
* “diabetes with kidney disease”
* “left ankle sprain initial encounter”
* pasted diagnosis lists
* paragraphs from clinical notes

The system must clearly communicate that results are suggestions and must be verified by an authorized medical coding professional.

---

# Module Placement

This feature will be part of the same healthcare SaaS platform as the HL7 tool.

It should be implemented as a module within the same backend application for MVP.

---

# Technology Stack

Use the same stack as the HL7 SaaS:

## Backend

* Java 21
* Spring Boot
* Maven
* Spring Web
* Spring Validation
* Spring Security ready
* PostgreSQL
* JPA/Hibernate
* Swagger/OpenAPI

## Frontend

* React
* TypeScript

## Infrastructure

* Docker
* Docker Compose
* GitHub Actions
* Environment-based configuration

---

# Authentication

Use the same authentication model as the HL7 tool.

## MVP

* Anonymous public access
* Anonymous rate limits

## Future

* Login
* User accounts
* Organization accounts
* Team workspaces
* API keys
* Role-based access

---

# Input Requirements

The page must provide a text input area that supports:

* Short phrases
* Full sentences
* Paragraphs
* Clinical note snippets
* Diagnosis lists
* Multiple diagnoses at once

The input area should support:

* Multiline text
* Clear button
* Submit/search button
* Optional sample inputs
* Autocomplete while typing

---

# Input Cleanup / Normalization

Before search, the system should clean and normalize user input.

Normalization should include:

* Trim whitespace
* Remove unnecessary punctuation
* Normalize casing
* Normalize common abbreviations where safe
* Split multiple diagnosis lines
* Detect multiple diagnosis concepts
* Remove filler words when helpful
* Preserve clinically meaningful terms

Example:

Input:

> Patient complains of chronic left knee pain and shortness of breath.

The system may identify:

* chronic left knee pain
* shortness of breath

---

# Government ICD-10 API Integration

The Java backend must call the government ICD-10 API.

The browser/frontend should not call the government API directly.

Reasons:

* Better security
* Centralized caching
* Centralized retry handling
* Rate limiting
* Logging control
* Future billing/API usage tracking

---

# Search Behavior

The backend should:

* Accept plain-English text
* Normalize the text
* Detect one or more diagnosis concepts
* Query the ICD-10 API
* Return ranked results
* Return up to 10 results by default
* Support configurable result count later
* Handle vague input gracefully
* Return “not enough information” when appropriate

---

# Multiple Diagnosis Support

The system must support multiple diagnoses in one request.

Example input:

> chest pain
> diabetes type 2
> left ankle sprain

The output should group results by detected diagnosis.

Each diagnosis group should contain its own ranked ICD-10 results.

---

# Result Display Requirements

Each ICD-10 result should display:

* ICD-10 code
* Short description
* Long description
* Score/rank
* Billable/non-billable indicator
* Chapter/category
* Match reason when available

Default number of results shown:

* 10

---

# Result Interaction

Users must be able to:

* Expand a result for more detail
* Select one or more ICD-10 codes
* Copy code
* Copy description
* Add result to selected-code panel
* Remove selected code
* Clear selected codes

---

# UI Layout

Preferred layout:

* Input box at top
* Search results below
* Selected-code panel on the right
* Optional search history sidebar
* Clear button

The UI should include:

* Search button
* Clear button
* Loading state
* Empty state
* Error state
* Results grouped by diagnosis concept
* Selected-code panel
* Export options
* Safety disclaimer

---

# Autocomplete

The input should support autocomplete while typing.

Autocomplete should suggest:

* ICD-10 descriptions
* Common diagnosis phrases
* Previously searched terms for logged-in users later

Autocomplete should be backend-powered when possible.

---

# Clarifying Questions

The tool should ask clarifying questions when diagnosis text is too vague or requires more specificity.

Clarifying question categories include:

* Left/right/bilateral
* Acute vs chronic
* Initial/subsequent/sequela encounter
* With or without complication
* Severity
* Cause or injury details
* Anatomical site
* Episode of care
* Etiology
* Manifestation

Example:

Input:

> ankle sprain

Possible clarifying questions:

* Which ankle: left, right, or unspecified?
* Is this the initial encounter, subsequent encounter, or sequela?
* Is the sprain affecting a specific ligament?

---

# “Not Enough Information” Behavior

When the input is too vague, the system should display:

* “Not enough information to confidently suggest a specific code.”
* Suggested clarifying questions
* Broader possible matches
* Explanation that coding requires clinical specificity

---

# AI-Assisted Features

AI assistance should be included as an advanced feature.

AI should help:

* Convert plain English into better ICD-10 search terms
* Detect multiple diagnoses from a paragraph
* Suggest clarifying questions
* Explain why specificity matters
* Summarize result differences
* Help users refine searches

AI must not:

* Present results as final medical advice
* Guarantee code correctness
* Replace certified coding review

---

# Safety / Disclaimer Requirements

The page must show a clear disclaimer.

Suggested disclaimer:

> ICD-10 results are suggestions only and may be incomplete or inaccurate. Always verify codes with official coding guidelines, payer requirements, and a certified medical coder or qualified healthcare professional.

The system should also warn:

* Do not submit PHI unless authorized
* Coding depends on clinical documentation
* Results are not medical advice
* Results are not billing advice

---

# Saving Requirements

Searches and results should be savable.

## Anonymous Users

* Searches are not saved automatically by default
* User may optionally save a search
* Saved anonymous searches expire after 24 hours

## Logged-In Users Later

Users should be able to save:

* Searches
* Selected code lists
* Search history
* Custom notes
* Exported reports

Saved logged-in data should remain until manually deleted.

---

# Future Logged-In Features

Future authenticated users should have:

* Saved searches
* Saved code lists
* Search history
* Team workspaces
* Audit history
* Custom notes
* API access

---

# Export Requirements

The module must support export in:

* CSV
* JSON
* PDF
* Plain text

Export should support:

* All results
* Selected codes only

Selected-code exports should be separate from full result exports.

---

# PDF Export Requirements

PDF exports should include:

* Search input
* Normalized search terms
* Timestamp
* Result groups
* Selected codes
* ICD-10 code
* Short description
* Long description
* Rank/score
* Billable indicator
* Disclaimer

---

# Backend API Endpoints

Suggested REST endpoints:

```text
POST /api/icd10/search
POST /api/icd10/autocomplete
POST /api/icd10/refine
POST /api/icd10/export/json
POST /api/icd10/export/csv
POST /api/icd10/export/pdf
POST /api/icd10/save
GET  /api/icd10/history
GET  /api/icd10/saved/{id}
DELETE /api/icd10/saved/{id}
```

---

# Backend Search Request Example

```json
{
  "inputText": "patient has chronic left knee pain and shortness of breath",
  "resultLimit": 10,
  "includeClarifyingQuestions": true,
  "includeAiRefinement": true
}
```

---

# Backend Search Response Example

```json
{
  "originalInput": "patient has chronic left knee pain and shortness of breath",
  "normalizedInput": "chronic left knee pain; shortness of breath",
  "diagnosisGroups": [
    {
      "diagnosisText": "chronic left knee pain",
      "needsMoreInformation": true,
      "clarifyingQuestions": [
        "Is the pain due to injury, arthritis, or another known condition?",
        "Is laterality left, right, bilateral, or unspecified?"
      ],
      "results": [
        {
          "code": "M25.562",
          "shortDescription": "Pain in left knee",
          "longDescription": "Pain in left knee",
          "rank": 1,
          "score": 0.97,
          "billable": true,
          "chapter": "Diseases of the musculoskeletal system and connective tissue"
        }
      ]
    }
  ]
}
```

---

# Caching Requirements

Search results should be cached.

Cache key should consider:

* Normalized search text
* Result limit
* API version
* Search mode

Cache should improve:

* Performance
* Government API reliability
* Rate-limit protection

---

# Retry Requirements

Failed API calls should retry automatically.

Retry behavior should include:

* Limited retry count
* Exponential backoff
* Timeout handling
* Graceful error response
* User-friendly error message

---

# Rate Limiting

Anonymous users must have rate limits.

Suggested limits:

* 20 searches per hour
* 100 autocomplete requests per hour
* Lower limits for export/report generation

Future authenticated plans may have higher limits.

---

# Error Handling

The system should handle:

* ICD-10 API unavailable
* Timeout
* Empty input
* Very large input
* Unsupported characters
* No results found
* Ambiguous diagnosis
* Internal server errors

User-facing errors should be clear and non-technical.

---

# Logging Requirements

Logs must not store PHI or full clinical text by default.

Allowed logging:

* Request timestamp
* Anonymous/session/user ID
* Status code
* API latency
* Error category
* Result count

Avoid logging:

* Full search text
* Patient identifiers
* Clinical note content

---

# Security Requirements

The module should follow the same security requirements as the HL7 tool:

* No PHI logging
* PHI warning
* Encryption for saved data
* Delete capability
* Secure session handling
* Future audit trail
* Environment-based secrets

---

# Future Code Set Support

Initial support:

* ICD-10-CM

Future support:

* ICD-10-PCS
* SNOMED CT
* CPT

The architecture should allow adding additional code systems later without rewriting the entire module.

---

# Data Model Suggestions

Suggested entities:

* IcdSearch
* IcdSearchGroup
* IcdSearchResult
* SelectedCode
* SavedCodeList
* SearchExport
* AuditLog

---

# OpenAPI / Swagger

The backend must provide Swagger/OpenAPI docs for all ICD-10 endpoints.

Docs should include:

* Request schemas
* Response schemas
* Error responses
* Example requests
* Example responses

---

# MVP Scope

Initial MVP should include:

1. ICD-10 search page inside existing SaaS
2. Plain-English input box
3. Backend-proxied government ICD-10 API call
4. Result normalization
5. Multiple diagnosis support
6. 10 ranked results per diagnosis
7. Result expansion
8. Selected-code panel
9. Export selected/all results
10. Save search/results
11. Disclaimers
12. Basic clarifying questions
13. Swagger API docs
14. Docker/GitHub Actions support

---

# Future Enhancements

Future roadmap:

* AI-powered search refinement
* AI-generated clarifying questions
* Saved user history
* Team workspaces
* API key access
* Batch diagnosis coding
* ICD-10-PCS
* SNOMED
* CPT
* Billing-rule integrations
* Payer-specific coding hints
* EHR integration
* HL7 diagnosis extraction from DG1 segments
* Link ICD-10 suggestions to HL7 validation results
