# Sentinel AML — Real-Time Money Laundering Detection Platform

A full-stack Anti-Money Laundering (AML) platform built for MeridianTrust Bank. Ingests customer, account, and transaction data, applies configurable detection rules, and surfaces prioritized alerts to compliance analysts in near real-time.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  React Frontend (Tailwind CSS, react-router-dom v6)     │
│  /alerts  /cases  /customers/:id  /accounts/:id         │
│  /rules (ADMIN)   /jobs (ADMIN)                         │
└────────────────────────┬────────────────────────────────┘
                         │ REST (JWT Bearer)
┌────────────────────────▼────────────────────────────────┐
│  Spring Boot 4.x — /api/v1/...                          │
│                                                         │
│  Controllers → Services → Repositories                  │
│                                                         │
│  IngestionService  (ingestion-pool: 4–16 threads)       │
│  DetectionEngine   (detection-pool: 8–32 threads)       │
│  AlertService      (dedup via DB partial unique index)  │
│  AuditService      (audit-pool: 2–4 threads, async)     │
└────────────────────────┬────────────────────────────────┘
                         │ Flyway migrations
┌────────────────────────▼────────────────────────────────┐
│  PostgreSQL                                             │
│  Customer → Account → Transaction                       │
│  Alert → Case → AuditLog                                │
│  DetectionRule  ExchangeRate  IngestionJob              │
└─────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
azentio/
├── app-server/          — Spring Boot backend
│   └── problem/         — Requirements, data model, API design docs
└── frontend/            — React frontend
    └── problem-design/  — UI context and design specs
```

---

## Backend

### Technology Stack

| Component | Choice |
|-----------|--------|
| Language | Java 17+ |
| Framework | Spring Boot 4.x (Web, Security, Data JPA) |
| Build | Maven |
| Database | PostgreSQL |
| Migrations | Flyway |
| Auth | JWT (JJWT library) |
| API docs | springdoc-openapi (Swagger UI) |
| CSV parsing | OpenCSV |
| Testing | JUnit 5 + Mockito |

### Setup

**Prerequisites:** Java 17+, Maven, PostgreSQL running locally.

1. Create a database:
   ```sql
   CREATE DATABASE sentinel_aml;
   ```

2. Set environment variables (do not hardcode secrets):
   ```bash
   export DB_URL=jdbc:postgresql://localhost:5432/sentinel_aml
   export DB_USERNAME=your_db_user
   export DB_PASSWORD=your_db_password
   export JWT_SECRET=your_jwt_secret_min_32_chars
   ```

3. Build and run:
   ```bash
   cd app-server
   mvn clean package
   mvn spring-boot:run
   ```

4. Flyway applies migrations automatically on startup. Seed data is included in `V10__seed_detection_rules.sql` and `V11__seed_exchange_rates.sql`.

5. Swagger UI: `http://localhost:8080/swagger-ui.html`

### Database Schema

Flyway migration order:

```
V1__create_customers.sql
V2__create_accounts.sql
V3__create_transactions.sql
V4__create_detection_rules.sql
V5__create_alerts.sql
V6__create_cases.sql
V7__create_audit_log.sql
V8__create_exchange_rates.sql
V9__create_ingestion_jobs.sql
V10__seed_detection_rules.sql
V11__seed_exchange_rates.sql
```

Entity relationships:

```
Customer (1) ──── (N) Account (1) ──── (N) Transaction
                                              │
                                              ▼
                       Customer (1) ── (N) Alert (N) ──── (1) Case
                                              │
                                              ▼
                                          AuditLog
```

### API Reference

Base path: `/api/v1` — Bearer JWT required on all endpoints.

| Group | Endpoints |
|-------|-----------|
| Auth | `POST /auth/login` |
| Customers | `POST /customers/bulk`, `GET /customers`, `GET /customers/{id}`, `PUT /customers/{id}` |
| Accounts | `POST /accounts/bulk`, `GET /accounts/{id}`, `GET /accounts/{id}/transactions`, `PUT /accounts/{id}` |
| Transactions | `POST /transactions`, `POST /transactions/bulk`, `GET /transactions/{id}` |
| Alerts | `GET /alerts`, `GET /alerts/{id}`, `PUT /alerts/{id}/acknowledge`, `PUT /alerts/{id}/dismiss`, `PUT /alerts/{id}/escalate` |
| Cases | `GET /cases`, `GET /cases/{id}`, `PUT /cases/{id}`, `POST /cases/{id}/close` |
| Rules | `GET /rules`, `PUT /rules/{id}` |
| Jobs | `GET /jobs/{jobId}` |
| Exchange Rates | `GET /exchange-rates`, `PUT /exchange-rates/{currency}` |

Full OpenAPI spec available at `/v3/api-docs` once the server is running.

### Roles

| Role | Permissions |
|------|-------------|
| `ANALYST` | View alerts, cases, customers, accounts; acknowledge/dismiss/escalate alerts |
| `SUPERVISOR` | All ANALYST actions + assign/close cases, update customer/account records |
| `ADMIN` | All SUPERVISOR actions + bulk ingestion, rule configuration, exchange rate updates |

### Detection Rules

Rules are stored in the `DetectionRule` table and evaluated without redeployment. Toggle or update thresholds via `PUT /api/v1/rules/{id}`.

| Rule Code | Typology | Default Parameters |
|-----------|----------|--------------------|
| `CTR_THRESHOLD` | Single transaction ≥ ₹10,00,000 | `{ "threshold_inr": 1000000 }` |
| `STRUCTURING` | 3+ transactions ₹9L–₹9.99L within 24h | `{ "window_hours": 24, "min_count": 3, "lower_inr": 900000, "upper_inr": 999999 }` |
| `RAPID_MOVEMENT` | ≥80% of deposited funds transferred out within 48h | `{ "window_hours": 48, "outflow_pct": 80 }` |
| `HIGH_RISK_JURISDICTION` | Transactions involving sanctioned/high-risk countries | `{ "jurisdictions": ["IR","KP","SY"] }` |
| `BEHAVIORAL_DEVIATION` | Daily volume exceeds 3× 90-day rolling average | `{ "lookback_days": 90, "multiplier": 3.0 }` |
| `ROUND_NUMBER` | Repeated suspiciously round amounts | `{ "window_hours": 24, "min_count": 2 }` |

Each rule produces an `Alert` with a risk score (0–100), triggered rule code, supporting transaction IDs, and a human-readable explanation.

### Concurrency Design

| Thread Pool | Purpose | Core / Max / Queue |
|-------------|---------|-------------------|
| `ingestion-pool` | Bulk CSV ingestion | 4 / 16 / 10,000 |
| `detection-pool` | Rule evaluation per transaction | 8 / 32 / 50,000 |
| `audit-pool` | Async audit log writes | 2 / 4 / 20,000 |

Bulk ingest (`POST /transactions/bulk`) returns a `jobId` immediately; poll `GET /jobs/{jobId}` for progress. Single transaction ingest (`POST /transactions`) is synchronous with a target latency of < 200ms p95.

Alert deduplication is enforced by a DB-level unique partial index on `(account_id, rule_code) WHERE status = 'OPEN'` — no duplicate open alerts for the same rule and account.

### Business Rules

1. Single transaction ≥ ₹10,00,000 is automatically flagged (CTR threshold).
2. 3+ transactions between ₹9L–₹9.99L within 24h triggers a Structuring alert.
3. ≥80% outflow within 48h of a deposit triggers a Rapid Movement alert.
4. Any transaction involving a sanctioned or high-risk jurisdiction always generates an alert.
5. Daily volume exceeding 3× the 90-day rolling average triggers a Behavioral Deviation alert.
6. Alerts are never hard-deleted — dismissed alerts retain disposition reason and analyst identity.
7. Risk scores (0–100) are derived from a weighted combination of triggered rules; higher scores sort to the top of the analyst queue.
8. PII (name, DOB, contact details) is masked in list views; full detail requires the ANALYST role.
9. All monetary amounts are normalized to INR at ingestion time using a configurable `ExchangeRate` table.

### Running Tests

```bash
cd app-server
mvn test
```

Unit tests cover each detection rule using JUnit 5 + Mockito. One test class per rule.

---

## Frontend

### Technology Stack

| Component | Choice |
|-----------|--------|
| Framework | React 19 (JavaScript) |
| Styling | Tailwind CSS v3 |
| Routing | react-router-dom v6 |
| HTTP | axios |
| Auth | JWT in memory (React context — not localStorage) |

### Setup

**Prerequisites:** Node.js 18+

```bash
cd frontend
npm install
```

Create a `.env` file:
```
REACT_APP_API_URL=http://localhost:8080
```

Start the dev server:
```bash
npm start
```

### Pages

| Route | Purpose | Roles |
|-------|---------|-------|
| `/login` | Login form | Public |
| `/alerts` | Alert queue — default landing after login | All |
| `/alerts/:id` | Alert detail with evidence and audit trail | All |
| `/cases` | Investigation case list | All |
| `/cases/:id` | Case detail with linked alerts | All |
| `/customers/:id` | Full customer PII and linked accounts | All |
| `/accounts/:id` | Account detail with transaction history | All |
| `/rules` | Rule configuration — toggle, edit thresholds | ADMIN |
| `/jobs` | CSV bulk upload and job progress monitoring | ADMIN |

### Auth Flow

- JWT is stored in React context (memory only — not localStorage/sessionStorage).
- Every API request carries `Authorization: Bearer <token>` via an axios request interceptor.
- A 401 response clears the token and redirects to `/login`.
- Non-ADMIN users hitting `/rules` or `/jobs` are redirected to `/alerts`.

### Seed Users (prototype)

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin` | ADMIN |
| `supervisor` | `supervisor` | SUPERVISOR |
| `analyst` | `analyst` | ANALYST |

### Shared Components

| Component | Purpose |
|-----------|---------|
| `RiskScoreBadge` | Colored pill: green (0–39), amber (40–69), red (70–100) |
| `StatusChip` | Colored chip for alert/case status and priority |
| `PaginatedTable` | Sortable table with cursor-based pagination and 25/50/100 page-size selector |
| `ConfirmModal` | Confirmation dialog; dismiss variant includes a reason textarea |
| `AuditTimeline` | Collapsible timeline of status changes with actor and timestamp |
| `GlobalErrorBanner` | Fixed top banner fed by the axios response interceptor |
| `Navbar` | Top nav with role-filtered links and logout |
| `ProtectedRoute` | Redirects unauthenticated users to `/login` |
| `AdminRoute` | Redirects non-ADMIN users to `/alerts` |

---

## Demo Flow

1. **Login** as `admin` / `admin`.
2. Go to **Jobs** (`/jobs`) — upload the seed CSVs (customers, accounts, transactions).
3. Monitor job progress until all three jobs complete.
4. Switch to **Alerts** (`/alerts`) — alerts generated by the detection engine appear sorted by risk score.
5. Open an alert to see the full explanation and supporting transactions.
6. **Acknowledge** or **Escalate** an alert to create an investigation case.
7. Go to **Cases** (`/cases`) — open the case, add notes, assign to an analyst.
8. As `supervisor`, **close** the case with a disposition reason.
9. Go to **Rules** (`/rules`) — toggle a rule off or adjust a threshold; changes take effect immediately without redeployment.

---

## Security Notes

- No secrets are hardcoded; all credentials are read from environment variables.
- RBAC is enforced at the API layer (Spring Security), not just the UI.
- All alert and case state transitions are written to an append-only `AuditLog` table.
- PII masking is applied server-side in DTO projections — list views never expose full names or contact details.
- All API endpoints use proper HTTP status codes, `@Valid` input validation, and a centralized `GlobalExceptionHandler`.
