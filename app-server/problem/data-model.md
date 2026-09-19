# Sentinel AML — Core Data Model

## Entity Relationship Summary

```
Customer (1) ──── (N) Account (1) ──── (N) Transaction
                                              │
                                              ▼
                        Customer (1) ── (N) Alert (N) ──── (1) Case
                                              │
                                              ▼
                                          AuditLog

  ExchangeRate   (referenced at ingest for INR normalization)
  IngestionJob   (tracks bulk load progress)
  DetectionRule  (config for the detection engine)
```

Alert → Case is many-to-one (an alert belongs to at most one case; a case groups many alerts).

---

## 1. Customer

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| customer_ref | VARCHAR(20) UNIQUE | e.g. CUST_00001 |
| first_name | VARCHAR(100) | PII — masked in list views |
| last_name | VARCHAR(100) | PII — masked in list views |
| gender | CHAR(1) | |
| date_of_birth | DATE | PII |
| email | VARCHAR(255) | PII, unique |
| phone_number | VARCHAR(20) | PII |
| city | VARCHAR(100) | |
| state | VARCHAR(100) | |
| country | CHAR(2) | ISO 3166-1 alpha-2 |
| postal_code | VARCHAR(20) | |
| occupation | VARCHAR(100) | |
| annual_income | NUMERIC(18,2) | |
| marital_status | VARCHAR(20) | |
| education_level | VARCHAR(50) | |
| employment_status | VARCHAR(30) | |
| customer_since | DATE | |
| customer_segment | VARCHAR(20) | RETAIL / PREMIUM / BUSINESS |
| kyc_status | VARCHAR(20) | VERIFIED / PENDING / REJECTED |
| risk_rating | VARCHAR(10) | LOW / MEDIUM / HIGH |
| is_politically_exposed | BOOLEAN | |
| preferred_channel | VARCHAR(50) | |
| email_verified | BOOLEAN | |
| phone_verified | BOOLEAN | |
| num_complaints_last_year | INTEGER | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

---

## 2. Account

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| account_ref | VARCHAR(20) UNIQUE | e.g. ACC_000001 |
| customer_id | UUID FK → Customer | |
| account_type | VARCHAR(20) | SAVINGS / NRE / CURRENT / LOAN |
| account_status | VARCHAR(20) | ACTIVE / INACTIVE / FROZEN / CLOSED |
| currency | CHAR(3) | ISO 4217 |
| open_date | DATE | |
| close_date | DATE | nullable |
| branch_code | VARCHAR(20) | |
| branch_city | VARCHAR(100) | |
| current_balance | NUMERIC(18,2) | |
| avg_monthly_balance_6m | NUMERIC(18,2) | |
| credit_limit | NUMERIC(18,2) | |
| credit_utilization_pct | NUMERIC(5,2) | |
| overdraft_enabled | BOOLEAN | |
| card_type | VARCHAR(20) | CLASSIC / GOLD / PLATINUM |
| is_joint_account | BOOLEAN | |
| num_linked_devices | INTEGER | |
| mobile_banking_enrolled | BOOLEAN | |
| last_login_date | DATE | |
| avg_monthly_txn_count | INTEGER | |
| account_tier | VARCHAR(20) | SILVER / GOLD / PLATINUM |
| risk_rating | VARCHAR(10) | LOW / MEDIUM / HIGH — overrides customer rating |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

---

## 3. Transaction

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| transaction_ref | VARCHAR(30) UNIQUE | |
| account_id | UUID FK → Account | source account |
| amount | NUMERIC(18,2) | original amount |
| currency | CHAR(3) | original currency |
| amount_inr | NUMERIC(18,2) | normalized to INR at ingestion time |
| exchange_rate_used | NUMERIC(18,6) | rate applied at ingestion |
| transaction_type | VARCHAR(20) | DEPOSIT / WITHDRAWAL / TRANSFER / PAYMENT |
| channel | VARCHAR(30) | MOBILE / BRANCH / ONLINE / ATM / SWIFT |
| counterparty_account | VARCHAR(50) | nullable |
| counterparty_bank | VARCHAR(100) | nullable |
| counterparty_jurisdiction | CHAR(2) | ISO 3166-1, nullable |
| description | VARCHAR(500) | |
| transaction_timestamp | TIMESTAMPTZ | when the txn occurred |
| ingested_at | TIMESTAMPTZ | when we received it |
| status | VARCHAR(20) | PENDING / PROCESSED / FAILED |
| job_id | UUID | bulk job reference, nullable |

Index: `(account_id, transaction_timestamp)` — supports time-window queries for detection rules.

---

## 4. DetectionRule

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| rule_code | VARCHAR(50) UNIQUE | e.g. STRUCTURING, RAPID_MOVEMENT |
| name | VARCHAR(100) | |
| description | TEXT | |
| enabled | BOOLEAN | toggle without redeployment |
| risk_weight | INTEGER | 1–100, used in score calculation |
| parameters | JSONB | threshold values, time windows, lists |
| version | INTEGER | incremented on each update |
| updated_by | VARCHAR(100) | |
| updated_at | TIMESTAMPTZ | |

**Rule codes and their parameters (JSONB shape):**

| rule_code | parameters example |
|-----------|-------------------|
| CTR_THRESHOLD | `{ "threshold_inr": 1000000 }` |
| STRUCTURING | `{ "window_hours": 24, "min_count": 3, "lower_inr": 900000, "upper_inr": 999999 }` |
| RAPID_MOVEMENT | `{ "window_hours": 48, "outflow_pct": 80 }` |
| HIGH_RISK_JURISDICTION | `{ "jurisdictions": ["IR","KP","SY"] }` |
| BEHAVIORAL_DEVIATION | `{ "lookback_days": 90, "multiplier": 3.0 }` |
| ROUND_NUMBER | `{ "window_hours": 24, "min_count": 2 }` |

---

## 5. Alert

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| alert_ref | VARCHAR(30) UNIQUE | |
| account_id | UUID FK → Account | |
| customer_id | UUID FK → Customer | denormalized for fast querying |
| rule_code | VARCHAR(50) | FK → DetectionRule.rule_code |
| status | VARCHAR(20) | OPEN / ACKNOWLEDGED / DISMISSED / ESCALATED |
| risk_score | INTEGER | 0–100 |
| explanation | TEXT | human-readable reason |
| evidence_txn_ids | UUID[] | supporting transaction IDs |
| disposition_reason | TEXT | populated on dismiss/close |
| assigned_to | VARCHAR(100) | analyst username |
| case_id | UUID FK → Case | nullable, set on escalation |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

Rule: alerts are NEVER hard-deleted (business rule #6).

Deduplication: unique partial index on `(account_id, rule_code)` WHERE `status = 'OPEN'` — prevents duplicate open alerts for the same rule + account.

---

## 6. Case

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| case_ref | VARCHAR(30) UNIQUE | |
| title | VARCHAR(255) | |
| status | VARCHAR(20) | OPEN / IN_REVIEW / CLOSED / REPORTED |
| priority | VARCHAR(10) | LOW / MEDIUM / HIGH / CRITICAL |
| assigned_to | VARCHAR(100) | analyst username |
| notes | TEXT | |
| disposition | VARCHAR(50) | SAR_FILED / FALSE_POSITIVE / CLOSED_NO_ACTION |
| disposition_reason | TEXT | |
| created_by | VARCHAR(100) | |
| closed_by | VARCHAR(100) | nullable |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |
| closed_at | TIMESTAMPTZ | nullable |

---

## 7. AuditLog

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| entity_type | VARCHAR(30) | ALERT / CASE / RULE / CUSTOMER |
| entity_id | UUID | |
| action | VARCHAR(50) | CREATED / STATUS_CHANGED / ASSIGNED / DISMISSED etc. |
| old_value | JSONB | previous state snapshot |
| new_value | JSONB | new state snapshot |
| actor | VARCHAR(100) | username or SYSTEM |
| actor_ip | VARCHAR(45) | |
| timestamp | TIMESTAMPTZ | |

Append-only table — no UPDATE or DELETE permitted.

---

## 8. ExchangeRate

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| currency | CHAR(3) UNIQUE | ISO 4217 |
| rate_to_inr | NUMERIC(18,6) | multiply original amount by this |
| effective_from | TIMESTAMPTZ | |
| updated_by | VARCHAR(100) | |

---

## 9. IngestionJob

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| job_type | VARCHAR(20) | CUSTOMERS / ACCOUNTS / TRANSACTIONS |
| status | VARCHAR(20) | QUEUED / RUNNING / COMPLETED / FAILED |
| total_records | INTEGER | |
| processed | INTEGER | |
| failed | INTEGER | |
| error_summary | TEXT | |
| submitted_by | VARCHAR(100) | |
| started_at | TIMESTAMPTZ | |
| completed_at | TIMESTAMPTZ | |

---

## Flyway Migration Order

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
