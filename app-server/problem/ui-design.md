# Sentinel AML — UI Design

Tech: React + TypeScript. Auth-gated. Roles: `ANALYST`, `SUPERVISOR`, `ADMIN`.

> Backend enforces JWT + RBAC (required by problem statement — RBAC at API layer). UI has a login screen. Seed users for the prototype (username = role, password = role name): `admin`/`admin` (ADMIN), `supervisor`/`supervisor` (SUPERVISOR), `analyst`/`analyst` (ANALYST).

---

## Pages & Routes

| Route | Page | Roles |
|-------|------|-------|
| `/login` | Login | Public |
| `/alerts` | Alert Queue (default landing after login) | All |
| `/alerts/:id` | Alert Detail | All |
| `/cases` | Case List | All |
| `/cases/:id` | Case Detail | All |
| `/customers/:id` | Customer Detail | All |
| `/accounts/:id` | Account Detail | All |
| `/rules` | Rule Configuration | ADMIN |
| `/jobs` | Ingestion Jobs | ADMIN |

Unauthenticated users are redirected to `/login`. After login, land on `/alerts`.

---

## 0. Login (`/login`)

- Username + password form (defaults pre-filled for prototype: `admin` / `admin`)
- On submit: `POST /api/v1/auth/login` → returns JWT
- On success: store JWT in memory (React context), redirect to `/alerts`
- On failure: inline error from API `message` field
- Already-authenticated users hitting `/login` are redirected to `/alerts`

---

## 1. Alert Queue (`/alerts`)

**Purpose:** Primary analyst workspace. Show all open alerts sorted by risk score descending.

### Filters (top bar)
- Status: `OPEN` | `ACKNOWLEDGED` | `DISMISSED` | `ESCALATED` (multi-select)
- Rule type: dropdown from rule codes
- Date range: from / to
- Min risk score: slider 0–100

### Table columns
| Column | Notes |
|--------|-------|
| Risk Score | Colored badge: 0–39 green, 40–69 amber, 70–100 red |
| Alert Ref | Link to `/alerts/:id` |
| Rule | Human-readable rule name |
| Customer | Masked — show `****name` |
| Account Ref | |
| Amount (INR) | Formatted |
| Created At | Relative time |
| Status | Chip |
| Actions | Acknowledge / Dismiss / Escalate buttons (inline) |

- Pagination: cursor-based (infinite scroll / "Load more"); page size 25 / 50 / 100 via `size` param, next page via `cursor` param
- Sort: fixed server-side newest-first (`id DESC`); no client-driven column sort on the server query
- Acknowledge / Dismiss open a small modal for confirmation (dismiss requires reason text)
- Escalate opens modal: create new case or link to existing case (search by case ref)

---

## 2. Alert Detail (`/alerts/:id`)

Layout: two-column.

**Left — Alert Summary**
- Risk score (large colored number)
- Status chip + action buttons (Acknowledge / Dismiss / Escalate)
- Rule triggered
- Explanation text (full human-readable)
- Created at / updated at

**Right — Evidence**
- Table of supporting transactions: ref, amount, currency, timestamp, counterparty jurisdiction
- Each transaction ref links to nothing (no transaction detail page — not required)

**Below — Customer & Account strip**
- Customer: masked name, segment, risk rating, KYC status → link to `/customers/:id`
- Account: ref, type, balance, tier → link to `/accounts/:id`

**Audit trail** (collapsible at bottom)
- Timeline of status changes: actor, timestamp, old → new status

---

## 3. Case List (`/cases`)

### Filters
- Status: `OPEN` | `IN_REVIEW` | `CLOSED` | `REPORTED`
- Assigned to: text search on analyst name
- Priority: LOW / MEDIUM / HIGH / CRITICAL

### Table columns
| Column | Notes |
|--------|-------|
| Case Ref | Link to `/cases/:id` |
| Title | |
| Priority | Colored chip |
| Status | Chip |
| Linked Alerts | Count badge |
| Assigned To | |
| Created At | |

---

## 4. Case Detail (`/cases/:id`)

Layout: two-column.

**Left — Case Info**
- Title, priority, status
- Assigned to (editable dropdown for SUPERVISOR+)
- Notes textarea (editable)
- Close button (SUPERVISOR+) → modal requires disposition + reason

**Right — Linked Alerts**
- List of alert cards: risk score badge, rule name, amount, status
- Each links to `/alerts/:id`

**Audit trail** (collapsible)
- Same timeline component as Alert Detail

---

## 5. Customer Detail (`/customers/:id`)

- Full PII visible (requires ANALYST role — enforced by API, not just UI)
- Fields: name, DOB, email, phone, address, KYC status, risk rating, PEP flag, segment
- Accounts table: ref, type, status, balance, tier → each links to `/accounts/:id`
- Recent alerts: last 10, link to `/alerts/:id`

---

## 6. Account Detail (`/accounts/:id`)

- Account fields: ref, type, status, balance, avg balance, branch, tier, risk rating
- Transaction table: paginated, last 30 days default
  - Columns: ref, amount (original + INR), type, channel, counterparty jurisdiction, timestamp
- Active alerts: badge count at top; list below

---

## 7. Rule Configuration (`/rules`) — ADMIN only

Table of all rules:

| Column | Notes |
|--------|-------|
| Rule Code | |
| Name | |
| Enabled | Toggle switch |
| Risk Weight | Editable number |
| Parameters | "Edit" button → inline JSON editor |
| Last Updated By / At | |

- Toggling enabled or saving parameters calls `PUT /api/v1/rules/:id`
- No delete — rules are always present, just enabled/disabled

---

## 8. Ingestion Jobs (`/jobs`) — ADMIN only

- Upload section: three file inputs (customers CSV, accounts CSV, transactions CSV) + Submit
  - Calls `POST /api/v1/customers/bulk`, `/accounts/bulk`, `/transactions/bulk`
- Jobs table: jobId, type, status, total / processed / failed counts, submitted by, started / completed at
- Auto-refresh every 5s while any job is `RUNNING` (stop polling when all terminal)

---

## Shared Components

| Component | Used by |
|-----------|---------|
| `RiskScoreBadge` | Alert Queue, Alert Detail, Case Detail |
| `StatusChip` | Alert Queue, Case List |
| `AuditTimeline` | Alert Detail, Case Detail |
| `ConfirmModal` | Dismiss, Escalate, Close Case |
| `PaginatedTable` | All list pages — cursor-based (`cursor` + `size`), renders "Load more" from `nextCursor`/`hasMore` |
| `GlobalErrorBanner` | Catches unhandled API errors via Axios interceptor |

---

## API Calls per Page

| Page | API calls |
|------|-----------|
| Login | `POST /auth/login` |
| Alert Queue | `GET /alerts` |
| Alert Detail | `GET /alerts/:id` |
| Case List | `GET /cases` |
| Case Detail | `GET /cases/:id` |
| Customer Detail | `GET /customers/:id` |
| Account Detail | `GET /accounts/:id` → `GET /accounts/:id/transactions` |
| Rules | `GET /rules`, `PUT /rules/:id` |
| Jobs | `GET /jobs/:jobId` (poll), bulk POSTs |

---

## Auth & Security (UI side)

- JWT stored in memory (React context) — not localStorage/sessionStorage
- Axios request interceptor attaches `Authorization: Bearer <token>` to every request
- Axios response interceptor: on 401 → clear token, redirect to `/login`
- Protected routes: unauthenticated access redirects to `/login`
- Role-based rendering: ADMIN-only pages redirect non-admins to `/alerts`
- PII fields rendered as-is from API — masking enforced server-side
- Note: `GET /customers` (list) fully redacts PII fields (`"***"`); `GET /customers/:id` (detail) returns full PII for ANALYST+
- Default seed users for prototype: `admin`/`admin` (ADMIN), `supervisor`/`supervisor` (SUPERVISOR), `analyst`/`analyst` (ANALYST)

