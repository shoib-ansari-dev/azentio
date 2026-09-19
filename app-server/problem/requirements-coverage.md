# Sentinel AML — Requirements Coverage

## Functional Requirements

| # | Requirement | How we cover it | Status |
|---|-------------|-----------------|--------|
| FR-1 | Customer / Account / Transaction schema with proper relationships | `Customer → Account → Transaction` FK chain; see data-model.md | ✅ Designed |
| FR-2 | REST ingestion endpoints for KYC, accounts, transactions | POST `/customers/bulk`, `/accounts/bulk`, `/transactions` (single) + `/transactions/bulk` | ✅ Designed |
| FR-3 | Data validation — reject malformed records, referential integrity | Spring Validation (`@Valid`), FK constraints, error log in IngestionJob | ✅ Designed |
| FR-4 | Streaming / incremental ingestion (continuous transactions) | POST `/transactions` for single; async bulk via IngestionJob + thread pool | ✅ Designed |
| FR-5 | Structuring / Smurfing detection | `STRUCTURING` rule — 3+ txns $9K–$9,999 in 24h window | ✅ Designed |
| FR-6 | Rapid Movement of Funds detection | `RAPID_MOVEMENT` rule — ≥80% outflow within 48h | ✅ Designed |
| FR-7 | High-Risk Jurisdiction detection | `HIGH_RISK_JURISDICTION` rule — configurable country list in JSONB | ✅ Designed |
| FR-8 | Behavioral Deviation detection | `BEHAVIORAL_DEVIATION` rule — 3× 90-day rolling average | ✅ Designed |
| FR-9 | Round-Number / Just-Below-Threshold patterns | `ROUND_NUMBER` rule — configurable thresholds | ✅ Designed |
| FR-10 | Each alert must carry risk score + triggered rules + evidence + explanation | `Alert.risk_score`, `Alert.rule_code`, `Alert.evidence_txn_ids`, `Alert.explanation` | ✅ Designed |
| FR-11 | Rules configurable without redeployment | `DetectionRule` table with JSONB parameters; PUT `/rules/{id}` | ✅ Designed |
| FR-12 | Alert deduplication | Unique partial index on `(account_id, rule_code)` WHERE `status = 'OPEN'` | ✅ Designed |
| FR-13 | Case management workflow | `Case` entity + alert escalation flow; PUT `/alerts/{id}/escalate` | ✅ Designed |

### Business Rules

| # | Rule | Implementation |
|---|------|----------------|
| BR-1 | Single txn ≥ ₹10,00,000 auto-flagged (CTR) | `CTR_THRESHOLD` detection rule; fires synchronously on ingest |
| BR-2 | 3+ txns ₹9L–₹9.99L in 24h → Structuring | `STRUCTURING` rule query over `(account_id, transaction_timestamp)` index |
| BR-3 | ≥80% outflow within 48h → Rapid Movement | `RAPID_MOVEMENT` rule compares deposit vs. outflow in window |
| BR-4 | High-risk/sanctioned jurisdiction → always alert | `HIGH_RISK_JURISDICTION` rule; country list in `DetectionRule.parameters` |
| BR-5 | Daily volume > 3× 90-day rolling avg → Behavioral Deviation | `BEHAVIORAL_DEVIATION` rule; rolling avg stored/updated on account |
| BR-6 | Alerts never silently deleted | No DELETE on alerts; dismiss sets status + disposition_reason + actor |
| BR-7 | Risk score 0–100, sortable | `Alert.risk_score` = weighted sum of triggered rules' `risk_weight`; index on score |
| BR-8 | PII masked in list views | Service-layer DTO projection masks name/email/phone; detail endpoint requires ANALYST role |
| BR-9 | Amounts normalized to INR | `Transaction.amount_inr` computed at ingest using `ExchangeRate` table |

---

## Non-Functional Requirements

| # | Requirement | Design decision |
|---|-------------|-----------------|
| NFR-1 | Java 17+, Spring Boot | Java 17, Spring Boot 4.x — already in pom.xml |
| NFR-2 | PostgreSQL | Configured in application.yaml; Flyway migrations |
| NFR-3 | 10,000 txns bulk < 2 min | Async thread pool for bulk ingestion (see concurrency design below) |
| NFR-4 | Single streaming txn sub-second | Synchronous detection path; no DB round-trips beyond indexed queries |
| NFR-5 | Thread-safe, no duplicate/lost alerts | DB-level unique partial index prevents duplicates; `@Transactional` on detection |
| NFR-6 | No hardcoded secrets | application.yaml reads from env vars (`${DB_PASSWORD}` etc.) |
| NFR-7 | RBAC at API layer | Spring Security with JWT; roles ANALYST / SUPERVISOR / ADMIN on each endpoint |
| NFR-8 | Proper HTTP status codes + input validation | `@Valid`, `@ExceptionHandler`, standard error response DTO |
| NFR-9 | Versioned RESTful API + OpenAPI | Base path `/api/v1/...`; springdoc-openapi dependency |
| NFR-10 | Immutable audit log | `AuditLog` table — append only; interceptor writes on every state change |
| NFR-11 | Layered architecture | `controller / service / repository` packages; no logic in controllers |
| NFR-12 | Unit tests for detection rules | JUnit 5 + Mockito; one test class per rule |
| NFR-13 | SLF4J logging | Logback via Spring Boot; structured log on every rule trigger |
| NFR-14 | Synthetic seed data only | Flyway seed scripts with generated customers/accounts/transactions |

---

## Concurrency & Async Design

### Thread Pools (configured in application.yaml)

| Pool | Purpose | Core | Max | Queue |
|------|---------|------|-----|-------|
| `ingestion-pool` | Bulk CSV / batch ingestion workers | 4 | 16 | 10,000 |
| `detection-pool` | Rule evaluation per transaction | 8 | 32 | 50,000 |
| `audit-pool` | Async audit log writes | 2 | 4 | 20,000 |

### Flow

```
POST /transactions/bulk
        │
        ▼
IngestionService  ──async──►  ingestion-pool
  (parses + validates)              │
        │                           ▼
        │                 TransactionRepository.save()
        │                           │
        │                    detection-pool (CompletableFuture)
        │                    ├── CTR_THRESHOLD rule
        │                    ├── STRUCTURING rule
        │                    ├── RAPID_MOVEMENT rule
        │                    ├── HIGH_RISK_JURISDICTION rule
        │                    ├── BEHAVIORAL_DEVIATION rule
        │                    └── ROUND_NUMBER rule
        │                           │
        │                    AlertService.createOrMerge()  ◄── dedup check
        │                           │
        │                    audit-pool (fire-and-forget)
        ▼
Returns JobId immediately;  caller polls GET /jobs/{jobId}

POST /transactions  (single, streaming)
        │
        ▼ synchronous path (same thread)
  validate → save → detect (all rules, indexed queries) → alert → return 201
  target: < 200ms p95
```

### Duplicate Alert Prevention

- DB unique partial index: `(account_id, rule_code) WHERE status = 'OPEN'`
- `AlertService.createOrMerge()` runs inside `@Transactional(isolation = SERIALIZABLE)` for the alert upsert
- No in-memory locking needed — the DB constraint is the single source of truth

### @Async Boundaries

| Method | Executor | Notes |
|--------|----------|-------|
| `BulkIngestionService.processFile()` | `ingestion-pool` | returns `CompletableFuture<JobResult>` |
| `DetectionEngine.evaluate()` | `detection-pool` | called per-transaction; `CompletableFuture.allOf()` for rule fan-out |
| `AuditService.log()` | `audit-pool` | fire-and-forget; never blocks business flow |

---

## Dependencies to Add (pom.xml)

| Artifact | Purpose |
|----------|---------|
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI / Swagger UI |
| `spring-boot-starter-security` | JWT-based RBAC |
| `jjwt-api` + `jjwt-impl` + `jjwt-jackson` | JWT token handling |
| `opencsv` | CSV parsing for bulk ingestion |
| `spring-boot-starter-test` | JUnit 5 + Mockito (already implied, make explicit) |

---

## What is NOT in scope (this prototype)

- Kafka streaming pipeline (planned as next phase)
- ML anomaly scoring
- Graph visualization
- SAR auto-generation
- UI login screen (backend JWT/RBAC is in scope; UI uses a dev token)
