# Sentinel AML — Architecture Walkthrough

Organized the way you'd walk a hackathon evaluator through it.

## 1. The one-line pitch

> A real-time anti-money-laundering platform that **catches** known laundering typologies as transactions arrive, **explains** *why* each alert fired (regulator-defensible), and gives analysts a clean workflow to **act** on them.

Everything in the design serves those three verbs: **catch · explain · action**.

## 2. Tech stack (and the "why")

| Choice | Why it's there |
|---|---|
| **Java 17 + Spring Boot 4.1.1** | Mandated by the problem statement (Spring Web, Data JPA, Security). |
| **PostgreSQL 16** (Docker, port 5433) | Relational DB required. `jsonb` columns give a queryable audit trail. |
| **Flyway (V1–V12)** | Versioned, documented schema — a scored deliverable. |
| **Stateless JWT + RBAC** | Security enforced at the API layer, not just the UI (NFR). Roles: ANALYST, SUPERVISOR, ADMIN. |
| **Constructor injection only** | Every service is unit-testable with pure Mockito — no Spring context needed. |
| **No try/catch in controllers** | All errors throw to one `GlobalExceptionHandler` → consistent JSON error bodies + correct HTTP codes. |
| **React + JavaScript frontend** | Separate SPA on port 3000, proxies to the backend on 8099. |

## 3. Layered architecture

Packages under `com.customer.support.ai.appserver`, split by type:

```
controller/       thin REST endpoints — validate, delegate, return
service/          business logic + @Transactional boundaries
repository/       Spring Data JPA
entity/           JPA entities (Customer, Account, Transaction, Alert, AmlCase, …)
dto/              request/response records
security/         JWT filter, SecurityConfig, user details
detection/        the detection engine (frozen interface contract)
detection/rules/  the 6 rule evaluators
config/           AsyncConfig (thread pools), OpenApiConfig
exception/        GlobalExceptionHandler + ApiError
```

**Key design move:** `detection/` is a **frozen interface contract** (`DetectionEngine`, `RuleEvaluator`, `RuleContext`, `RuleHit`, `CurrencyConverter`). Freezing it let the 6 rules, the engine, and the ingestion layer be built **in parallel** — each side coded against the interface, not the implementation.

## 4. The three flows, mapped to the problem

### A) Ingestion — two modes, deliberately different

- **Bulk** (`POST /api/v1/{customers|accounts|transactions}/bulk`): CSV upload → creates an `IngestionJob` (QUEUED → RUNNING → COMPLETED/FAILED), processed **asynchronously** on a dedicated pool. Polled via `GET /api/v1/jobs/{jobId}`, or listed via `GET /api/v1/jobs`. *Why async:* a 10k-row load must not block the HTTP thread (NFR: 10k txns < 2 min).
- **Single/streaming** (`POST /api/v1/transactions`): ingests one transaction and runs detection **synchronously**, returning the transaction + any alerts in one response. *Why sync:* streaming transactions want their alerts *now*. This is the "endpoint for continuously arriving transactions" (in place of Kafka).

### B) Detection engine — catch + explain

- Six DB-configurable rules (thresholds tunable **without redeploy**): CTR_THRESHOLD, STRUCTURING, RAPID_MOVEMENT, HIGH_RISK_JURISDICTION, BEHAVIORAL_DEVIATION, ROUND_NUMBER.
- `DetectionEngine.evaluate(txn)` **fans out over all enabled rules concurrently** (`CompletableFuture` on a detection pool), then joins. Each rule returns an `Optional<RuleHit>` carrying rule code, risk weight, a **human-readable explanation**, and **evidence transaction ids** — that's the "explain WHY" for regulators.
- **Currency normalization:** every amount converted to **INR** at ingest; rules compare on `amount_inr`, so cross-currency patterns are catchable.
- **Alert de-duplication:** a Postgres **partial unique index** on `(account_id, rule_code) WHERE status='OPEN'`. A repeat hit **merges** into the existing alert (union evidence, keep max risk) instead of spawning duplicates.

### C) Case management & audit — action + compliance

- Alerts flow `acknowledge` / `dismiss` / `escalate`; escalation opens an `AmlCase`, closed with a disposition.
- **Alerts are never deleted** — cleared ones stay with reason + analyst identity (spec requirement).
- **Immutable audit trail:** every state transition writes an `AuditLog` row **asynchronously** on an audit pool, so auditing never slows the request path.

## 5. Concurrency model — three isolated thread pools

| Pool | Job | Why isolated |
|---|---|---|
| `ingestionExecutor` | bulk CSV | long batch work must not starve detection |
| `detectionExecutor` | rule fan-out (hot path) | a slow audit/bulk job can't stall alerts |
| `auditExecutor` | fire-and-forget audit writes | best-effort background, never blocks a business op |

Pool isolation + the dedup index together give "thread-safe under concurrent streams, no duplicate/lost alerts."

## 6. Security & standards

- JWT bearer auth; RBAC via `@PreAuthorize` at the controller layer. Role hierarchy: **ADMIN ⊇ SUPERVISOR ⊇ ANALYST**.
- Secrets from config/env, never hardcoded.
- OpenAPI/Swagger at `/swagger-ui.html`.
- One `GlobalExceptionHandler` → 400 / 401 / 403 / 404 / 405 / 409 / 422 / 500.

---

## A strong closing line for the evaluators

> **"Compile-clean ≠ works."** We caught a real runtime bug — audit rows silently vanished because a bare string was written to a `jsonb` column and the fire-and-forget pool swallowed the exception. We only found it by running a **live end-to-end test against a real database**, and fixed it by JSON-serializing at the `AuditService` boundary. That story shows the evaluators you *verified*, not just built.
