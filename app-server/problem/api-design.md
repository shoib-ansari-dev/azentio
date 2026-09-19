# Sentinel AML — API Design

Base path: `/api/v1`

Auth: Bearer JWT on all endpoints. Roles: `ANALYST`, `SUPERVISOR`, `ADMIN`.

---

## Centralized Exception Handling

A single `@RestControllerAdvice` (`GlobalExceptionHandler`) handles all errors.
No try/catch in controllers — they throw, the handler maps to response.

| Exception | HTTP Status | Error Code |
|-----------|-------------|------------|
| `EntityNotFoundException` | 404 | `NOT_FOUND` |
| `DuplicateEntityException` | 409 | `CONFLICT` |
| `ValidationException` / `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `AccessDeniedException` | 403 | `FORBIDDEN` |
| `AuthenticationException` | 401 | `UNAUTHORIZED` |
| `IngestionException` | 422 | `INGESTION_FAILED` |
| `Exception` (catch-all) | 500 | `INTERNAL_ERROR` |

Standard error response shape:
```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "amount must be positive",
  "timestamp": "2026-09-19T10:00:00Z"
}
```

---

## 1. Auth

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/auth/login` | Public | Returns JWT |

---

## 2. Customers

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/customers/bulk` | ADMIN | Bulk ingest from CSV |
| GET | `/customers/{id}` | ANALYST | Full detail (PII visible) |
| GET | `/customers` | ANALYST | List — PII masked, paginated |
| PUT | `/customers/{id}` | SUPERVISOR | Update KYC / risk rating |

---

## 3. Accounts

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/accounts/bulk` | ADMIN | Bulk ingest from CSV |
| GET | `/accounts/{id}` | ANALYST | Account detail |
| GET | `/accounts/{id}/transactions` | ANALYST | Transactions for account — paginated |
| PUT | `/accounts/{id}` | SUPERVISOR | Update status / risk rating |

---

## 4. Transactions

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/transactions` | ADMIN | Single transaction — synchronous detection |
| POST | `/transactions/bulk` | ADMIN | Bulk ingest — async, returns `jobId` |
| GET | `/transactions/{id}` | ANALYST | Transaction detail |

---

## 5. Detection Rules

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/rules` | ANALYST | List all rules with current parameters |
| PUT | `/rules/{id}` | ADMIN | Update thresholds / toggle enabled |

---

## 6. Alerts

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/alerts` | ANALYST | List — sortable by risk score, filterable by status |
| GET | `/alerts/{id}` | ANALYST | Detail with evidence + explanation |
| PUT | `/alerts/{id}/acknowledge` | ANALYST | Mark acknowledged |
| PUT | `/alerts/{id}/dismiss` | ANALYST | Dismiss with reason (soft, never deleted) |
| PUT | `/alerts/{id}/escalate` | ANALYST | Escalate → creates or links a Case |

---

## 7. Cases

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/cases` | ANALYST | List cases — paginated |
| GET | `/cases/{id}` | ANALYST | Case detail with linked alerts |
| PUT | `/cases/{id}` | ANALYST | Assign / add notes |
| POST | `/cases/{id}/close` | SUPERVISOR | Close with disposition reason |

---

## 8. Ingestion Jobs

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/jobs/{jobId}` | ADMIN | Poll bulk job status |

---

## 9. Exchange Rates

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/exchange-rates` | ANALYST | List current rates |
| PUT | `/exchange-rates/{currency}` | ADMIN | Update a rate |

---

## Common Conventions

- List endpoints support: `page`, `size`, `sort` query params.
- All amounts returned in INR; original currency + amount also present on transaction.
- Bulk endpoints accept `multipart/form-data` with a CSV file.
