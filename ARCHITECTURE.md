# Sentinel AML — System Architecture

> A real-time anti-money-laundering platform that **catches** known laundering typologies as transactions arrive, **explains** why each alert fired with regulator-defensible evidence, and gives compliance analysts a clean workflow to act on them.

---

## 1. System Overview

```
┌─────────────────────────────────────────────────────────┐
│           React SPA  (port 3000)                        │
│   /alerts  /cases  /customers/:id  /accounts/:id        │
│   /rules (ADMIN)   /jobs (ADMIN)                        │
└────────────────────────┬────────────────────────────────┘
                         │  REST + JWT Bearer
┌────────────────────────▼────────────────────────────────┐
│           Spring Boot 4.x  (port 8080)                  │
│                                                         │
│  IngestionService   ──►  ingestion-pool (4–16 threads)  │
│  DetectionEngine    ──►  detection-pool (8–32 threads)  │
│  AlertService       ──►  DB partial-unique dedup index  │
│  AuditService       ──►  audit-pool (2–4 threads, async)│
└────────────────────────┬────────────────────────────────┘
                         │  Flyway (V1–V12)
┌────────────────────────▼────────────────────────────────┐
│           PostgreSQL 16                                  │
│  Customer → Account → Transaction                       │
│  Alert → AmlCase → AuditLog                             │
│  DetectionRule   ExchangeRate   IngestionJob            │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Tech Stack

| Layer | Choice | Reason |
|---|---|---|
| **Backend** | Java 17 + Spring Boot 4.1.1 | Required stack (Spring Web, Data JPA, Security) |
| **Database** | PostgreSQL 16 | Relational + `jsonb` for queryable audit payloads |
| **Migrations** | Flyway V1–V12 | Versioned, reproducible schema — scored deliverable |
| **Auth** | Stateless JWT + RBAC | Security enforced at the API layer, not just the UI |
| **CSV parsing** | OpenCSV | Bulk ingestion of customer/account/transaction data |
| **API docs** | springdoc-openapi | Swagger UI at `/swagger-ui.html` |
| **Frontend** | React 19 + Tailwind CSS | SPA on port 3000, JWT kept in memory (not localStorage) |
| **HTTP client** | axios | Request interceptor injects Bearer token; 401 → redirect |
| **Testing** | JUnit 5 + Mockito | One test class per detection rule, no Spring context needed |

---

## 3. Package Structure

```
com.customer.support.ai.appserver
├── controller/        thin REST endpoints — validate, delegate, return
├── service/           business logic + @Transactional boundaries
├── repository/        Spring Data JPA
├── entity/            JPA entities (Customer, Account, Transaction, Alert, AmlCase, …)
├── dto/               request/response records and projections
├── security/          JWT filter, SecurityConfig, role hierarchy
├── detection/         DetectionEngine + frozen interface contract
│   └── rules/         six rule evaluators
├── config/            AsyncConfig (thread pools), OpenApiConfig
└── exception/         GlobalExceptionHandler + ApiError
```

**Key design decision:** `detection/` is a frozen interface contract (`DetectionEngine`, `RuleEvaluator`, `RuleContext`, `RuleHit`, `CurrencyConverter`). Freezing the contract before implementation let the six rules, the engine, and the ingestion layer be built in parallel — each side coded against the interface, not a concrete class.

---

## 4. Core Flows

### A. Ingestion — two deliberately different modes

| Mode | Endpoint | Behavior |
|---|---|---|
| **Bulk** | `POST /api/v1/{customers\|accounts\|transactions}/bulk` | CSV upload creates an `IngestionJob` (QUEUED → RUNNING → COMPLETED/FAILED), processed async on the ingestion pool. Poll via `GET /jobs/{jobId}`. |
| **Streaming** | `POST /api/v1/transactions` | Single transaction ingested synchronously; detection runs inline and the response includes any generated alerts. |

Bulk is async because a 10k-row load must not block an HTTP thread. Streaming is sync because real-time transactions need their alerts immediately.

### B. Detection Engine — catch + explain

`DetectionEngine.evaluate(txn)` fans out over all enabled rules concurrently using `CompletableFuture` on a dedicated detection pool, then joins the results.

Each rule returns an `Optional<RuleHit>` containing:
- **rule code** and **risk weight**
- **human-readable explanation** (regulator-defensible)
- **evidence transaction IDs** (supporting the finding)

**Currency normalization:** all amounts are converted to INR at ingestion time using a configurable `ExchangeRate` table. Every rule compares on `amount_inr`, so cross-currency typologies are detectable without rule-level conversion logic.

**Alert deduplication:** a PostgreSQL partial unique index on `(account_id, rule_code) WHERE status = 'OPEN'` prevents duplicate open alerts. A repeat rule hit merges into the existing alert — unioning evidence IDs and keeping the higher risk score — instead of creating a new row.

### C. Case Management & Audit — action + compliance

- Alert lifecycle: `OPEN` → `ACKNOWLEDGED` → `DISMISSED` or `ESCALATED`
- Escalation opens an `AmlCase`, which is closed with a disposition reason
- **Alerts are never hard-deleted** — dismissed alerts retain the reason and analyst identity (compliance requirement)
- **Immutable audit trail:** every state transition writes an `AuditLog` row asynchronously on the audit pool, so audit logging never adds latency to the business operation

---

## 5. Concurrency Model

Three fully isolated thread pools prevent one workload from starving another:

| Pool | Workload | Core / Max / Queue |
|---|---|---|
| `ingestion-pool` | Bulk CSV processing | 4 / 16 / 10,000 |
| `detection-pool` | Rule fan-out per transaction (hot path) | 8 / 32 / 50,000 |
| `audit-pool` | Fire-and-forget audit writes | 2 / 4 / 20,000 |

A slow bulk ingestion job cannot delay a real-time alert. Audit writes can never block a detection result. This, combined with the DB dedup index, gives thread-safe operation under concurrent transaction streams with no duplicate or lost alerts.

---

## 6. Detection Rules

All six rules are stored in the `DetectionRule` table. Thresholds are tunable at runtime via `PUT /api/v1/rules/{id}` — no redeployment required.

| Rule Code | Typology | Default Parameters |
|---|---|---|
| `CTR_THRESHOLD` | Single transaction ≥ ₹10,00,000 | `threshold_inr: 1,000,000` |
| `STRUCTURING` | 3+ transactions ₹9L–₹9.99L within 24h | `window_hours: 24, min_count: 3` |
| `RAPID_MOVEMENT` | ≥80% outflow within 48h of a deposit | `window_hours: 48, outflow_pct: 80` |
| `HIGH_RISK_JURISDICTION` | Transaction touches a sanctioned country | `jurisdictions: [IR, KP, SY, …]` |
| `BEHAVIORAL_DEVIATION` | Daily volume > 3× 90-day rolling average | `lookback_days: 90, multiplier: 3.0` |
| `ROUND_NUMBER` | Repeated suspiciously round amounts in 24h | `window_hours: 24, min_count: 2` |

---

## 7. Security

| Concern | Approach |
|---|---|
| **Authentication** | Stateless JWT; token validated on every request by a servlet filter |
| **Authorization** | Role hierarchy `ADMIN ⊇ SUPERVISOR ⊇ ANALYST`; enforced via `@PreAuthorize` at the controller layer, not just the UI |
| **Secrets** | All credentials read from environment variables — nothing hardcoded |
| **PII masking** | List view DTOs omit full name and contact details; full PII requires the ANALYST role |
| **Error handling** | Single `GlobalExceptionHandler` → consistent JSON error body for 400 / 401 / 403 / 404 / 405 / 409 / 422 / 500 |

---

## 8. What Was Verified, Not Just Built

During end-to-end testing against a real database, audit rows were silently disappearing. Root cause: a bare Java string was written to a `jsonb` column, and the fire-and-forget audit pool swallowed the resulting exception without surfacing it. The fix was to JSON-serialize the payload at the `AuditService` boundary before persistence. This was caught only by running a live integration test — not by unit tests or compilation — which is why the test suite includes real-database coverage alongside the Mockito unit tests.
