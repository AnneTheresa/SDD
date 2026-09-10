# Digital Banking Platform — Feature Specification

**Project:** Digital Banking Platform  
**Team:** Lalitha · Sheheyar · Tarunjeet · Suraj · Srikrishna · Emad  
**Stack:** Spring Boot 3.x (Java 21) · React 18 / Next.js · H2 (dev, file-based) · JPA · JWT (HS256)  
**Last Updated:** 2026-04-20  
**Status:** Final — single unified specification

---

## Table of Contents

1. [Business Context](#1-business-context)
2. [Purpose and Scope](#2-purpose-and-scope)
3. [Clarifications and Decisions](#3-clarifications-and-decisions)
4. [Architecture Overview](#4-architecture-overview)
5. [Shared Definitions](#5-shared-definitions)
6. [Role-Based Access Control](#6-role-based-access-control)
7. [Non-Functional Requirements](#7-non-functional-requirements)
8. [Data Storage Principles](#8-data-storage-principles)
9. [Data Retention and Audit Policy](#9-data-retention-and-audit-policy)
10. [Cross-Cutting Validation and Error Semantics](#10-cross-cutting-validation-and-error-semantics)
11. [Implementation Infrastructure](#11-implementation-infrastructure)
12. [US-101 — Register](#12-us-101--register)
13. [US-102 — Login](#13-us-102--login)
14. [US-103 — Create Customer](#14-us-103--create-customer)
15. [US-104 — Update Customer](#15-us-104--update-customer)
16. [US-105 — Get Customer](#16-us-105--get-customer)
17. [AD-001 — List All Customers (Admin)](#17-ad-001--list-all-customers-admin)
18. [OP-01 — Delete Customer](#18-op-01--delete-customer)
19. [OP-02 — Create Account](#19-op-02--create-account)
20. [OP-03 — Retrieve Account Details](#20-op-03--retrieve-account-details)
21. [OP-04 — List Customer Accounts](#21-op-04--list-customer-accounts)
22. [OP-05 — Update Account](#22-op-05--update-account)
23. [OP-06 — Delete Account](#23-op-06--delete-account)
24. [OP-07 — Deposit](#24-op-07--deposit)
25. [OP-08 — Withdraw](#25-op-08--withdraw)
26. [OP-09 — Transfer Funds](#26-op-09--transfer-funds)
27. [US-08 — Get Transaction History](#27-us-08--get-transaction-history)
28. [US-09 — Standing Order Management](#28-us-09--standing-order-management)
29. [US-10 — Evaluate Notification Event](#29-us-10--evaluate-notification-event)
30. [US-11 — Get Monthly Statement](#30-us-11--get-monthly-statement)
31. [US-12 — Spending Insights](#31-us-12--spending-insights)
32. [Security and Guardrails](#32-security-and-guardrails)
33. [Definition of Done](#33-definition-of-done)

---

## 1. Business Context

The Banking API is the core service for managing customer accounts and monetary operations within a retail banking platform. It exposes a REST interface consumed by a web frontend and, where authorised, by internal back-office tooling. Two categories of caller interact with the API: customers who manage their own data, and administrators who have elevated, unrestricted access across all resources.

The platform covers the full customer lifecycle:

- **Authentication and identity:** Registration, login, JWT issuance, customer profile management.
- **Accounts and transactions:** Account creation, balance operations (deposit, withdraw, transfer), account lifecycle management.
- **Extended banking features:** Transaction history, standing orders, notification evaluation, monthly statements, and spending insights.

---

## 2. Purpose and Scope

This specification defines the Banking API as the single source of truth for contract generation and implementation alignment. It covers all operations:

**Auth & Customer**
1. Register (US-101)
2. Login (US-102)
3. Create Customer (US-103)
4. Update Customer (US-104)
5. Get Customer (US-105)
6. List All Customers — Admin only (AD-001, implementation-derived)

**Accounts & Monetary Operations**
7. Delete Customer
8. Create Account
9. Retrieve Account Details
10. List Customer Accounts
11. Update Account
12. Delete Account
13. Deposit
14. Withdraw
15. Transfer Funds

**Extended Banking Features**
16. Get Transaction History (US-08)
17. Standing Order Management (US-09)
18. Evaluate Notification Event (US-10)
19. Get Monthly Statement (US-11)
20. Spending Insights (US-12)

### Development Perspective

- **Backend:** Java 21 with Spring Boot 3.x (`Web`, `Data JPA`, `Validation`, `Security`, `Actuator`)
- **Frontend:** React 18 with JavaScript, React Query v5, Axios, and Vite
- **Runtime persistence:** H2 file-based (`./data/digitalbankdb`) for current environment
- **Authentication:** Stateless JWT (HS256), validated by Spring Security filter on every protected request
- **API documentation:** SpringDoc / Swagger UI at `/swagger-ui.html`; OpenAPI JSON at `/v3/api-docs/**`
- Treat all statements using MUST, MUST NOT, REQUIRED, and NOT ALLOWED as normative.
- Operation-specific statements take precedence over general cross-cutting statements when they are more specific.

### QA Perspective

- Use this document as the basis for traceable test coverage.
- For every operation verify: request/response contract, security behaviour, state change, and required persisted side effects.
- Acceptance criteria are the minimum regression set and should be extended with boundary, negative, and authorization scenarios.

---

## 3. Clarifications and Decisions

### Session 2026-04-02

- Q: Should failed monetary operations be persisted as transactions? → **A:** Yes, persist transactions for both SUCCESS and FAILED for Deposit, Withdraw, and Transfer.
- Q: How should concurrent money-movement conflicts be handled? → **A:** Allow concurrent operations; whichever commits first wins silently.
- Q: How should duplicate retries be handled for monetary operations? → **A:** Require idempotency for Deposit, Withdraw, and Transfer so retried identical requests return the original outcome.
- Q: What is the API security baseline? → **A:** All endpoints require authenticated caller; account/customer operations allowed only on resources the caller is authorized to access.
- Q: What are the monetary currency and precision rules? → **A:** All monetary amounts are in one system currency with scale=2.

### Session 2026-04-06

- Q: Should this API persist internal user data in addition to external identity? → **A:** Yes. The API persists a local user record linked to the external identity subject.
- Q: Are admin role grant/revoke actions in scope? → **A:** Yes, admin role-management capability is in scope.
- Q: What validation applies to `interestRate` on savings accounts? → **A:** `interestRate` must be a non-negative decimal with at most 4 decimal places.

---

## 4. Architecture Overview

The platform is built as a single unified backend. The JWT issued at login is validated on every protected request.

```
Frontend (React/Next.js)
        │
        │  POST /api/auth/register
        │  POST /api/auth/login  ──────► JWT issued
        │
        │  Authorization: Bearer <token>
        ▼
  Auth & Customer (/api/*)        Accounts & Extended Features (no /api prefix)
  ──────────────────────          ──────────────────────────────────────
  /api/auth/register              /customers/{id}
  /api/auth/login                 /customers/{id}/accounts
  /api/customers                  /accounts/{id}
  /api/customers/{id}             /accounts/{id}/deposit
                                  /accounts/{id}/withdraw
                                  /accounts/transfer
                                  /accounts/{id}/transactions
                                  /accounts/{id}/transactions/export
                                  /accounts/{id}/standing-orders
                                  /standing-orders/{standingOrderId}
                                  /notifications/evaluate
                                  /accounts/{id}/statements/{period}
                                  /accounts/{id}/insights
                                  /accounts/{id}/transactions/{txId}/category
```

> **Key dependency:** `customerId` generated by `POST /api/customers` is the foreign key for every account operation. All components use the same `Long` type and auto-increment strategy.

---

## 5. Shared Definitions

### 5.1 Domain Objects

#### User

Represents an authenticated actor with a local application record linked to an external identity subject.

- `userId` — UUID, system-generated unique identifier. Immutable. Auto-generated via `@PrePersist`
- `username` — string, lowercase email format, unique, stored as-is. Valid email format enforced at registration
- `passwordHash` — BCrypt hash. Never stored plain. **Never returned in any response, log, or error**
- `externalSubjectId` — string. Links this record to the external Identity Provider JWT subject claim
- `customerId` — long, optional. Foreign key linking this user to their Customer record. Null for admin-only users
- `roles` — list of `RoleName` enum values (`CUSTOMER`, `ADMIN`). Defaults to `[CUSTOMER]` on registration. Stored in `user_roles` join table
- `isActive` — boolean. `true` on registration. `false` = login rejected with `403 ACCOUNT_INACTIVE`
- `createdAt` — `Instant`, system-managed, set by `@PrePersist`, immutable
- `updatedAt` — `Instant`, system-managed, set by `@PreUpdate`

> **Implementation note:** The `User` entity uses UUID primary key (`user_id`) stored in a `users` table. Roles are stored as `RoleName` enum strings in a `user_roles` collection table.

#### Customer

Represents a person or company that holds banking accounts.

- `customerId` — long, auto-incremented primary key. Cannot be supplied by caller
- `name` — string, minimum 2 characters, not blank
- `address` — string, not blank
- `type` — enum, `PERSON` or `COMPANY`
- `accounts` — list of `Account` objects. Starts empty on customer creation
- `createdAt` — `Instant`, system-managed, immutable
- `updatedAt` — `Instant`, system-managed, refreshed on every update

**Immutability rule:** `email` and `accountNumber` on a customer are permanently immutable after creation. Any `PATCH /api/customers/{customerId}` request that includes either field must be rejected with `400 FIELD_NOT_UPDATABLE` before any DB call, even if other fields are valid.

#### Account

A customer-held banking product. Account type is immutable after creation.

- `accountId` — long, system-generated unique identifier. Used as the path parameter in all account-level endpoints
- `customerId` (via `customer` relationship) — long, foreign key linking this account to its owning Customer
- `accountType` — enum, `CHECKING` or `SAVINGS`. **Immutable after creation**
- `status` — enum, `ACTIVE` or `CLOSED`. Newly created accounts MUST be `ACTIVE`
- `balance` — `BigDecimal` at exactly two decimal places
- `interestRate` — `BigDecimal`, Savings accounts only. Non-negative, at most 4 decimal places. Not present on CHECKING accounts
- `accountNumber` — string, system-generated account number string *(implementation-derived; not in original specs)*
- `dailyTransferLimit` — `BigDecimal`, default `3000.00`. Applied to standing orders and transfer operations *(implementation-derived)*
- `version` — long, optimistic locking version field managed by JPA `@Version` *(implementation-derived)*
- `deletedAt` — `Instant`, set when account is soft-deleted *(implementation-derived)*
- `closedAt` — `Instant`, set when account status transitions to `CLOSED` *(implementation-derived)*
- `createdAt` — `Instant`, system-managed, set at creation
- `updatedAt` — `Instant`, system-managed, updated on each change

> **Note:** The `nextCheckNumber` field is **not implemented** in the current Account entity. It is out of scope for this release.

#### Transaction

A record of a monetary operation on an account. Both SUCCESS and FAILED outcomes are persisted and are immutable once written.

- `transactionId` — string (UUID, 36 chars), system-generated unique identifier. Stored in `bank_transaction` table
- `accountId` (via `account` relationship) — long, foreign key linking this transaction to its account
- `amount` — `BigDecimal` at exactly two decimal places. Must be greater than zero
- `direction` — enum, `CREDIT`, `DEBIT`, or `TRANSFER`. Determines the nature of the monetary operation. Note: the implementation uses `TransactionDirection` (`CREDIT`/`DEBIT`/`TRANSFER`) whereas some spec sections describe a `type` field (`DEPOSIT`/`WITHDRAW`/`TRANSFER`). The normative model is `direction` per the implementation
- `status` — enum, `PENDING`, `SUCCESS`, or `FAILED`
- `timestamp` — `Instant`, ISO-8601 UTC, the date and time the transaction occurred
- `description` — string, maximum 255 characters
- `senderInfo` — string, maximum 100 characters, present on `CREDIT` transactions *(implementation field)*
- `receiverInfo` — string, maximum 100 characters, present on `DEBIT` transactions *(implementation field)*
- `idempotencyKey` — string, optional. Present only when the originating request supplied an `Idempotency-Key` header
- `category` — string, optional. Spending category assigned by the system or manually by the customer via the recategorise endpoint *(implementation-derived)*
- `externalTransactionId` — string (UUID), auto-generated. Secondary unique identifier for cross-system reference *(implementation-derived)*

#### StandingOrder

A recurring payment instruction that transfers funds from a source account to a named payee on a defined schedule.

- `standingOrderId` — UUID string of exactly 36 characters, system-generated
- `sourceAccountId` — long, foreign key to the source account
- `payeeAccount` — string, maximum 34 characters. Must pass Modulo 97 (ISO 7064 MOD-97-10) checksum
- `payeeName` — string, 1 to 70 characters
- `amount` — `BigDecimal` at exactly two decimal places. Greater than zero, must not exceed `dailyTransferLimit`
- `frequency` — enum, `DAILY`, `WEEKLY`, `MONTHLY`, or `QUARTERLY`
- `startDate` — UTC datetime. Must be at least 24 hours from creation time
- `endDate` — UTC datetime, optional. Must be after `startDate` if provided
- `reference` — string, 1 to 18 alphanumeric characters
- `status` — enum: `ACTIVE`, `CANCELLED`, `LOCKED`, `TERMINATED`, `RETRY_PENDING`, `FAILED_INSUFFICIENT_FUNDS` *(the last two are implementation-derived lifecycle states)*
- `nextRunDate` — UTC datetime. Next scheduled execution date, calculated with Canadian Bank Holiday shifting
- `createdAt` — system-managed UTC datetime
- `updatedAt` — system-managed UTC datetime

#### NotificationDecision

The persisted outcome of evaluating a business event.

- `eventId` — UUID string (36 chars), primary key. Used for deduplication
- `eventType` — string, must match a defined type in the Event Classification Matrix
- `accountId` — long, identifies the account the event relates to
- `customerId` — long, identifies the customer
- `businessTimestamp` — UTC datetime of when the business event occurred
- `payload` — TEXT, optional. Event-specific contextual data (stored as JSON string)
- `decision` — enum, `RAISED`, `GROUPED`, or `SUPPRESSED`
- `decisionReason` — string, maximum 500 characters
- `mandatoryOverride` — boolean. True when a mandatory event overrode a customer opt-out
- `evaluatedAt` — UTC datetime, set by `@PrePersist`

#### NotificationPreference *(implementation-derived)*

Stores per-customer, per-event-type notification opt-in preferences.

- `preferenceId` — long, auto-generated
- `customerId` — long, foreign key
- `eventType` — string. The event type this preference applies to. Unique constraint on (`customerId`, `eventType`)
- `optedIn` — boolean, default `true`
- `updatedAt` — UTC datetime, set by `@PrePersist` and `@PreUpdate`

#### MonthlyStatement

The formal monthly account record. Generated as a PDF by the system.

- `accountId` — long, identifies the account. Immutable
- `period` — string in `YYYY-MM` format
- `openingBalance` — `BigDecimal` at exactly two decimal places. Must match the closing balance of the prior period's issued statement
- `closingBalance` — `BigDecimal` at exactly two decimal places
- `totalMoneyIn` — sum of all DEPOSIT and TRANSFER-in transactions with status SUCCESS for the period
- `totalMoneyOut` — sum of all WITHDRAW and TRANSFER-out transactions with status SUCCESS for the period
- `transactions` — all transactions for the period including SUCCESS and FAILED. Never truncated
- `versionNumber` — integer, starts at 1, incremented on each correction
- `correctionSummary` — string, present on corrected versions only
- `generatedAt` — ISO-8601 UTC timestamp

> **Implementation note:** The `GET /accounts/{accountId}/statements/{period}` endpoint returns the statement directly as `application/pdf` bytes. JSON representation is not currently returned by this endpoint.

#### SpendingInsight

The insight record for a selected calendar month.

- `accountId` — long, the account the insight was generated for
- `period.year` / `period.month` / `period.isComplete` — selected period details
- `totalDebitSpend` — sum of all eligible SUCCESS WITHDRAW and TRANSFER-out amounts
- `transactionCount` — count of eligible SUCCESS transactions
- `hasUncategorised` — boolean
- `hasExcludedDisputes` — boolean
- `dataFresh` — boolean. `false` when upstream data is delayed
- `categoryBreakdown` — all eight categories with total amount and percentage. Always eight entries. Percentages sum to exactly 100
- `topTransactions` — five largest individual eligible transactions
- `sixMonthTrend` — exactly six entries covering the last six calendar months

#### AuditLog *(implementation-derived)*

An append-only log of all significant operations. Stored in `audit_log` table.

- `logId` — long, auto-generated
- `actorId` — string, maximum 50 characters. The user or system ID performing the action
- `actorRole` — string, maximum 50 characters
- `action` — string, maximum 100 characters
- `resourceType` — string, maximum 60 characters
- `resourceId` — string, maximum 100 characters
- `outcome` — string, maximum 20 characters (e.g., `SUCCESS`, `ERROR`)
- `timestamp` — set by `@PrePersist`

#### ErrorResponse

Returned for all error responses.

```json
{
  "code": "string",
  "message": "string",
  "field": "string"
}
```

- `code` — machine-readable error code (e.g., `ACCOUNT_NOT_FOUND`)
- `message` — human-readable explanation
- `field` — optional, present when the error relates to a specific request field

---

### 5.2 Enums

| Enum | Values |
|---|---|
| `AccountType` | `CHECKING`, `SAVINGS` |
| `AccountStatus` | `ACTIVE`, `CLOSED` |
| `TransactionDirection` | `CREDIT`, `DEBIT`, `TRANSFER` |
| `TransactionStatus` | `PENDING`, `SUCCESS`, `FAILED` |
| `StandingOrderStatus` | `ACTIVE`, `CANCELLED`, `LOCKED`, `TERMINATED`, `RETRY_PENDING`, `FAILED_INSUFFICIENT_FUNDS` |
| `Frequency` | `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY` |
| `SpendingCategory` | `Housing`, `Transport`, `Food & Drink`, `Entertainment`, `Shopping`, `Utilities`, `Health`, `Income` |
| `CustomerType` | `PERSON`, `COMPANY` |
| `RoleName` | `CUSTOMER`, `ADMIN` |
| `NotificationDecision` | `RAISED`, `GROUPED`, `SUPPRESSED` |

---

### 5.3 Actors and Roles

**CUSTOMER** — an authenticated end-user. Access restricted to their own resources only.

Allowed:
- Register, login (public)
- Create, read, and update their own Customer profile
- Create, read, and update accounts for their own Customer
- Deposit, withdraw, and transfer on their own accounts
- View transaction history for their own accounts
- Create, list, and cancel standing orders on their own accounts
- View monthly statements for their own accounts
- View spending insights for their own accounts

Not allowed:
- Delete customers or accounts
- Access another customer's resources
- Grant or revoke roles

**ADMIN** — a privileged operator with unrestricted access. Bypasses ownership checks. All permissions apply.

Allowed:
- All CUSTOMER operations on any resource
- Delete Customer (no active accounts constraint)
- Delete Account (zero balance constraint applied)
- List all customers
- Grant and revoke roles
- Create Customers

---

### 5.4 Credential Policy

**Username**
- Valid email format (`@Email`). Unique. Stored as-is (used as login identifier).
- May also follow the pattern `^[a-zA-Z][a-zA-Z0-9._-]{3,29}$` (4–30 chars, begins with a letter).
- Length 4–30, letters/digits/underscores/hyphens/periods, must begin with a letter, case-insensitive stored in lower case.

**Password**
- Length: 8–128 characters
- Must contain at least one uppercase letter, one lowercase letter, one digit, and one special character from: `! @ # $ % ^ & * ( ) - _ = + [ ] { } | ; : , . < > ?`
- Spaces are not permitted
- Pattern: `^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[!@#$%^&*()\-_=+\[\]{}|;:,.<>?])[^\s]{8,128}$`

---

## 6. Role-Based Access Control

### Overview

- Users are assigned one or more roles.
- Roles are composed of one or more permissions.
- Each API endpoint MUST define one or more required permissions.
- Access is granted if `required_permissions ⊆ user.permissions`.
- Permission checks and ownership checks are **independent** requirements. Both must pass.

### Permissions

> **Implementation note — permission format:** The `Permission` enum in the codebase defines only four underscore-format values: `CUSTOMER_CREATE`, `CUSTOMER_READ`, `CUSTOMER_UPDATE`, `CUSTOMER_DELETE`. Auth and customer (`/api/customers/**`) endpoints enforce these via `@PreAuthorize`. Account and monetary endpoints do not use the `Permission` enum — they perform ownership and role checks directly in the service layer. Extended feature endpoints reuse the same four permission strings or perform role-based checks via the service layer. The colon-format permission names from earlier drafts (`SO:CREATE`, `STATEMENT:READ`, etc.) are **not implemented** in the `Permission` enum and are not enforced by Spring Security annotations.

| Permission (codebase) | Description | Enforced by |
|---|---|---|
| `CUSTOMER_CREATE` | Create a customer profile | `@PreAuthorize` on `POST /api/customers`; also reused by `StandingOrderService.create()` |
| `CUSTOMER_READ` | Read a customer profile | `@PreAuthorize` on `GET /api/customers/{id}`; also reused by `TransactionHistoryService`, `MonthlyStatementService`, `StandingOrderService.list()` |
| `CUSTOMER_UPDATE` | Modify a customer profile | `@PreAuthorize` on `PATCH /api/customers/{id}`; also reused by `StandingOrderService.cancel()` |
| `CUSTOMER_DELETE` | Delete a customer profile | Granted to `ADMIN` only; no `@PreAuthorize` on delete endpoint — admin-role check used instead |

### Default Role Permissions

Granted by `CustomUserPrincipal.buildAuthorities()` at authentication time:

#### CUSTOMER
`ROLE_CUSTOMER` + `CUSTOMER_CREATE` + `CUSTOMER_READ` + `CUSTOMER_UPDATE`

#### ADMIN
`ROLE_ADMIN` + `CUSTOMER_CREATE` + `CUSTOMER_READ` + `CUSTOMER_UPDATE` + `CUSTOMER_DELETE`

### Endpoint Permission Matrix

> Enforcement method: **`@PreAuthorize`** = Spring Security annotation on controller; **Service layer** = role/ownership check inside service class using `SecurityContextHolder` or `CustomUserPrincipal`.

| Method | Endpoint | Actual Enforcement |
|---|---|---|
| `POST` | `/api/auth/register` | Public |
| `POST` | `/api/auth/login` | Public |
| `POST` | `/api/customers` | `@PreAuthorize`: `hasAuthority('CUSTOMER_CREATE') or hasRole('ADMIN')` |
| `PATCH` | `/api/customers/{customerId}` | `@PreAuthorize`: `hasAuthority('CUSTOMER_UPDATE') and @ownershipService.canAccessCustomer(...)` or `hasRole('ADMIN')` |
| `GET` | `/api/customers/{customerId}` | `@PreAuthorize`: `hasAuthority('CUSTOMER_READ') and @ownershipService.canAccessCustomer(...)` or `hasRole('ADMIN')` |
| `GET` | `/api/customers` | `@PreAuthorize`: `hasRole('ADMIN')` |
| `DELETE` | `/api/customers/{customerId}` | `@PreAuthorize`: `hasRole('ADMIN')` |
| `POST` | `/customers/{customerId}/accounts` | Service layer: authenticated user + ownership check (CUSTOMER own customer, ADMIN any) |
| `GET` | `/accounts/{accountId}` | Service layer: authenticated user + ownership check |
| `GET` | `/customers/{customerId}/accounts` | Service layer: authenticated user + ownership check |
| `PUT` | `/accounts/{accountId}` | Service layer: authenticated user + ownership check |
| `DELETE` | `/accounts/{accountId}` | Service layer: authenticated user + ownership check (CUSTOMER can delete own zero-balance account) |
| `POST` | `/accounts/{accountId}/deposit` | Service layer: authenticated user + ownership check |
| `POST` | `/accounts/{accountId}/withdraw` | Service layer: authenticated user + ownership check |
| `POST` | `/accounts/transfer` | Service layer: authenticated user + source account ownership check |
| `GET` | `/accounts/{accountId}/transactions` | Service layer: `hasPermission(caller, "CUSTOMER_READ")` + ownership check |
| `GET` | `/accounts/{accountId}/transactions/export` | Service layer: `hasPermission(caller, "CUSTOMER_READ")` + ownership check |
| `POST` | `/accounts/{accountId}/standing-orders` | Service layer: `hasPermission(caller, "CUSTOMER_CREATE")` + ownership check |
| `GET` | `/accounts/{accountId}/standing-orders` | Service layer: `hasPermission(caller, "CUSTOMER_READ")` + ownership check |
| `DELETE` | `/standing-orders/{standingOrderId}` | Service layer: `hasPermission(caller, "CUSTOMER_UPDATE")` + ownership check |
| `POST` | `/notifications/evaluate` | `ServiceApiKeyFilter` (separate security chain — API key, not JWT) |
| `GET` | `/accounts/{accountId}/statements/{period}` | Service layer: `hasPermission(caller, "CUSTOMER_READ")` + ownership check |
| `GET` | `/accounts/{accountId}/insights` | Service layer: `hasInsightsReadAccess` = `INSIGHTS:READ` OR `CUSTOMER_READ` OR `ROLE_ADMIN` + ownership check |
| `PUT` | `/accounts/{accountId}/transactions/{transactionId}/category` | Service layer: `hasInsightsReadAccess` = `INSIGHTS:READ` OR `CUSTOMER_READ` OR `ROLE_ADMIN` + ownership check |

### Authorization Rules

- Auth and customer endpoints (`/api/customers/**`) use Spring Security `@PreAuthorize` with `hasAuthority()` and `hasRole()` expressions
- Account and monetary operation endpoints have **no** `@PreAuthorize` annotations; all authorization is done in the service layer via `SecurityContextHolder` and an admin/ownership role check
- Extended feature endpoints (transactions, standing orders, statements, insights) have **no** `@PreAuthorize` annotations; authorization done in service layer using `CustomUserPrincipal` authorities
- If a caller is not authenticated (no valid JWT) → `401 UNAUTHORISED`
- If ownership validation fails for a CUSTOMER caller → `401 UNAUTHORISED`
- ADMIN callers bypass ownership checks — a `401` for ownership is never returned to ADMIN
- Role assignments MUST be persisted. Only ADMIN may grant or revoke roles

### JWT Authorization Filter

```
Incoming request
      │
      ▼
Extract Authorization: Bearer <token>
      │
      ├── Missing / malformed ──► 401 UNAUTHORISED
      │
      ▼
Validate JWT signature + expiry
      │
      ├── Expired ──────────────► 401 SESSION_EXPIRED
      ├── Invalid signature ────► 401 UNAUTHORISED
      │
      ▼
Load local user record by sub claim
      │
      ├── isActive = false ─────► 403 ACCOUNT_INACTIVE
      │
      ▼
Check required permissions
      │
      ├── Missing permission ───► 401 UNAUTHORISED
      │
      ▼
Check ownership (CUSTOMER role only)
      │
      ├── Ownership fails ──────► 401 UNAUTHORISED
      │
      ▼
Process request
```

---

## 7. Non-Functional Requirements

| # | Category | Requirement |
|---|---|---|
| NFR-01 | Security | Passwords stored with BCrypt. Never returned in any response or log |
| NFR-02 | Security | Auth failures return generic message — never reveal which field failed |
| NFR-03 | Security | JWT secret read from environment variable only. Never hardcoded |
| NFR-04 | Security | JWT includes `sub`, `roles`, and `exp` claims |
| NFR-05 | Data Integrity | `email` and `accountNumber` on customer are permanently immutable |
| NFR-06 | Monetary Precision | All monetary amounts stored and returned at exactly scale=2 |
| NFR-07 | Idempotency | Deposit, Withdraw, Transfer, and Standing Order execution MUST be idempotent when the same `Idempotency-Key` header is supplied |
| NFR-08 | Atomicity | Transfer MUST be atomic: both debit and credit commit or neither applies |
| NFR-09 | Concurrency | Optimistic locking (`@Version`) on Account entity. First commit wins |
| NFR-10 | Content-Type | All JSON API responses MUST use `application/json`. PDF responses use `application/pdf` |
| NFR-11 | Testing | 100% code coverage across all classes. Verified by JaCoCo before PR approval |
| NFR-12 | Testing | Component and integration tests required per story |
| NFR-13 | Contract | All endpoints pass contract tests against OpenAPI YAML before merging |
| NFR-14 | Audit | All operations logged to append-only `audit_log` table; retained 7 years |

---

## 8. Data Storage Principles

- Core business records MUST be stored in persistent server-side data stores and MUST NOT rely on in-memory-only storage.
- Customer, account, user-role assignment, and transaction records MUST be stored as structured operational data.
- Audit logs MUST be stored separately from normal operational records in append-only durable storage.
- Idempotency records MUST be stored in durable storage for their configured retention window (default 72 hours) so repeated requests can be detected reliably across restarts.
- Deleted resources MUST be removed from normal operational access while their retained records remain stored for audit and regulatory purposes.
- The system MUST be able to retrieve retained records for audit, legal, regulatory, and support purposes throughout the required retention period.
- **Current implementation:** H2 file-based database (`jdbc:h2:file:./data/digitalbankdb`). H2 in-memory is NOT used in this environment.

---

## 9. Data Retention and Audit Policy

- Financial transaction records and customer/account data MUST be retained for **7 years**.
- This 7-year period satisfies the CRA 6-year requirement plus a 1-year buffer, and meets the FINTRAC 5-year minimum.
  - FINTRAC reference: https://fintrac-canafe.canada.ca/guidance-directives/recordkeeping-document/record/fin-eng
- Audit logs MUST be retained for at least 7 years and MUST NOT be modified or deleted during the retention period.
- Transaction records SHOULD be treated as immutable once written. All transaction records (SUCCESS and FAILED) MUST be retained and MUST NOT be deleted.
- "Delete" operations remove the resource from normal operational use; underlying records remain stored.
- Idempotency keys MAY be retained for 24 to 72 hours and MAY be purged thereafter. Purging idempotency data MUST NOT affect transaction, customer/account, or audit log retention.
- Export cache entries (PDF caches) are purged after 72 hours via the `IdempotencyPurgeJob`. This purge MUST NOT affect retained transaction or statement records.
- Statement originals must never be overwritten. Corrections create a new version with an incremented version number.
- Personal data MUST be retained only as long as necessary per PIPEDA Principle 5.

---

## 10. Cross-Cutting Validation and Error Semantics

- All endpoints require authenticated callers except `/api/auth/register` and `/api/auth/login`.
- `CUSTOMER` callers may use only the explicitly allowed self-service features, and only on resources they own.
- `ADMIN` callers bypass ownership checks and may use all operations.
- All monetary amounts are expressed in a single system currency and MUST use scale=2.
- Error responses for `400`, `401`, `403`, `404`, `409`, `410`, and `422` use the shared `ErrorResponse` schema.

| Status Code | Use It When |
|---|---|
| `400` | Malformed syntax, type mismatch, invalid path param format, immutable field in body |
| `401` | No valid token; CUSTOMER attempts admin-only op; caller lacks required permission; ownership failure |
| `403` | Authenticated but account is inactive; cancellation within 24-hour standing order lock window |
| `404` | Resource does not exist, or exists as a retained record but is no longer in normal operational use (e.g. `status=CLOSED`) |
| `409` | Business-state conflict (insufficient funds, delete blocked by active accounts, duplicate idempotency, period not yet closed) |
| `410` | Statement beyond self-service retention window |
| `422` | Well-formed but semantically invalid input (negative amount, incompatible field combination, unknown enum value) |
| `200` / `201` | Success with resource representation |

---

## 11. Implementation Infrastructure

This section documents components found in the implementation that are not explicitly described in any of the three original spec files.

### 11.1 JWT Configuration

```
jwt.secret=${JWT_SECRET:ThisIsADevOnlySecretKeyForHs256JwtTokenMinimum32Chars}
jwt.access-token-expiry=3600       // seconds (1 hour)
jwt.refresh-token-expiry=604800    // seconds (7 days)
```

- JWT payload carries: `sub` (userId UUID), `roles` (list), `exp`, `iat`
- Algorithm: HS256
- The dev fallback secret MUST NOT be used in production. Production MUST set `JWT_SECRET` via environment variable

### 11.2 Security Filter Chain

Implemented in `SecurityConfig`:

- CSRF disabled (stateless API)
- CORS enabled via `CorsConfig`
- Session management: `STATELESS`
- Public endpoints: `/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**`, `/h2-console/**`, `/actuator/health`
- All other requests require JWT authentication via `JwtAuthenticationFilter`
- Custom `CustomAuthenticationEntryPoint` and `CustomAccessDeniedHandler` for structured `ErrorResponse` on auth failures
- `OwnershipService` and `OwnershipValidator` are separate components enforcing resource ownership independently from permission checks

### 11.3 Swagger / API Documentation

- Swagger UI: `GET /swagger-ui.html`
- OpenAPI JSON: `GET /v3/api-docs/**`
- Both are publicly accessible (no auth required)

### 11.4 H2 Developer Console

- Path: `/h2-console`
- Publicly accessible in dev environment
- Database URL: `jdbc:h2:file:./data/digitalbankdb`
- Username: `sa`, Password: (empty)

### 11.5 Actuator

- `GET /actuator/health` — publicly accessible, returns service health status

### 11.6 Standing Order Execution Scheduler

Implemented in `StandingOrderExecutionJob`. Runs three scheduled jobs daily in UTC:

| Job | Cron | Action |
|---|---|---|
| `processOrders` | `1 0 0 * * *` (00:00:01 UTC) | Processes all ACTIVE orders where `nextRunDate` matches today. Marks insufficient-funds orders as `RETRY_PENDING` |
| `firstAttempt` | `0 0 8 * * *` (08:00 UTC) | Retries all `RETRY_PENDING` orders for today. Still-insufficient orders remain `RETRY_PENDING` |
| `finalAttempt` | `0 0 16 * * *` (16:00 UTC) | Final retry of `RETRY_PENDING` orders. Persistent failure sets status to `FAILED_INSUFFICIENT_FUNDS`, persists a `FAILED` transaction record, and triggers a mandatory `StandingOrderFailure` notification |

**Execution idempotency:** Each cycle uses a composite key `standingOrderId:nextRunDate` as the idempotency key stored in `idempotency_record`. A duplicate scheduler trigger for the same cycle performs no action.

**Account closure handling:** If the source account is not found at execution time, the standing order status is set to `TERMINATED` and no transaction is created.

**Holiday shifting:** `nextRunDate` is advanced to the next business day if it falls on a weekend or Canadian Bank Holiday (see §11.7). The frequency cycle is not affected.

**Scheduler thread pool:** Configured in `SchedulerConfig` with pool size 5, thread prefix `banking-scheduler-`, and graceful shutdown (`waitForTasksToCompleteOnShutdown=true`).

### 11.7 Canadian Holiday Service

`CanadianHolidayService` provides holiday detection and next-business-day calculation. Hard-coded federal statutory holidays (non-year-specific rules):

| Holiday | Rule |
|---|---|
| New Year's Day | January 1 |
| Good Friday | 2 days before Easter (computed dynamically — Anonymous Gregorian algorithm) |
| Victoria Day | Last Monday before May 25 (Monday between May 18–24) |
| Canada Day | July 1 |
| Civic Holiday | First Monday in August (Monday on day 1–7 of August) |
| Labour Day | First Monday in September (Monday on day 1–7 of September) |
| Thanksgiving | Second Monday in October (Monday on day 8–14 of October) |
| Remembrance Day | November 11 |
| Christmas Day | December 25 |
| Boxing Day | December 26 |

### 11.8 Idempotency Purge Job

`IdempotencyPurgeJob` runs daily at **03:00 UTC** and purges:
- `idempotency_record` table entries older than `banking.idempotency.purge-after-hours` (default: `72`)
- `export_cache` table entries older than the same threshold

This purge does NOT affect transaction records, account records, audit logs, or statement data.

### 11.9 PDF Export Cache

`ExportCacheEntity` stores PDF bytes keyed by `(account_id, param_hash)`:
- `param_hash` is a SHA-based hash of the export request parameters (date range)
- On repeat identical export requests, the cached PDF is returned without regeneration
- Cache entries are purged after 72 hours by the `IdempotencyPurgeJob`

This mechanism ensures the transaction history PDF export is idempotent (identical requests return the same file).

### 11.10 Category Resolver

`CategoryResolver` assigns spending categories to transactions using case-insensitive keyword matching against the transaction description:
- Keywords are injected from application configuration under `banking.categories.<CategoryName>`
- First matching category wins
- Returns `null` (uncategorised) when no keyword matches
- Used at transaction creation time and referenced in spending insight calculations

### 11.11 Modulo 97 Validator

`Mod97Validator` implements the ISO 7064 MOD-97-10 algorithm for validating `payeeAccount` on standing orders:
- Input: 5–34 characters (after whitespace removal)
- Algorithm: moves first 4 chars to end, converts letters to digits (A=10…Z=35), computes MOD 97. Valid if remainder == 1
- Exposed as a `@ValidPayeeAccount` Bean Validation constraint annotation

### 11.12 Request Logging Filter

`RequestLoggingFilter` logs all incoming HTTP requests. Output goes to:
- Console (standard out)
- Log file: `logs/digital-banking.log`

Log levels: `ROOT=INFO`, `com.group1.banking=INFO`, `org.springframework.security=INFO`

### 11.13 Audit Service

`AuditService` persists an `AuditLogEntity` row on significant operations:

```java
auditService.log(actorId, actorRole, action, resourceType, resourceId, outcome);
```

Called by schedulers on execution outcomes, and should be called on all create/update/delete operations per the Data Retention and Audit Policy. Stored in `audit_log` table with indexes on `actor_id`, `resource_type + resource_id`, and `timestamp`.

### 11.14 Notification Preference Store

`NotificationPreferenceEntity` (table: `notification_preferences`) stores customer opt-in/out preferences per event type. The notification evaluation service reads these preferences when deciding whether to raise optional events. Mandatory events override preferences regardless of stored opt-out values.

---

## 12. US-101 — Register

### Endpoint

```
POST /api/auth/register
Content-Type: application/json
```

### Request Body

```json
{
  "username": "abc@example.com",
  "password": "Secure@123"
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `username` | `String` | Yes | `@Email @NotBlank` — unique in DB |
| `password` | `String` | Yes | `@Pattern` — min 8 chars, 1 uppercase, 1 digit, 1 special char |
| `role` | `String` | No | Optional. Defaults to `CUSTOMER` in service layer |

### Response — `201 Created`

```json
{
  "userId": "3f2a1b4c-...",
  "username": "abc@example.com",
  "roles": ["CUSTOMER"],
  "externalSubjectId": null,
  "customerId": null,
  "isActive": true,
  "createdAt": "2026-04-07T10:00:00Z"
}
```

`passwordHash` MUST NEVER appear in this response or any other.

### Business Rules

- `username` must be unique in the database
- Password is BCrypt-hashed before storage
- Default role is `CUSTOMER`
- `isActive` is set to `true` on registration

### Error Mapping

| HTTP | Code | When |
|---|---|---|
| `201` | — | Registered successfully |
| `409` | `USER_ALREADY_EXISTS` | Email already in the database |
| `422` | `INVALID_EMAIL_FORMAT` | Not a valid email address |
| `422` | `INVALID_PASSWORD_FORMAT` | Password fails complexity check |
| `422` | `MISSING_REQUIRED_FIELD` | `username` or `password` absent |

### Acceptance Criteria

| TC | Scenario | Expected |
|---|---|---|
| TC-101-01 | Valid registration | `201`. `passwordHash` absent from response |
| TC-101-02 | Duplicate username | `409 USER_ALREADY_EXISTS` |
| TC-101-03 | Weak password | `422 INVALID_PASSWORD_FORMAT` |
| TC-101-04 | Invalid email format | `422 INVALID_EMAIL_FORMAT` |
| TC-101-05 | Missing required field | `422 MISSING_REQUIRED_FIELD` |
| TC-101-06 | `passwordHash` in any response | Must be absent — always |

---

## 13. US-102 — Login

### Endpoint

```
POST /api/auth/login
Content-Type: application/json
```

### Request Body

```json
{
  "username": "abc@example.com",
  "password": "Secure@123"
}
```

### Response — `200 OK`

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

| Field | Notes |
|---|---|
| `accessToken` | Short-lived (3600s). Include as `Authorization: Bearer <token>` on every protected request |
| `refreshToken` | Long-lived (604800s) |
| `tokenType` | Always `Bearer` |
| `expiresIn` | Seconds until `accessToken` expires |

### Business Rules

- Wrong password AND unregistered email MUST return the same `401 INVALID_CREDENTIALS` response — never disclose which field failed
- If `isActive = false`, return `403 ACCOUNT_INACTIVE`
- JWT payload includes: `sub` (userId), `roles`, `exp`, `iat`

### Error Mapping

| HTTP | Code | When |
|---|---|---|
| `200` | — | Login successful |
| `401` | `INVALID_CREDENTIALS` | Wrong password or email not found |
| `401` | `SESSION_EXPIRED` | Expired access token used on a protected endpoint |
| `403` | `ACCOUNT_INACTIVE` | Account exists but `isActive = false` |
| `422` | `MISSING_REQUIRED_FIELD` | `username` or `password` absent |

### Acceptance Criteria

| TC | Scenario | Expected |
|---|---|---|
| TC-102-01 | Valid credentials, active account | `200`. All 4 token fields present |
| TC-102-02 | Wrong password | `401 INVALID_CREDENTIALS`. No field hint |
| TC-102-03 | Unregistered email | `401 INVALID_CREDENTIALS`. Same as wrong password |
| TC-102-04 | Correct credentials, inactive account | `403 ACCOUNT_INACTIVE` |
| TC-102-05 | Expired access token on protected endpoint | `401 SESSION_EXPIRED` |
| TC-102-06 | No Authorization header | `401 UNAUTHORISED` |
| TC-102-07 | JWT contains `sub`, `roles`, `exp` | Verified by other endpoints |
| TC-102-08 | Missing field | `422 MISSING_REQUIRED_FIELD` |

---

## 14. US-103 — Create Customer

### Endpoint

```
POST /api/customers
Authorization: Bearer <token>
Content-Type: application/json
```

### Request Body

```json
{
  "name": "Jane Doe",
  "address": "123 Main St, Toronto, ON",
  "type": "PERSON"
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| `name` | `String` | Yes | `@NotBlank @Size(min=2)` |
| `address` | `String` | Yes | `@NotBlank` |
| `type` | `Enum` | Yes | `@NotNull` — `PERSON` or `COMPANY` |

`customerId` is auto-generated by the system. Any `customerId` in the request body is silently ignored.

### Response — `201 Created`

```json
{
  "customerId": 42,
  "name": "Jane Doe",
  "address": "123 Main St, Toronto, ON",
  "type": "PERSON",
  "accounts": [],
  "createdAt": "2026-04-07T10:00:00Z",
  "updatedAt": "2026-04-07T10:00:00Z"
}
```

### Security Constraints

- Enforced by `@PreAuthorize("hasAuthority('CUSTOMER_CREATE') or hasRole('ADMIN')")`

### Error Mapping

| HTTP | Code | When |
|---|---|---|
| `201` | — | Customer created |
| `401` | `UNAUTHORISED` | Missing or invalid token |
| `422` | `MISSING_REQUIRED_FIELD` | Required field absent |
| `422` | `INVALID_CUSTOMER_TYPE` | `type` is not `PERSON` or `COMPANY` |

### Acceptance Criteria

| TC | Scenario | Expected |
|---|---|---|
| TC-103-01 | Valid PERSON | `201`. All fields returned. `accounts = []` |
| TC-103-02 | Valid COMPANY | `201` |
| TC-103-03 | No Bearer token | `401 UNAUTHORISED` |
| TC-103-04 | Missing required field | `422 MISSING_REQUIRED_FIELD` |
| TC-103-05 | Invalid type | `422 INVALID_CUSTOMER_TYPE` |
| TC-103-06 | `customerId` supplied in body | Ignored — system ID used |

---

## 15. US-104 — Update Customer

### Endpoint

```
PATCH /api/customers/{customerId}
Authorization: Bearer <token>
Content-Type: application/json
```

### Request Body

Only fields present in the body are updated. Absent fields stay unchanged.

```json
{
  "name": "Jane Smith",
  "address": "456 New St, Toronto, ON"
}
```

| Field | Type | Required | Behaviour |
|---|---|---|---|
| `name` | `String` | Optional | `@Size(min=2)` if provided |
| `address` | `String` | Optional | No additional constraint |
| `email` | — | **BLOCKED** | Reject entire request → `400 FIELD_NOT_UPDATABLE` |
| `accountNumber` | — | **BLOCKED** | Reject entire request → `400 FIELD_NOT_UPDATABLE` |

**Immutability rule:** Inspect the DTO BEFORE any DB call. If `email` or `accountNumber` is non-null, reject the entire request immediately — even if other fields are valid. No partial updates.

### Response — `200 OK`

```json
{
  "customerId": 42,
  "name": "Jane Smith",
  "address": "456 New St, Toronto, ON",
  "accounts": [],
  "createdAt": "2026-04-07T10:00:00Z",
  "updatedAt": "2026-04-07T11:30:00Z"
}
```

### Security Constraints

- Enforced by `@PreAuthorize("(hasAuthority('CUSTOMER_UPDATE') and @ownershipService.canAccessCustomer(authentication, #customerId)) or hasRole('ADMIN')")`
- CUSTOMER caller must own the specified customer (`OwnershipService.canAccessCustomer`)

### Error Mapping

| HTTP | Code | When |
|---|---|---|
| `200` | — | Customer updated |
| `400` | `FIELD_NOT_UPDATABLE` | `email` or `accountNumber` in request body |
| `401` | `UNAUTHORISED` | Missing or invalid token; or ownership failure |
| `404` | `CUSTOMER_NOT_FOUND` | No customer with that ID |
| `422` | `INVALID_CUSTOMER_TYPE` | `type` not `PERSON` or `COMPANY` |

### Acceptance Criteria

| TC | Scenario | Expected |
|---|---|---|
| TC-104-01 | Update name | `200`. `updatedAt` refreshed. `createdAt` unchanged |
| TC-104-02 | Update address | `200`. Address updated |
| TC-104-03 | `email` in body | `400 FIELD_NOT_UPDATABLE`. No changes made |
| TC-104-04 | `accountNumber` in body | `400 FIELD_NOT_UPDATABLE`. No changes made |
| TC-104-05 | Valid field + immutable field together | `400`. Entire request rejected |
| TC-104-06 | Non-existent `customerId` | `404 CUSTOMER_NOT_FOUND` |
| TC-104-07 | No Bearer token | `401 UNAUTHORISED` |

---

## 16. US-105 — Get Customer

### Endpoint

```
GET /api/customers/{customerId}
Authorization: Bearer <token>
```

### Response — `200 OK`

```json
{
  "customerId": 42,
  "name": "Jane Smith",
  "address": "456 New St, Toronto, ON",
  "type": "PERSON",
  "accounts": [],
  "createdAt": "2026-04-07T10:00:00Z",
  "updatedAt": "2026-04-07T11:30:00Z"
}
```

Read-only. No DB writes. `updatedAt` must be identical on consecutive calls.

### Security Constraints

- Enforced by `@PreAuthorize("(hasAuthority('CUSTOMER_READ') and @ownershipService.canAccessCustomer(authentication, #customerId)) or hasRole('ADMIN')")`
- CUSTOMER caller must own the specified customer

### Error Mapping

| HTTP | Code | When |
|---|---|---|
| `200` | — | Customer returned |
| `400` | `INVALID_IDENTIFIER` | `customerId` cannot be parsed as `Long` |
| `401` | `UNAUTHORISED` | Missing or invalid token; or ownership failure |
| `404` | `CUSTOMER_NOT_FOUND` | No customer with that ID |

### Acceptance Criteria

| TC | Scenario | Expected |
|---|---|---|
| TC-105-01 | Existing customer | `200`. All fields present |
| TC-105-02 | Non-existent ID | `404 CUSTOMER_NOT_FOUND` |
| TC-105-03 | No Bearer token | `401 UNAUTHORISED` |
| TC-105-04 | String in path (e.g. `abc`) | `400 INVALID_IDENTIFIER` |
| TC-105-05 | Two consecutive GETs | `updatedAt` identical on both responses |

---

## 17. AD-001 — List All Customers (Admin)

> **Implementation-derived endpoint.** This endpoint exists in `CustomerController` but was not described in any of the three original spec files.

### Endpoint

```
GET /api/customers
Authorization: Bearer <token>
```

### Response — `200 OK`

A JSON array of all customer records in the system.

### Security Constraints

- ADMIN role required (`@PreAuthorize("hasRole('ADMIN')")`)
- CUSTOMER callers are not permitted to access this endpoint and receive `401`

### Error Mapping

| HTTP | Code | When |
|---|---|---|
| `200` | — | Customer list returned |
| `401` | `UNAUTHORISED` | Missing/invalid token or non-ADMIN caller |

### Acceptance Criteria

- Given an ADMIN caller
- When `GET /api/customers` is submitted
- Then the API returns `200` with all customer records

- Given a CUSTOMER caller
- When `GET /api/customers` is submitted
- Then the API returns `401`

---

## 18. OP-01 — Delete Customer

### Description

Deletes an existing customer when the customer has no active accounts. ADMIN only.

### HTTP Contract

- **Method:** `DELETE`
- **Path:** `/customers/{customerId}`
- **Request Body:** None
- **Expected Response Codes:** `200`, `400`, `401`, `404`, `409`

### Business Rules

- A customer can be deleted only if the customer exists and has no active accounts
- Deleting a customer removes the customer from normal operational use
- No implied account closure is performed by this operation
- Underlying records are retained per the 7-year retention policy

### Security Constraints

- Enforced by `@PreAuthorize("hasRole('ADMIN')")`
- CUSTOMER callers receive `401`

### Validation Rules

- `customerId` must be a valid numeric value greater than 0

### Error Mapping

- Invalid `customerId` format → `400`, `field="customerId"`
- Customer not found → `404`
- Customer has active accounts → `409`
- CUSTOMER caller → `401`

### Edge Cases

- Already deleted or non-existent customer → `404`
- Concurrent deletes → only one succeeds; subsequent return `404`

### Acceptance Criteria

#### Success (200)
```json
{ "message": "Customer deleted successfully" }
```

#### Positive Scenario
- Given ADMIN caller and customer with no active accounts
- When DELETE is submitted with that `customerId`
- Then `200` and customer is removed from normal operational use

#### Negative Scenario 1
- Given no customer for the supplied `customerId`
- Then `404`

#### Negative Scenario 2
- Given customer with one or more active accounts
- Then `409`

#### Negative Scenario 3
- Given a CUSTOMER caller
- Then `401`

---

## 19. OP-02 — Create Account

### Description

Creates a new account for an existing customer as either a checking or savings account.

### HTTP Contract

- **Method:** `POST`
- **Path:** `/customers/{customerId}/accounts`
- **Request Body:**
```json
{
  "accountType": "CHECKING | SAVINGS",
  "balance": "BigDecimal",
  "interestRate": "BigDecimal (required for SAVINGS, forbidden for CHECKING)"
}
```
- **Expected Response Codes:** `201`, `400`, `401`, `404`, `409`, `422`

### Business Rules

- `accountType` is required and must be `CHECKING` or `SAVINGS`
- Newly created accounts MUST have `status=ACTIVE`
- `balance` is required and must be ≥ 0
- For `SAVINGS`: `interestRate` is required
- For `CHECKING`: `interestRate` must not be provided
- `accountNumber` is system-generated on creation *(implementation-derived)*
- `dailyTransferLimit` defaults to `3000.00` *(implementation-derived)*

### Security Constraints

- Service layer authorization: authenticated user extracted from `SecurityContextHolder`
- CUSTOMER callers must own the specified customer (`user.getCustomerId().equals(customerId)`)
- ADMIN callers bypass ownership check

### Validation Rules

- `customerId` must be valid numeric > 0
- `accountType` required: `CHECKING` or `SAVINGS`
- `balance` required, non-negative, at most 2 decimal places
- For `SAVINGS`: `interestRate` required, non-negative, at most 4 decimal places
- For `CHECKING`: `interestRate` must not be present

### Error Mapping

- Invalid `customerId` format → `400`, `field="customerId"`
- Missing/invalid `accountType` → `400`, `field="accountType"`
- Missing/negative `balance` → `400`, `field="balance"`
- Customer not found → `404`
- `interestRate` for CHECKING → `422`, `field="interestRate"`
- `interestRate` missing for SAVINGS → `422`, `field="interestRate"`

### Acceptance Criteria

#### Positive Scenario
- Given existing customer
- When POST with `accountType=SAVINGS`, non-negative `balance`, valid `interestRate`
- Then `201` and created account details

#### Negative Scenario 1 — Customer not found
- Then `404`

#### Negative Scenario 2 — CHECKING with interestRate
- Then `422`, `field="interestRate"`

#### Negative Scenario 3 — Ownership failure
- Given CUSTOMER caller who does not own the customer
- Then `401`

---

## 20. OP-03 — Retrieve Account Details

### Description

Returns the details of a specific active account.

### HTTP Contract

- **Method:** `GET`
- **Path:** `/accounts/{accountId}`
- **Expected Response Codes:** `200`, `400`, `401`, `404`

### Business Rules

- Account must exist and have `status=ACTIVE`
- Returned data includes all Account domain object fields
- `status=CLOSED` accounts are not available for normal operational retrieval → `404`

### Security Constraints

- Service layer authorization: authenticated user extracted from `SecurityContextHolder`
- CUSTOMER callers must own the account (`user.getCustomerId().equals(account.customer.customerId)`)
- ADMIN callers bypass ownership check

### Error Mapping

- Invalid `accountId` format → `400`, `field="accountId"`
- Account not found or CLOSED → `404`
- Ownership failure → `401`

### Acceptance Criteria

#### Positive Scenario
- Given existing ACTIVE account
- When GET with that `accountId`
- Then `200` with account details

#### Negative Scenarios
- Non-existent account → `404`
- Invalid non-numeric `accountId` → `400`
- CUSTOMER ownership failure → `401`

---

## 21. OP-04 — List Customer Accounts

### Description

Returns all active accounts belonging to a specific customer.

### HTTP Contract

- **Method:** `GET`
- **Path:** `/customers/{customerId}/accounts`
- **Expected Response Codes:** `200`, `400`, `401`, `404`

### Business Rules

- Customer must exist to list accounts
- Customer with no accounts returns `200` with empty list
- Response includes only active accounts (CLOSED accounts excluded from normal operational listing)

### Security Constraints

- Service layer authorization: authenticated user extracted from `SecurityContextHolder`
- CUSTOMER callers must own the customer
- ADMIN callers bypass ownership check

### Error Mapping

- Invalid `customerId` format → `400`
- Customer not found → `404`
- Ownership failure → `401`

### Edge Cases

- Customer exists with no accounts → `200` with empty list (not an error)

### Acceptance Criteria

#### Positive Scenario
- Customer with two accounts → `200` with both accounts

#### Negative Scenarios
- Non-existent customer → `404`
- Invalid `customerId` → `400`
- CUSTOMER ownership failure → `401`

---

## 22. OP-05 — Update Account

### Description

Updates mutable account attributes while preserving account type constraints.

### HTTP Contract

- **Method:** `PUT`
- **Path:** `/accounts/{accountId}`
- **Request Body:**
```json
{
  "interestRate": "BigDecimal (SAVINGS only)"
}
```
- **Expected Response Codes:** `200`, `400`, `401`, `404`, `422`

### Business Rules

- Account must exist and have `status=ACTIVE`
- Only SAVINGS accounts expose a mutable field (`interestRate`) through this operation
- CHECKING accounts have no mutable fields exposed — any update request is rejected
- Immutable fields: `accountId`, `customerId`, `accountType`, `balance`, `createdAt`
- Successful update MUST refresh `updatedAt`

### Field Update Matrix

| Field | CHECKING | SAVINGS |
|---|---|---|
| `interestRate` | Not allowed | Updatable |
| All other fields | Not allowed | Not allowed |

### Security Constraints

- Service layer authorization: authenticated user extracted from `SecurityContextHolder`
- CUSTOMER callers must own the account
- ADMIN callers bypass ownership check

### Validation Rules

- `accountId` valid numeric > 0
- Request body must contain `interestRate`; empty body is rejected
- Request body MUST NOT contain fields other than `interestRate`
- `interestRate` for SAVINGS: non-negative, at most 4 decimal places

### Error Mapping

- Invalid `accountId` → `400`
- Empty request body → `400`
- Unsupported/immutable field in body → `400`
- Account not found → `404`
- Negative/excess-precision `interestRate` → `422`, `field="interestRate"`
- `interestRate` for CHECKING → `422`, `field="interestRate"`

### Acceptance Criteria

#### Positive Scenario
- Existing SAVINGS account with valid `interestRate` → `200` with updated details

#### Negative Scenarios
- CHECKING with `interestRate` → `422`
- Non-existent account → `404`
- CUSTOMER ownership failure → `401`
- Immutable field in body (e.g. `balance`) → `400`

---

## 23. OP-06 — Delete Account

### Description

Closes an account when closure rules are satisfied.

### HTTP Contract

- **Method:** `DELETE`
- **Path:** `/accounts/{accountId}`
- **Expected Response Codes:** `200`, `400`, `401`, `404`, `409`

### Business Rules

- Account must exist
- Only accounts with `balance = 0.00` can be deleted
- Deletion sets `status=CLOSED`, sets `deletedAt` and `closedAt` timestamps *(implementation-derived)*
- Account is removed from normal operational access; underlying record is retained

### Security Constraints

- Service layer authorization: authenticated user extracted from `SecurityContextHolder`
- CUSTOMER callers can delete their own zero-balance accounts
- ADMIN callers can delete any zero-balance account

> **Note:** The original spec stated ADMIN only. The codebase service layer allows CUSTOMER to delete their own account provided the balance is 0.

### Error Mapping

- Invalid `accountId` format → `400`
- Account not found → `404`
- Non-zero balance → `409`
- CUSTOMER does not own account → `401`

### Edge Cases

- `balance=0.00` exactly → allowed
- Already deleted account → `404`
- Concurrent delete requests → first succeeds; subsequent return `404`

### Acceptance Criteria

#### Success (200)
```json
{ "message": "Account deleted successfully" }
```

#### Positive Scenario
- ADMIN caller, account with `balance=0` → `200`
- CUSTOMER caller, own account with `balance=0` → `200`

#### Negative Scenarios
- Non-zero balance → `409`
- Non-existent account → `404`
- CUSTOMER does not own account → `401`

---

## 24. OP-07 — Deposit

### Description

Credits funds to an account and records the deposit transaction.

### HTTP Contract

- **Method:** `POST`
- **Path:** `/accounts/{accountId}/deposit`
- **Headers:** `Idempotency-Key: <uuid>` (optional — `required = false` in implementation; behaviour defined when provided)
- **Request Body:**
```json
{
  "amount": "BigDecimal",
  "description": "string"
}
```
- **Expected Response Codes:** `200`, `400`, `401`, `404`, `422`

### Business Rules

- Account must exist with `status=ACTIVE`
- `amount` must be > 0
- Successful deposit increases balance by `amount`
- Successful deposit → `Transaction` with `direction=CREDIT`, `status=SUCCESS`
- Failed deposit → `Transaction` with `status=FAILED`
- Duplicate `Idempotency-Key` → return original outcome without re-applying balance change

### Security Constraints

- Service layer authorization: authenticated user extracted from `SecurityContextHolder`
- CUSTOMER callers must own the account
- ADMIN callers bypass ownership check

### Validation Rules

- `accountId` valid numeric > 0
- `amount` > 0, at most 2 decimal places
- `Idempotency-Key` header: non-empty string (UUID recommended) when provided

### Error Mapping

- Invalid `accountId` format → `400`, `field="accountId"`
- Missing/non-positive `amount` → `422`, `field="amount"`
- Account not found → `404`
- Ownership failure → `401`

### Acceptance Criteria

#### Positive Scenario
- Account balance `100.00` + deposit `25.00` → `200`, balance `125.00`, transaction `CREDIT / SUCCESS`

#### Negative Scenarios
- `amount = 0` → `422`, `field="amount"`
- Non-existent account → `404`
- CUSTOMER ownership failure → `401`
- Duplicate `Idempotency-Key` → `200` with original response, no second balance change

---

## 25. OP-08 — Withdraw

### Description

Debits funds from an account and records the withdrawal transaction.

### HTTP Contract

- **Method:** `POST`
- **Path:** `/accounts/{accountId}/withdraw`
- **Headers:** `Idempotency-Key: <uuid>` (optional — `required = false` in implementation)
- **Request Body:**
```json
{
  "amount": "BigDecimal",
  "description": "string"
}
```
- **Expected Response Codes:** `200`, `400`, `401`, `404`, `409`, `422`

### Business Rules

- Account must exist with `status=ACTIVE`
- `amount` must be > 0
- Balance cannot become negative
- Successful withdrawal → `Transaction` with `direction=DEBIT`, `status=SUCCESS`
- Failed withdrawal → `Transaction` with `status=FAILED`
- Concurrent withdrawals evaluated against latest committed balance; later requests may fail if balance is insufficient after earlier ones commit
- Duplicate `Idempotency-Key` → return original outcome

### Security Constraints

- Service layer authorization: authenticated user extracted from `SecurityContextHolder`
- CUSTOMER callers must own the account
- ADMIN callers bypass ownership check

### Error Mapping

- Invalid `accountId` format → `400`
- Missing/non-positive `amount` → `422`, `field="amount"`
- Account not found → `404`
- Withdrawal would make balance negative → `409`
- Ownership failure → `401`

### Acceptance Criteria

#### Positive Scenario
- Account balance `100.00` − withdraw `20.00` → `200`, balance `80.00`, transaction `DEBIT / SUCCESS`

#### Negative Scenarios
- Insufficient funds (balance `50.00`, withdraw `75.00`) → `409`, balance unchanged
- `amount = -5.00` → `422`, `field="amount"`
- CUSTOMER ownership failure → `401`
- Duplicate `Idempotency-Key` → original response, no second balance change

---

## 26. OP-09 — Transfer Funds

### Description

Transfers funds from one account to another as a single atomic operation.

### HTTP Contract

- **Method:** `POST`
- **Path:** `/accounts/transfer`
- **Headers:** `Idempotency-Key: <uuid>` (optional — `required = false` in implementation)
- **Request Body:**
```json
{
  "fromAccountId": "long",
  "toAccountId": "long",
  "amount": "BigDecimal",
  "description": "string"
}
```
- **Expected Response Codes:** `200`, `400`, `401`, `404`, `409`, `422`

### Business Rules

- Both source and destination accounts must exist with `status=ACTIVE`
- `fromAccountId` and `toAccountId` must be different
- Source balance cannot become negative
- Transfer is **atomic**: both debit and credit succeed or neither applies
- Successful transfer → `DEBIT` transaction on source (`status=SUCCESS`) + `CREDIT` transaction on destination (`status=SUCCESS`)
- Failed transfer → transaction records with `status=FAILED`
- Duplicate `Idempotency-Key` → return original outcome without re-applying

### Security Constraints

- Service layer authorization: authenticated user extracted from `SecurityContextHolder`
- CUSTOMER callers must own the **source** account (`fromAccountId`). Destination ownership is NOT required
- ADMIN callers bypass ownership check

### Validation Rules

- Both `fromAccountId` and `toAccountId` valid numeric > 0
- `fromAccountId ≠ toAccountId`
- `amount` > 0, at most 2 decimal places

### Error Mapping

- Invalid `fromAccountId` or `toAccountId` format → `400`, `field` set to offending field
- Missing/non-positive `amount` → `422`, `field="amount"`
- `fromAccountId == toAccountId` → `422`, `field="toAccountId"`
- Source account not found → `404`
- Destination account not found → `404`
- Insufficient funds on source → `409`
- CUSTOMER does not own source → `401`

### Acceptance Criteria

#### Positive Scenario
- Source `200.00`, destination `50.00`, transfer `75.00` → `200`, source `125.00`, destination `125.00`

#### Negative Scenarios
- Insufficient funds (source `30.00`, transfer `45.00`) → `409`, balances unchanged
- `fromAccountId == toAccountId` → `422`, `field="toAccountId"`
- CUSTOMER does not own source → `401`
- Duplicate `Idempotency-Key` → original response, no second balance change

---

## 27. US-08 — Get Transaction History

### Description

Returns transaction history for an eligible ACTIVE account for a selected date range. CLOSED accounts accessible up to 90 days post-closure. PDF export also available.

### HTTP Contract

**Endpoint 1 — Retrieve Transaction History**

- **Method:** `GET`
- **Path:** `/accounts/{accountId}/transactions`
- **Query Parameters:**
  - `startDate` (optional) — ISO-8601 UTC date. Defaults to 28 days before current date
  - `endDate` (optional) — ISO-8601 UTC date. Defaults to current date. Future date silently overridden to current datetime
- **Expected Response Codes:** `200`, `400`, `401`, `404`, `409`

**Endpoint 2 — Export PDF Statement**

- **Method:** `GET`
- **Path:** `/accounts/{accountId}/transactions/export`
- **Query Parameters:** `startDate` (required), `endDate` (required)
- **Response:** `Content-Type: application/pdf`. PDF with Statement of Activity label, masked account identifier, and effective date range
- PDF export is idempotent — identical requests return the same cached file (see §11.9)

### Response Fields — Retrieve Transaction History (200)

- `accountId`, `startDate`, `endDate` (effective after overrides), `transactionCount`
- `transactions` — ordered PENDING first, then POSTED chronologically:
  - `transactionId`, `amount`, `type`/`direction`, `status`, `timestamp`, `description`, `idempotencyKey` (when present)

### Business Rules

- Default date range: last 28 days
- Maximum date range: 366 days
- Future `endDate` silently overridden to current datetime
- PENDING transactions appear at top, POSTED in chronological order
- Both SUCCESS and FAILED transactions included
- CLOSED accounts: accessible up to 90 days post-closure; after that → `409 ERR_ACC_003`
- Transaction records are retained 7 years and are immutable

### Security Constraints

- Service layer authorization: `hasPermission(caller, "CUSTOMER_READ")` + ownership check via `OwnershipValidator`
- CUSTOMER callers must own the account

### Error Mapping

- Invalid `accountId` → `400`, `field="accountId"`
- Invalid `startDate` format → `400`, `field="startDate"`
- Invalid `endDate` format → `400`, `field="endDate"`
- Date range > 366 days → `400`, `field="endDate"`
- Unauthenticated or lacks permission → `401`
- CUSTOMER does not own account → `401`
- Account not found → `404`
- CLOSED account, 90-day window expired → `409`, `code="ERR_ACC_003"`

### Edge Cases

- No transactions in range → empty list with `transactionCount=0` (not an error)
- Future `endDate` → overridden silently
- CLOSED within 90 days → return history normally
- Identical PDF export requests → same file returned

### Acceptance Criteria

#### Positive Scenario 1 — History for valid date range
- ACTIVE account, CUSTOMER with `CUSTOMER_READ` permission
- `200` with PENDING first, POSTED chronologically, all required fields

#### Positive Scenario 2 — No date range defaults to 28 days
- `200` with transactions for last 28 days, `startDate` reflects system default

#### Positive Scenario 3 — Future endDate overridden
- `200`, `endDate` overridden to current datetime, no error

#### Positive Scenario 4 — PDF export idempotent
- Two identical export requests → same PDF both times

#### Negative Scenario 1 — Lacks CUSTOMER_READ permission
- `401`

#### Negative Scenario 2 — CUSTOMER does not own account
- `401`

#### Negative Scenario 3 — Date range > 366 days
- `400`, `field="endDate"`

#### Negative Scenario 4 — CLOSED account outside 90-day window
- `409`, `code="ERR_ACC_003"`

#### Negative Scenario 5 — Non-numeric accountId
- `400`, `field="accountId"`

---

## 28. US-09 — Standing Order Management

### Description

Create, list, and cancel recurring payment instructions. Standing orders execute automatically on schedule with holiday shifting and retry logic.

### HTTP Contract

**Endpoint 1 — Create Standing Order**

- **Method:** `POST`
- **Path:** `/accounts/{accountId}/standing-orders`
- **Request Body:**
  - `payeeAccount` (string, required) — max 34 chars, must pass Modulo 97 checksum
  - `payeeName` (string, required) — 1–70 chars
  - `amount` (BigDecimal, required) — > 0, 2 decimal places, must not exceed `dailyTransferLimit` (default 3000.00)
  - `frequency` (string, required) — `DAILY`, `WEEKLY`, `MONTHLY`, or `QUARTERLY`
  - `startDate` (UTC datetime, required) — at least 24 hours from creation
  - `endDate` (UTC datetime, optional) — after `startDate` if provided
  - `reference` (string, required) — 1–18 alphanumeric characters
- **Expected Response Codes:** `201`, `400`, `401`, `404`, `409`

**Endpoint 2 — List Standing Orders**

- **Method:** `GET`
- **Path:** `/accounts/{accountId}/standing-orders`
- **Expected Response Codes:** `200`, `400`, `401`, `404`

**Endpoint 3 — Cancel Standing Order**

- **Method:** `DELETE`
- **Path:** `/standing-orders/{standingOrderId}`
- **Expected Response Codes:** `200`, `400`, `401`, `403`, `404`

### Response Fields — Create (201)

`standingOrderId`, `sourceAccountId`, `payeeAccount`, `payeeName`, `amount`, `frequency`, `startDate`, `endDate`, `reference`, `status=ACTIVE`, `nextRunDate` (holiday-adjusted), `message`

### Response Fields — List (200)

`accountId`, `standingOrderCount`, `standingOrders` (each with `standingOrderId`, `payeeName`, `amount`, `frequency`, `status`, `nextRunDate`, `startDate`, `endDate`)

### Business Rules

- Can only be created from an ACTIVE account the caller owns
- `startDate` must be at least 24 hours in the future (no exceptions)
- `amount > 0` and must not exceed `dailyTransferLimit`
- `payeeAccount` must pass Modulo 97 checksum (ISO 7064 MOD-97-10)
- Duplicate detection: identical ACTIVE order (same payeeAccount + amount + frequency) → `409 ERR_SO_DUPLICATE`
- Cancellation within 24 hours of `nextRunDate` → `403 ERR_SO_LOCKED`
- `endDate` passes → status set to `TERMINATED` by system
- Standing order execution: see §11.6 (Scheduler)
- Both SUCCESS and FAILED execution outcomes persisted as TRANSFER transactions

### Execution Lifecycle

| Status | Description |
|---|---|
| `ACTIVE` | Normal operational state |
| `CANCELLED` | Manually cancelled by customer |
| `LOCKED` | Within 24-hour processing lock window |
| `TERMINATED` | `endDate` passed or source account closed |
| `RETRY_PENDING` | First execution attempt failed (insufficient funds); final retry pending at 16:00 UTC |
| `FAILED_INSUFFICIENT_FUNDS` | Both execution attempts at 08:00 and 16:00 UTC failed |

### Security Constraints

- Service layer authorization: authenticated user extracted from `SecurityContextHolder`
- Create: checks `hasPermission(caller, "CUSTOMER_CREATE")`
- List: checks `hasPermission(caller, "CUSTOMER_READ")`
- Cancel: checks `hasPermission(caller, "CUSTOMER_UPDATE")`
- CUSTOMER callers must own the source account

### Error Mapping

- Invalid `accountId` format → `400`
- `payeeAccount` fails Modulo 97 → `400`, `field="payeeAccount"`
- Missing/invalid `payeeName` → `400`
- Non-positive `amount` → `400`, `field="amount"`
- `amount` exceeds daily limit → `400`, `field="amount"`
- Invalid `frequency` → `400`
- `startDate` < 24 hours → `400`, `field="startDate"`
- `endDate` before `startDate` → `400`, `field="endDate"`
- Invalid `reference` → `400`
- Caller lacks permission → `401`
- CUSTOMER does not own account → `401`
- Cancellation within 24h of `nextRunDate` → `403`, `code="ERR_SO_LOCKED"`
- Account not found → `404`
- Standing order not found → `404`
- Duplicate ACTIVE order → `409`, `code="ERR_SO_DUPLICATE"`

### Acceptance Criteria

#### Positive Scenario 1 — Created
- CUSTOMER with `CUSTOMER_CREATE` permission, ACTIVE account, valid details → `201`, `status=ACTIVE`, `nextRunDate` holiday-adjusted

#### Positive Scenario 2 — Listed
- Account with 2 ACTIVE orders → `200`, `standingOrderCount=2`

#### Positive Scenario 3 — Cancelled
- `nextRunDate` > 24h away → `200`, `status=CANCELLED`

#### Positive Scenario 4 — Execution idempotent
- Duplicate execution trigger for same cycle → no second payment

#### Negative Scenario 1 — startDate < 24h
- `400`, `field="startDate"`

#### Negative Scenario 2 — Duplicate order
- `409`, `code="ERR_SO_DUPLICATE"`

#### Negative Scenario 3 — Within processing lock
- `403`, `code="ERR_SO_LOCKED"`

#### Negative Scenario 4 — Lacks CUSTOMER_CREATE permission
- `401`

#### Negative Scenario 5 — Ownership failure
- `401`

#### Negative Scenario 6 — Amount exceeds daily limit
- `400`, `field="amount"`

#### Negative Scenario 7 — payeeAccount fails checksum
- `400`, `field="payeeAccount"`

---

## 29. US-10 — Evaluate Notification Event

### Description

Evaluates an incoming business event to determine whether a customer notification should be raised (RAISED), grouped (GROUPED), or suppressed (SUPPRESSED). Invoked by internal upstream event producers.

### HTTP Contract

- **Method:** `POST`
- **Path:** `/notifications/evaluate`
- **Authentication:** mTLS or API Key. Caller's `ServiceID` must be on the configured allow-list (`banking.notifications.allowed-service-ids`)
- **Request Body:**
  - `eventId` (UUID string, required) — used for deduplication
  - `eventType` (string, required) — must match a defined type in the Event Classification Matrix
  - `accountId` (long, required)
  - `customerId` (long, required)
  - `businessTimestamp` (ISO-8601, required)
  - `payload` (object, optional)
- **Expected Response Codes:** `200`, `400`, `401`, `409`, `422`

### Response Fields (200)

`eventId`, `decision` (RAISED/GROUPED/SUPPRESSED), `decisionReason`, `customerId`, `accountId`, `evaluatedAt`, `mandatoryOverride`

### Event Classification Matrix

| Event Type | Classification | Customer Preference Applies |
|---|---|---|
| `StandingOrderFailure` | Mandatory | No — always raised |
| `StatementAvailability` | Mandatory | No — always raised |
| `UnusualAccountActivity` | Mandatory | No — always raised |
| `StandingOrderCreation` | Optional | Yes — customer may opt out |

### Business Rules

- Mandatory events always produce a notification outcome, even if the customer has opted out
- Optional events are suppressed if the customer has opted out (per `NotificationPreferenceEntity`)
- Multiple events of the same type arriving close together may be grouped or suppressed — mandatory events must never be suppressed
- Every evaluation produces exactly one decision record (`NotificationDecisionEntity`)
- Duplicate `eventId` is detected, discarded, and returns `409`

### Security Constraints

- Caller authenticated via `ServiceApiKeyFilter` (API key, not JWT). `ServiceID` validated against allow-list (`banking.notifications.allowed-service-ids`)
- No JWT permission check is applied. This endpoint is not accessible via normal customer or admin JWT tokens

> **Note:** The original spec referenced `NOTIFICATION:READ` and JWT-based CUSTOMER/ADMIN roles. The codebase uses a separate security filter chain (`ServiceApiKeyFilter`) without any JWT permission enforcement on this endpoint.

### Error Mapping

- Malformed `eventId` → `400`, `field="eventId"`
- Missing `eventType` → `400`, `field="eventType"`
- Invalid `accountId` → `400`, `field="accountId"`
- Invalid `customerId` → `400`, `field="customerId"`
- Malformed `businessTimestamp` → `400`, `field="businessTimestamp"`
- Unauthenticated or unregistered ServiceID → `401`
- Duplicate `eventId` → `409`, `field="eventId"`
- Unknown `eventType` → `422`, `field="eventType"`
- `customerId` not linked to `accountId` → `422`, `field="customerId"`
- Customer not entitled → `422`, `field="customerId"`

### Acceptance Criteria

#### Positive Scenario 1 — Mandatory event always raised
- `200`, `decision=RAISED`, `mandatoryOverride=true` if customer had opted out

#### Positive Scenario 2 — Optional event, opted-in customer
- `200`, `decision=RAISED`

#### Negative Scenario 1 — Optional event, customer opted out
- `200`, `decision=SUPPRESSED`

#### Negative Scenario 2 — Duplicate eventId
- `409`, `field="eventId"`

#### Negative Scenario 3 — Unauthenticated caller
- `401`

#### Negative Scenario 4 — Missing required field
- `400`

#### Negative Scenario 5 — Unknown eventType
- `422`, `field="eventType"`

---

## 30. US-11 — Get Monthly Statement

### Description

Returns the formal monthly statement for an eligible account and a specified closed period. Delivered as a PDF.

### HTTP Contract

- **Method:** `GET`
- **Path:** `/accounts/{accountId}/statements/{period}`
- **Response:** `Content-Type: application/pdf` with Content-Disposition `attachment; filename="statement-{accountId}-{period}.pdf"`
- **Expected Response Codes:** `200`, `400`, `401`, `404`, `409`, `410`

> **Implementation note:** The response is a PDF byte stream, not JSON. The `version` query parameter is **not yet implemented** in the current `StatementController`.

### Statement Content (PDF)

- Account details, statement period
- `openingBalance`, `closingBalance`, `totalMoneyIn`, `totalMoneyOut`
- All transactions for the period (SUCCESS and FAILED), never truncated
- `versionNumber`, `correctionSummary` (on corrected versions), `generatedAt`

### Business Rules

- Statement covers a formally closed calendar month (`YYYY-MM`)
- `openingBalance` must match the closing balance of the prior period's issued statement
- An account with no activity still produces a statement with zero totals
- Corrections create a new version. Original is never overwritten
- Statements beyond the self-service retention window → `410`

### Security Constraints

- Service layer authorization: `hasPermission(caller, "CUSTOMER_READ")` + ownership check
- CUSTOMER callers must own the account

### Error Mapping

- Invalid `accountId` → `400`, `field="accountId"`
- Invalid `period` format (not YYYY-MM) → `400`, `field="period"`
- Lacks `CUSTOMER_READ` permission → `401`
- CUSTOMER does not own account → `401`
- Account not found → `404`, `field="accountId"`
- No statement for period → `404`, `field="period"`
- Period not yet closed → `409`, `field="period"`
- Period beyond retention window → `410`, `field="period"`

### Acceptance Criteria

#### Positive Scenario 1 — Closed period statement
- CUSTOMER with `CUSTOMER_READ` permission, closed period → `200` with PDF

#### Positive Scenario 2 — Corrected version
- Corrected statement at version 2 → `200`, `versionNumber=2`, `correctionSummary` present

#### Positive Scenario 3 — Empty month
- No activity in selected month → `200`, zero totals, empty transaction list

#### Negative Scenario 1 — Lacks CUSTOMER_READ permission → `401`
#### Negative Scenario 2 — Account not found → `404`, `field="accountId"`
#### Negative Scenario 3 — Period not yet closed → `409`, `field="period"`
#### Negative Scenario 4 — Beyond retention window → `410`, direct to support
#### Negative Scenario 5 — Invalid period format → `400`, `field="period"`

---

## 31. US-12 — Spending Insights

### Description

Returns a categorised breakdown of debit spending for a customer's ACTIVE account covering a selected calendar month plus a six-month spending trend. Supports real-time transaction recategorisation.

### HTTP Contract

**Endpoint 1 — Get Spending Insights**

- **Method:** `GET`
- **Path:** `/accounts/{accountId}/insights`
- **Query Parameters:** `year` (required, 4-digit), `month` (required, 1–12)
- **Expected Response Codes:** `200`, `400`, `401`, `404`, `409`

**Endpoint 2 — Recategorise a Transaction**

- **Method:** `PUT`
- **Path:** `/accounts/{accountId}/transactions/{transactionId}/category`
- **Request Body:** `{ "category": "string" }` — must be one of the eight agreed values, case-sensitive
- **Expected Response Codes:** `200`, `400`, `401`, `404`, `422`

### Response Fields — Get Insights (200)

- `accountId`, `period` (`year`, `month`, `isComplete`)
- `totalDebitSpend` — sum of eligible SUCCESS WITHDRAW and TRANSFER-out amounts
- `transactionCount`
- `hasUncategorised`, `hasExcludedDisputes`, `dataFresh`
- `categoryBreakdown` — always 8 entries; percentages sum to exactly 100
- `topTransactions` — 5 largest eligible transactions
- `sixMonthTrend` — exactly 6 entries; zero spend months included

### Response Fields — Recategorise (200)

- `transactionId`, `previousCategory`, `updatedCategory`, `updatedCategoryBreakdown`, `updatedTotalDebitSpend`

### Business Rules

- Spending derived from WITHDRAW and TRANSFER-out transactions with `status=SUCCESS` only
- DEPOSIT and FAILED transactions excluded from all spending totals
- Every eligible transaction mapped to exactly one of the eight categories: Housing, Transport, Food & Drink, Entertainment, Shopping, Utilities, Health, Income
- Categories auto-assigned by `CategoryResolver` using keyword matching on transaction description (§11.10). Unmatched → uncategorised
- All eight categories always returned; percentages sum to exactly 100
- Six-month trend always exactly six entries
- Category change by customer → insight recalculated immediately, updated totals returned in response
- Disputed transactions excluded until resolved
- Insights are NOT financial advice and are NOT a formal account record
- Insights retained 7 years per CRA/FINTRAC

### Security Constraints

- Service layer authorization: `hasInsightsReadAccess` check — passes if the caller has authority `INSIGHTS:READ`, `CUSTOMER_READ`, or role `ROLE_ADMIN`; plus ownership check
- In practice, `INSIGHTS:READ` is never granted (not present in the `Permission` enum), so access is granted via `CUSTOMER_READ` (CUSTOMER and ADMIN roles) or `ROLE_ADMIN`
- CUSTOMER callers must own the account

### Error Mapping

- Invalid `accountId` → `400`, `field="accountId"`
- Invalid/missing `year` → `400`, `field="year"`
- Invalid/missing `month` → `400`, `field="month"`
- Invalid `transactionId` → `400`, `field="transactionId"`
- Account not found → `404`
- Transaction not found → `404`
- Unrecognised `category` → `422`, `field="category"`
- Lacks `CUSTOMER_READ` permission or `ROLE_ADMIN` → `401`
- CUSTOMER does not own account → `401`
- Month not yet started → `409`

### Edge Cases

- No eligible transactions → all eight categories zero, `totalDebitSpend=0.00` (not an error)
- All transactions disputed → `hasExcludedDisputes=true`, zero totals
- Account didn't exist for some trend months → those months show zero spend and `accountExisted=false`
- Current in-progress month → `isComplete=false`
- DEPOSIT recategorised → excluded from totals regardless (exclusion based on direction, not category)
- FAILED transaction recategorised → excluded from totals (exclusion based on status)

### Acceptance Criteria

#### Positive Scenario 1 — Month with eligible activity
- `200`, all 8 categories, percentages sum to 100, 6 trend entries, top 5 transactions

#### Positive Scenario 2 — Empty month returns zero totals
- `200`, all 8 categories with zero values

#### Positive Scenario 3 — Real-time recategorisation
- PUT with `category="Food & Drink"` → `200`, Shopping decreased, Food & Drink increased, all percentages still sum to 100

#### Positive Scenario 4 — Six-month trend always 6 entries
- Inactive months present with zero spend

#### Negative Scenario 1 — Lacks CUSTOMER_READ permission → `401`
#### Negative Scenario 2 — CUSTOMER ownership failure → `401`
#### Negative Scenario 3 — Account not found → `404`
#### Negative Scenario 4 — Unrecognised category ("Groceries") → `422`, `field="category"`
#### Negative Scenario 5 — Future month → `409`
#### Negative Scenario 6 — All transactions excluded → `200`, zero totals, `hasExcludedDisputes=true`
#### Negative Scenario 7 — Non-numeric accountId → `400`, `field="accountId"`

---

## 32. Security and Guardrails

### Forbidden Patterns — Never Do These

```
✗  Storing passwords in plain text
✗  Returning passwordHash in any response, log, or error message
✗  Hardcoding the JWT secret — read from environment variable only
✗  Committing .env or any secrets file to version control
✗  Using RestTemplate — use WebClient for any service-to-service calls
✗  Bypassing JPA with raw SQL
✗  Adding fields to a response not in the OpenAPI spec
✗  Using AI-suggested dependencies without a security audit
✗  Logging sensitive personal data or credentials
```

### Required `.gitignore`

```gitignore
.env
*.env
target/
*.class
application-local.properties
```

### Spring Boot Dependencies (pom.xml reference)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 33. Definition of Done

A story is only done when **all** of the following are true:

| Category | Done When |
|---|---|
| BA Spec | User story, business rules, pre/postconditions, and acceptance criteria (positive + negative GWT) written and approved |
| Dev Spec | OpenAPI YAML in spec branch. Request/response tables complete. Implementation matches contract — no extra fields |
| QA | All test cases automated. 100% coverage via JaCoCo. Contract tests passing. All negative tests implemented |
| Guardrails | No forbidden patterns. Static analysis passed. Secret scanning passed. Dependency audit passed |
| Git | Feature branches only → PR → `develop`. Min 2 peer approvals. CI passing. Meaningful commit messages |
| Docs | OpenAPI YAML valid. Swagger UI renders correctly. README updated |
| Demo Ready | API running. Positive and negative paths demonstrated live. Contract tests shown passing. Guardrails explained |

### Git Workflow

```
feature/<story-name>   ← implementation + tests
      │
      ▼ (PR, 2 approvals, CI passing)
    develop
      │
      ▼
      qa  →  release  →  main
```

### Commit Message Convention

```
spec: defined acceptance criteria for <feature>
api:  implemented <endpoint>
api:  added <business rule> to <service>
test: added negative tests for <scenario>
test: added contract test for <response shape>
fix:  corrected <status code> response for <scenario>
docs: updated README with <endpoint> example
```

---

*This document is the single source of truth for the Digital Banking Platform. Any change to endpoint contracts, error codes, JWT claims, or the data model must be updated here and re-approved before implementation begins. Section 11 (Implementation Infrastructure) documents behaviour found in the codebase that was not captured in earlier drafts.*


### JUnit Testing Specification (Digital Banking Platform)

This section defines the mandatory testing requirements for all components of the system. Every service, controller, repository, and scheduler must include unit and integration tests before merging into develop.

📁 Test Project Structure
src/test/java
└── com.group1.banking
    ├── controller
    ├── service
    ├── repository
    ├── scheduler
    ├── util
    └── integration
🧪 Testing Levels
## Unit Tests (Service Layer)
Use JUnit 5 + Mockito
No Spring context
Mock all external dependencies
Must cover:
Business logic validation
Branch conditions (if/else, switch)
Exception handling paths
Edge cases
## Controller Tests (Web Layer)
Use @WebMvcTest
Mock service layer
Must validate:
**HTTP** status codes
Request validation errors (**400**)
Authorization errors (**401**/**403**)
Response structure correctness
## Repository Tests (Data Layer)
Use @DataJpaTest
In-memory DB (H2)
Must validate:
Entity mapping correctness
Custom query methods
Constraints (**NOT** **NULL**, **UNIQUE**)
ID generation strategy
## Scheduler Tests (Critical)
Must **NOT** rely on cron execution
Scheduler methods must be called manually
Must test:
Due order selection logic
Retry handling (RETRY_PENDING)
Idempotency prevention
Balance checks
Transaction creation
Next run date calculation
💳 Standing Order Testing (**MANDATORY**)

For StandingOrderExecutionJob, tests must cover:

Successful debit of source account Successful credit of payee account Transaction creation (**SUCCESS** / **FAILED**) Retry logic (08:00 / 16:00 execution paths) Final failure handling (insufficient funds) Idempotency duplicate prevention Status transitions (**ACTIVE** / RETRY_PENDING / **FAILED** / **COMPLETED**) 💰 Transaction Module Testing

Must verify:

**UUID** generation for transaction_id Correct debit/credit values **SUCCESS** vs **FAILED** transaction creation Idempotency key uniqueness Timestamp correctness 🔐 Security Testing

Must include:

**JWT** authentication validation Role-based access control Unauthorized access → **401**/**403** Invalid token handling ⚠️ Exception Handling Tests

Must validate mapping:

ResourceNotFoundException → **404** BusinessStateException → **422** SemanticValidationException → **400** PermissionDeniedException → **403** ⏱ Scheduler Testing Rules Never rely on @Scheduled timing Call methods directly: processOrders() attemptExecution() finalExecutionAttempt() 🧾 Integration Testing Use @SpringBootTest Test full flow without mocking core services Validate: Standing order end-to-end execution Account balance consistency Transaction persistence Idempotency behavior 📊 Coverage Requirements

Minimum coverage thresholds:

Service layer: 85%+ Controller layer: 80%+ Scheduler layer: 90%+ Overall system: 85%+ 🧪 Test Naming Convention should_<expectedBehavior>_when_<condition> Examples: shouldDebitAndCreditAccounts_whenStandingOrderExecutes shouldThrowException_whenInsufficientBalance shouldReturn403_whenUserLacksPermission shouldPreventDuplicateExecution_whenIdempotencyExists 🔄 Mocking Guidelines Mock only external dependencies: Repositories External APIs Do **NOT** mock: Business logic inside service under test