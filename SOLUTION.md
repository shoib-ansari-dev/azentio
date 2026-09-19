# Sentinel AML — What We Built and Why

A walkthrough of the Sentinel AML platform: every major decision, what it solves in the
problem statement, and why we did it that way. Backend lives in `app-server/`, the UI in
`frontend/`.

---

## 1. The problem, in one line

MeridianTrust Bank's transaction monitoring was *"manual, reactive, and dangerously slow"* —
analysts caught laundering 48–72 hours late from nightly spreadsheets. The CCO wanted a system
that **reliably catches known laundering typologies, explains *why* it flagged something, and
gives analysts a clean workflow to act on it** — not a perfect AI model.

Everything below is in service of those three goals: *catch*, *explain*, *action*.

---

## 2. Tech choices (and why)

| Choice | Why |
|---|---|
| **Java 17 + Spring Boot 4.1.1** | Mandated by the problem statement (Spring Web, Data JPA, Security). |
| **PostgreSQL 16** (Docker, port 5433) | Relational DB required; Postgres preferred. `jsonb` columns give us queryable audit values. |
| **Flyway migrations** (V1–V12) | Documented, versioned schema — a required deliverable. One table per migration keeps history readable. |
| **Constructor injection only** | No `@Autowired`/field injection anywhere. Makes every service unit-testable and spy-able without a Spring context. This is why the rule tests are pure Mockito, no `@SpringBootTest`. |
| **Stateless JWT + RBAC** | Security must be enforced at the API layer, not just the UI (NFR). Roles: `ANALYST`, `SUPERVISOR`, `ADMIN`. |
| **No try/catch in controllers** | Errors throw to a single `GlobalExceptionHandler` (`@RestControllerAdvice`) → consistent JSON error bodies with correct HTTP codes. |

---

## 3. Architecture at a glance

Layer-by-type packages under `com.customer.support.ai.appserver`:

```
controller/   REST endpoints (thin — validate, delegate, return)
service/      business logic + transactions
repository/   Spring Data JPA
entity/       JPA entities (Customer, Account, Transaction, Alert, AmlCase, ...)
dto/          request/response records
security/     JWT filter, config, user details
detection/    the detection engine (contract + impls)
detection/rules/  the 6 rule evaluators
config/       AsyncConfig (thread pools), OpenApiConfig
exception/    GlobalExceptionHandler
```

The `detection/` package is a **frozen interface contract** (`DetectionEngine`,
`RuleEvaluator`, `RuleContext`, `RuleHit`, `CurrencyConverter`). We froze it so the six rule
evaluators, the engine, and the ingestion layer could be built in parallel without stepping on
each other — each side coded against the interface, not the implementation.

---

## 4. What we built, mapped to the problem

### A) Data model & ingestion
- **Schema**: `Customer → Account → Transaction`, plus `Alert`, `AmlCase`, `DetectionRule`,
  `ExchangeRate`, `AuditLog`, `IngestionJob`, `AppUser`. 12 Flyway migrations.
- **Bulk ingest**: CSV upload endpoints for customers, accounts, transactions
  (`POST /api/v1/{customers|accounts|transactions}/bulk`). Parsed with opencsv by header name.
  Each upload creates an `IngestionJob` (QUEUED → RUNNING → COMPLETED/FAILED) processed
  **asynchronously** on a dedicated thread pool, tracking processed/failed counts and an error
  summary. Analysts poll `GET /api/v1/jobs/{jobId}`.
- **Single/streaming ingest**: `POST /api/v1/transactions` ingests one transaction and runs
  detection **synchronously**, returning the transaction plus any alerts it raised in one
  response. This is the "REST endpoint for continuously arriving transactions" the spec asked
  for (in place of Kafka).
- **Validation & referential integrity**: bean validation on DTOs; foreign keys resolved by
  reference (e.g. `customer_id` in the CSV → looked up by `customerRef`). Malformed/duplicate
  rows are counted as failures with a reason, never silently dropped.

**Why async for bulk, sync for single:** a 10k-row bulk load must not block the HTTP thread
(NFR: 10k transactions under 2 minutes), so it runs on a pool and returns a job id immediately.
A single streaming transaction wants its alerts *now*, so it runs inline for sub-second feedback.

### B) Detection engine — *catch* and *explain*
Six configurable rules, all seeded in the `detection_rule` table (thresholds/windows are DB
config, tunable **without redeploy** — a hard requirement):

| Rule | Business rule | Risk weight |
|---|---|---|
| `CTR_THRESHOLD` | Single txn ≥ threshold (CTR-style) | 80 |
| `STRUCTURING` | ≥3 txns just under threshold in 24h | 90 |
| `RAPID_MOVEMENT` | ≥80% of an inflow moved out within window (layering) | 70 |
| `HIGH_RISK_JURISDICTION` | Counterparty on the high-risk list, any amount | 85 |
| `BEHAVIORAL_DEVIATION` | Volume exceeds customer's rolling baseline | 60 |
| `ROUND_NUMBER` | Suspiciously round amounts | 40 |

- **How it runs**: `DetectionEngine.evaluate(txn)` fans out over all enabled evaluators
  concurrently (`CompletableFuture` on a detection thread pool), then joins. Each evaluator
  returns an `Optional<RuleHit>` carrying `ruleCode`, `riskWeight`, a **human-readable
  explanation**, and the **evidence transaction ids**. That explanation + evidence is what makes
  every alert defensible to a regulator — directly answering the CCO's "explain WHY."
- **Risk score**: taken from the triggering rule's configured weight, capped 0–100, so
  higher-risk alerts sort to the top of the analyst queue.
- **Alert de-duplication**: enforced by a Postgres **partial unique index** on
  `(account_id, rule_code) WHERE status = 'OPEN'`. When a rule fires again for an account that
  already has an open alert of that type, we **merge** (union the evidence, keep the max risk,
  bump `updated_at`) instead of creating a 51st redundant alert. Verified live: a second large
  transaction grew an existing alert's evidence from 1 → 2 rather than duplicating it.
- **Currency normalization**: every amount is converted to **INR** at ingest via a configurable
  `exchange_rate` table; we persist both `amount_inr` and the `exchange_rate_used`. All rules
  compare on `amount_inr`, so cross-currency patterns are detectable.

### C) Case management & audit — *action* and *compliance*
- Alerts flow through `acknowledge` / `dismiss` / `escalate`; escalation opens an `AmlCase`,
  which an analyst can `close` with a disposition.
- **Alerts are never deleted.** Cleared alerts stay in the system with disposition reason and
  the analyst's identity — the spec is explicit about this for auditability.
- **Immutable audit trail**: every alert/case state transition writes an `AuditLog` row
  (entity, action, actor, timestamp) **asynchronously** on an audit thread pool, so auditing
  never slows the request path.

---

## 5. Concurrency model

Three separate `ThreadPoolTaskExecutor` beans (`config/AsyncConfig.java`), sized via
`sentinel.thread-pool.*` config:

| Pool | Used for | Why separate |
|---|---|---|
| `ingestionExecutor` | bulk CSV jobs | Long-running batch work must not starve detection. |
| `detectionExecutor` | rule fan-out per transaction | The hot path; isolated so a slow audit or bulk job can't stall it. |
| `auditExecutor` | fire-and-forget audit writes | Auditing is best-effort background work; it must never block or fail a business operation. |

Isolating the pools is what lets the system stay "thread-safe … without duplicate/lost alerts"
under concurrent streams (the dedup index is the other half of that guarantee).

---

## 6. Security & API standards
- **JWT bearer** auth; RBAC enforced with `@PreAuthorize` at the controller layer (not the UI).
  Example proven live: `admin` can POST transactions but gets **403** on `GET /customers`
  (that's ANALYST/SUPERVISOR) — RBAC is real, not cosmetic.
- Secrets (JWT secret, DB password) come from config/env, never hardcoded.
- **OpenAPI/Swagger**: `springdoc` serves the full spec at `/v3/api-docs` (23 documented paths)
  and interactive UI at `/swagger-ui.html`.
- Consistent JSON errors via `GlobalExceptionHandler` with proper HTTP codes
  (400/401/403/404/409/422/500).

---

## 7. Testing
- Unit tests for the six rule evaluators (JUnit 5 + Mockito + AssertJ), covering fire /
  no-fire / boundary cases per rule. Pure unit tests — no Spring context — enabled by the
  constructor-injection-only rule. This satisfies the "unit tests for detection rule logic"
  deliverable.

---

## 8. A notable bug we found and fixed (and how)

**Symptom:** alerts were being created correctly, but `audit_log` stayed empty — the immutable
audit trail (a scored NFR) was silently not working.

**Root cause:** the audit `old_value`/`new_value` columns are Postgres `jsonb`, but the caller
passed a bare string like `ALRT_faf6769d`. Postgres rejects an unquoted bare string as invalid
JSON. Because the audit write is **fire-and-forget on a background pool**, the exception was
swallowed and never surfaced to the request — so the alert looked fine while the audit row
vanished.

**Why it survived compilation:** it's a runtime data-type mismatch (`jsonb` vs raw string), not
a type error the compiler can see. It only appears when you actually run the path against a real
database — which is exactly why we ran a live end-to-end verification rather than trusting
"BUILD SUCCESS."

**Fix:** serialize the value to valid JSON at the `AuditService.log` boundary (a bare string
becomes a JSON-quoted string). Keeps the column `jsonb` (still queryable) and keeps every caller
simple. Verified live: the audit row now lands as `"ALRT_faf6769d"`.

**Lesson:** compile-clean ≠ works. Fire-and-forget code hides failures — verify it against a
running system.

---

## 9. How to run it

```bash
cd app-server
docker compose up -d            # Postgres 16 on localhost:5433
./mvnw spring-boot:run          # app on localhost:8099, Flyway applies V1–V12 + seeds

# log in
curl -X POST localhost:8099/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}'

# Swagger UI
open http://localhost:8099/swagger-ui.html
```

Seed users: `admin/admin` (ADMIN), `supervisor/supervisor` (SUPERVISOR), `analyst/analyst`
(ANALYST). Detection rules and exchange rates are seeded by Flyway.
