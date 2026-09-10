# Implementation Plan: Savings Goal Tracker

**Branch**: `001-savings-goal-tracker` | **Date**: June 5, 2026 | **Spec**: [specs/001-savings-goals/spec.md](../spec.md)

**Input**: Feature specification from `specs/001-savings-goals/spec.md`

## Summary

The Savings Goal Tracker is a new feature added to the Accounts section that allows customers to set, monitor, and manage one savings goal per account. Progress is calculated automatically by pulling the live `account.balance` — no manual input required. The "What are you saving for?" question uses a dropdown of preset options (Emergency Fund, Travel, Tuition, Home, Car, Retirement) with a custom free-text fallback. Overdue goals remain visible with an OVERDUE status badge. All account types (SAVINGS, CHEQUING, RRSP, GIC) are eligible if `account.status = 'ACTIVE'` and `account.deleted_at IS NULL`.

## Technical Context

**Language/Version**: Java (Spring Boot 3.x), JavaScript/React (Vite)

**Primary Dependencies**:

- Backend: Spring Data JPA, Spring Security (JWT), Spring Validation
- Frontend: React, Axios, Vite

**Storage**: MySQL 8.0+ (existing schema)

**Testing**:

- Backend: JUnit 5, Mockito
- Frontend: Vitest, React Testing Library

**Target Platform**: Web (responsive desktop/mobile)

**Project Type**: Full-stack web application (feature addition to existing banking platform)

**Performance Goals**:

- Goal creation: < 200ms
- Goal fetch (single or bulk): < 500ms
- Progress calculation: real-time on read

**Constraints**:

- No writes to `account.balance`, `bank_transaction`, or `standing_orders`
- One active goal per account per customer (unique constraint)
- JWT authentication required on all endpoints
- Soft deletes only (set `deleted_at`)

**Scale/Scope**:

- Leverages existing customer/account hierarchy (no new user management)
- Supports multi-account customers
- Integrates into existing Accounts page

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

- [x] **API contract impacts identified**: 5 new endpoints (POST/GET/PUT for goals, GET bulk, DELETE). DTOs: `SavingsGoalRequest`, `SavingsGoalResponse`, `SavingsGoalListResponse`. No changes to existing Account or Customer DTOs.
- [x] **Contract-affecting changes include same-PR updates**: Controller tests for all 5 endpoints + `SavingsGoalService` tests. Frontend `useGoals` hook + `SavingsGoalCard` component tests. Audit log integration tested.
- [x] **Error semantics standardized**: Backend errors (409 duplicate, 400 validation, 403 forbidden, 404 not found) emitted via `GlobalExceptionHandler` with stable error codes. Frontend maps all codes in `src/api/axiosClient.js` with user-facing messages.
- [x] **Security/CORS reviewed**: All endpoints protected by JWT `@PreAuthorize` checks. Account ownership validated via `account.customer_id == authenticated user.customer_id`. CORS unchanged (existing settings apply). No new endpoints bypass `SecurityConfig`.
- [x] **Environment parity confirmed**: All environment-specific URLs already proxied through `vite.config.js`. Goal endpoints inherit proxy setup. No hardcoded infrastructure hostnames.
- [x] **Layer ownership preserved**: Business invariants (progress calculation, status derivation, validation rules) in `SavingsGoalService`. Frontend pre-validation is UX-only. Backend is final authority.
- [x] **Testability gate defined**:
  - Success: POST create, GET single, GET bulk, PUT edit
  - Auth/validation: 403 ownership check, 400 invalid amount/date, 409 duplicate
  - Business failure: 404 account not found, deleted, inactive
- [x] **No silent degradation**: Auth failures caught by `@PreAuthorize`. Invalid customer_id returns 403. Missing account returns 404. No graceful fallbacks that hide errors.

**GATE STATUS**: ✅ **PASSED** — All principles satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/001-savings-goals/
├── spec.md              # Feature specification
├── plan.md              # This file (implementation plan)
├── research.md          # Phase 0: Resolved clarifications (generated below)
├── data-model.md        # Phase 1: Entity design + schema
├── quickstart.md        # Phase 1: Validation guide + runnable scenarios
├── contracts/
│   ├── SavingsGoalRequest.md        # POST/PUT request contract
│   ├── SavingsGoalResponse.md       # GET/POST/PUT response contract
│   └── ErrorCodes.md                # Error code mapping
└── checklists/
    └── requirements.md              # Quality checklist
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/group1/banking/
│   ├── entity/
│   │   └── SavingsGoal.java                    # JPA entity for savings_goals table
│   ├── dto/
│   │   ├── SavingsGoalRequest.java             # POST/PUT request DTO
│   │   └── SavingsGoalResponse.java            # GET/POST/PUT response DTO
│   ├── service/
│   │   └── SavingsGoalService.java             # Progress calc, status derivation, validation
│   ├── controller/
│   │   └── SavingsGoalController.java          # 5 endpoints (CREATE, GET, GET_BULK, UPDATE, DELETE)
│   └── repository/
│       └── SavingsGoalRepository.java          # JPA repository with custom queries
├── src/test/java/com/group1/banking/
│   ├── controller/
│   │   └── SavingsGoalControllerTest.java      # Happy path + error cases + auth tests
│   ├── service/
│   │   └── SavingsGoalServiceTest.java         # Progress/status calculation, validation
│   └── repository/
│       └── SavingsGoalRepositoryTest.java      # Query tests
└── migrations/
    └── V001__create_savings_goals.sql          # Schema creation (Phase 1)

frontend/
├── src/
│   ├── hooks/
│   │   └── useGoals.js                         # React hook: fetch, create, update, delete
│   ├── components/
│   │   ├── SavingsGoalCard.jsx                 # Goal display (progress bar, %, status badge)
│   │   ├── GoalProgressBar.jsx                 # Reusable progress bar component
│   │   ├── GoalCreationFlow.jsx                # 3-step Q form + review screen
│   │   ├── GoalEditForm.jsx                    # Edit form (pre-populated)
│   │   └── GoalDeleteConfirmation.jsx          # Delete confirmation modal
│   ├── api/
│   │   └── goals.js                            # Axios wrapper for goal endpoints
│   └── pages/
│       └── AccountsPage.jsx                    # Modified to include goal tracker below account list
└── src/test/
    ├── components/
    │   └── SavingsGoalCard.test.jsx            # Component tests
    ├── hooks/
    │   └── useGoals.test.js                    # Hook tests
    └── api/
        └── goals.test.js                       # API call tests
```

**Structure Decision**: Full-stack feature with dedicated layers. Backend service encapsulates progress/status calculation and validation. Frontend components handle UI flows (creation, edit, delete) with React hooks for state management. Separates concerns per existing Voltio architecture.

## Complexity Tracking

| Violation                                            | Why Needed                                    | Simpler Alternative Rejected Because                                                                                     |
| ---------------------------------------------------- | --------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| Progress calc as derived field (not stored)          | Real-time accuracy as account balance changes | Storing progress would require updating savings_goals on every transaction; creates race conditions and audit complexity |
| 5 separate endpoints instead of single bulk endpoint | RESTful design + independent testability      | Single endpoint would couple create/read/update/delete into one mutation, breaking independent deployment                |
| Soft delete pattern (deleted_at)                     | Consistency with existing schema              | Hard delete would violate audit trail requirements and existing pattern used by account/customer                         |

---

## PHASE 0: Research & Clarifications

### Research Tasks Dispatched

1. **Goal Name Preset Options** — Confirmed: Emergency Fund, Travel, Tuition, Home, Car, Retirement + custom free-text
2. **Balance Source** — Confirmed: `account.balance` (DECIMAL 19,2) pulled at read time; no manual override
3. **Eligible Account Types** — Confirmed: All types (SAVINGS, CHEQUING, RRSP, GIC) eligible if `status = 'ACTIVE'`
4. **Ownership Validation Chain** — Confirmed: `users.customer_id → customers.customer_id → account.customer_id`
5. **Overdue Behavior** — Confirmed: Overdue goals remain visible with OVERDUE status badge; no archiving
6. **Status Calculation** — Confirmed: Derived on every read from `account.balance` and `target_date`
7. **Theme Support** — Confirmed: Both Classic and Neon themes required per Voltio Constitution

### Resolved Clarifications

**Q: How to handle existing goals when account balance changes?**
A: Progress percentage recalculates automatically on every GET. No manual updates needed. Derived from live `account.balance`.

**Q: Should goal deletion free up the account for a new goal?**
A: Yes. Soft delete by setting `deleted_at`. Account then shows "Add a goal" prompt immediately.

**Q: Can customers create goals for accounts in CLOSED or INACTIVE state?**
A: No. POST validation rejects if `account.status ≠ 'ACTIVE'` or `account.deleted_at IS NOT NULL`. Returns 404.

**Q: How to display time remaining for overdue goals?**
A: Calculated as `target_date - TODAY`. If negative, flip to status=OVERDUE and return `time_remaining_days=0`. Display "Overdue by X days" in UI.

**Q: Should goal amounts support cents (e.g., $1234.56)?**
A: Yes. Use DECIMAL(19,2) to match existing `account.balance` precision.

**Q: Can multiple customers share an account for savings goals?**
A: No. Goal ownership is scoped by `customer_id`. Multiple users in the same customer record can view/edit goals; different customers cannot.

---

## PHASE 1: Design & Contracts

### 1. Data Model

**Entity: SavingsGoal**

```java
@Entity
@Table(name = "savings_goals")
public class SavingsGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long goalId;

    @Column(nullable = false)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 255)
    private String goalName;        // Preset or custom free-text

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SavingsGoalStatus status;  // NOT_STARTED, IN_PROGRESS, ACHIEVED, OVERDUE

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Progress percentage is DERIVED on read, not stored
    @Transient
    private BigDecimal progressPercentage;

    @Transient
    private Long timeRemainingDays;

    @Transient
    private BigDecimal currentBalance;
}
```

**Database Schema**

```sql
CREATE TABLE savings_goals (
    goal_id             BIGINT          PRIMARY KEY AUTO_INCREMENT,
    customer_id         BIGINT          NOT NULL,
    account_id          BIGINT          NOT NULL,
    goal_name           VARCHAR(255)    NOT NULL,
    target_amount       DECIMAL(19,2)   NOT NULL,
    target_date         DATE            NOT NULL,
    status              ENUM('NOT_STARTED', 'IN_PROGRESS', 'ACHIEVED', 'OVERDUE') NOT NULL DEFAULT 'NOT_STARTED',
    deleted_at          TIMESTAMP       NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_sg_customer_account UNIQUE (customer_id, account_id),
    CONSTRAINT fk_sg_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CONSTRAINT fk_sg_account FOREIGN KEY (account_id) REFERENCES account(account_id)
);

CREATE INDEX idx_sg_customer_id ON savings_goals (customer_id);
CREATE INDEX idx_sg_account_id ON savings_goals (account_id);
CREATE INDEX idx_sg_status ON savings_goals (status);
```

**Enum: SavingsGoalStatus**

```java
public enum SavingsGoalStatus {
    NOT_STARTED,   // account.balance = 0 AND target_date ≥ today
    IN_PROGRESS,   // account.balance > 0 AND < target_amount AND target_date ≥ today
    ACHIEVED,      // account.balance ≥ target_amount
    OVERDUE        // target_date < today AND account.balance < target_amount
}
```

### 2. Contract Specifications

**File**: [contracts/SavingsGoalRequest.md](contracts/SavingsGoalRequest.md)

````markdown
# SavingsGoalRequest Contract

Used for POST /accounts/{account_id}/goals and PUT /accounts/{account_id}/goals/{goal_id}

## Fields

| Field         | Type              | Required | Constraints                              |
| ------------- | ----------------- | -------- | ---------------------------------------- |
| goal_name     | string            | yes      | 1-255 chars; preset value or custom text |
| target_amount | decimal(19,2)     | yes      | > 0                                      |
| target_date   | date (YYYY-MM-DD) | yes      | ≥ today                                  |

## Example

```json
{
  "goal_name": "Travel",
  "target_amount": 5000.0,
  "target_date": "2026-12-31"
}
```
````

## Validation

- Returns 400 if goal_name is empty
- Returns 400 if target_amount ≤ 0
- Returns 400 if target_date is in the past (create only)
- Returns 400 if target_date is in the past (edit requires explicit user action)

````

**File**: [contracts/SavingsGoalResponse.md](contracts/SavingsGoalResponse.md)

```markdown
# SavingsGoalResponse Contract

Used for GET, POST, and PUT responses

## Fields

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| goal_id | integer | savings_goals.goal_id | Unique goal identifier |
| account_id | integer | savings_goals.account_id | Linked account |
| account_number | string | account.account_number | Display only |
| account_type | enum | account.account_type | SAVINGS, CHEQUING, RRSP, GIC |
| goal_name | string | savings_goals.goal_name | Preset or custom |
| target_amount | decimal(19,2) | savings_goals.target_amount | Target amount |
| target_date | date | savings_goals.target_date | Goal deadline |
| current_balance | decimal(19,2) | account.balance | Live balance (read-time) |
| progress_percentage | decimal(5,2) | Calculated | (current_balance / target_amount) * 100, capped at 100 |
| time_remaining_days | integer | Calculated | target_date - TODAY, ≥ 0, shown as 0 if overdue |
| status | enum | Calculated | NOT_STARTED, IN_PROGRESS, ACHIEVED, OVERDUE |
| created_at | datetime | savings_goals.created_at | ISO 8601 |
| updated_at | datetime | savings_goals.updated_at | ISO 8601 |

## Example (In Progress)

```json
{
  "goal_id": 1,
  "account_id": 42,
  "account_number": "ACC-00042",
  "account_type": "SAVINGS",
  "goal_name": "Travel",
  "target_amount": 5000.00,
  "target_date": "2026-12-31",
  "current_balance": 1200.00,
  "progress_percentage": 24.00,
  "time_remaining_days": 209,
  "status": "IN_PROGRESS",
  "created_at": "2026-06-05T10:00:00Z",
  "updated_at": "2026-06-05T10:00:00Z"
}
````

## Example (Overdue)

```json
{
  "goal_id": 2,
  "account_id": 87,
  "account_number": "ACC-00087",
  "account_type": "CHEQUING",
  "goal_name": "Emergency Fund",
  "target_amount": 3000.0,
  "target_date": "2026-03-01",
  "current_balance": 800.0,
  "progress_percentage": 26.67,
  "time_remaining_days": 0,
  "status": "OVERDUE",
  "created_at": "2026-05-01T14:30:00Z",
  "updated_at": "2026-05-01T14:30:00Z"
}
```

````

**File**: [contracts/ErrorCodes.md](contracts/ErrorCodes.md)

```markdown
# Savings Goal Error Codes

Emitted by GlobalExceptionHandler; mapped in frontend by axiosClient.js

| HTTP | Code | Reason | Frontend Handling |
|------|------|--------|-------------------|
| 400 | INVALID_TARGET_AMOUNT | target_amount ≤ 0 | "Target amount must be greater than $0" |
| 400 | INVALID_TARGET_DATE | target_date is in the past (create) | "Target date must be in the future" |
| 400 | MISSING_REQUIRED_FIELD | goal_name, target_amount, or target_date is missing | "[Field name] is required" |
| 409 | GOAL_ALREADY_EXISTS | Goal already exists for this account | "This account already has a goal. Edit or delete it first." |
| 403 | UNAUTHORIZED_ACCOUNT_ACCESS | account.customer_id ≠ authenticated customer_id | "You do not have permission to access this account" |
| 404 | ACCOUNT_NOT_FOUND | Account not found, deleted, or inactive | "Account not found or is inactive" |
| 404 | GOAL_NOT_FOUND | Goal not found or already deleted | "Goal not found" |
| 500 | INTERNAL_SERVER_ERROR | Unexpected backend error | "An error occurred. Please try again." |
````

### 3. Contracts Created

See `specs/001-savings-goals/contracts/` directory for full contract definitions.

### 4. Data Model Document

See [specs/001-savings-goals/data-model.md](data-model.md) for complete entity relationships and validation rules.

### 5. Quick Start Validation Guide

**File**: [specs/001-savings-goals/quickstart.md](quickstart.md)

This guide documents:

- Prerequisites (authenticated session, account setup)
- Runnable scenarios (create goal, view with different balances, edit, delete, check overdue)
- Expected outcomes for each scenario
- Theme-specific UI validation (Classic and Neon)

---

## Agent Context Update

Updated `.github/copilot-instructions.md` to point to this plan:

```markdown
<!-- SPECKIT START -->

For additional context about the implementation plan for Savings Goal Tracker,
read the current plan at specs/001-savings-goals/plan.md

<!-- SPECKIT END -->
```

---

## Constitution Re-Check (Post-Phase 1)

✅ **All gates remain PASSED after Phase 1 design**

- API contracts finalized (SavingsGoalRequest, SavingsGoalResponse, ErrorCodes)
- Error semantics mapped (400, 409, 403, 404, 500 → user-facing messages)
- Security checks confirmed (customer ownership validated on all endpoints)
- Layer boundaries maintained (business logic in service, UI flow in React components)
- Test coverage defined (5 happy paths + 8 error scenarios minimum)

---

## Generated Artifacts Summary

✅ **Phase 1 Complete**

- [x] `research.md` — Clarifications resolved, technical decisions justified
- [x] `data-model.md` — Entity schema, relationships, validation rules
- [x] `contracts/SavingsGoalRequest.md` — Request DTO specification
- [x] `contracts/SavingsGoalResponse.md` — Response DTO specification
- [x] `contracts/ErrorCodes.md` — Error code mapping
- [x] `quickstart.md` — Validation guide + runnable scenarios
- [x] Agent context updated

**Next Step**: Run `/speckit.tasks` to generate actionable, dependency-ordered tasks.md
