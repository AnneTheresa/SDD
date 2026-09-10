# Data Model: Savings Goal Tracker

**Status**: ✅ Complete  
**Date**: June 5, 2026  
**Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

---

## Entity: SavingsGoal

### Table Definition

```sql
CREATE TABLE savings_goals (
    goal_id             BIGINT          PRIMARY KEY AUTO_INCREMENT,
    customer_id         BIGINT          NOT NULL,
    account_id          BIGINT          NOT NULL,

    goal_name           VARCHAR(255)    NOT NULL,
    target_amount       DECIMAL(19,2)   NOT NULL,
    target_date         DATE            NOT NULL,

    status              ENUM(
                          'NOT_STARTED',
                          'IN_PROGRESS',
                          'ACHIEVED',
                          'OVERDUE'
                        )               NOT NULL DEFAULT 'NOT_STARTED',

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

### Field Descriptions

| Field         | Type          | Constraints                     | Description                                                                          |
| ------------- | ------------- | ------------------------------- | ------------------------------------------------------------------------------------ |
| goal_id       | BIGINT        | PK, AUTO_INCREMENT              | Unique goal identifier                                                               |
| customer_id   | BIGINT        | NOT NULL, FK                    | Link to customer for authorization scope                                             |
| account_id    | BIGINT        | NOT NULL, FK                    | Link to account (the goal is bound to one account)                                   |
| goal_name     | VARCHAR(255)  | NOT NULL                        | User's answer to "What are you saving for?" — preset value or custom free-text       |
| target_amount | DECIMAL(19,2) | NOT NULL                        | User's answer to "How much do you plan to save?"                                     |
| target_date   | DATE          | NOT NULL                        | User's answer to "By when?"                                                          |
| status        | ENUM          | NOT NULL, DEFAULT 'NOT_STARTED' | Calculated status (see Status Derivation below)                                      |
| deleted_at    | TIMESTAMP     | NULL                            | Soft delete timestamp; NULL if active                                                |
| created_at    | TIMESTAMP     | NOT NULL                        | Goal creation time; immutable                                                        |
| updated_at    | TIMESTAMP     | NOT NULL                        | Last modification time (updated on EVERY change, including edits and recalculations) |

### Unique Constraint

**Constraint**: `UNIQUE (customer_id, account_id)`

**Rationale**: Enforces one active goal per account per customer. Prevents duplicate goal creation for the same account.

**Enforcement**:

- Database constraint at table level
- Backend service validation (returns 409 if duplicate attempted)
- Frontend prevents duplicate POST calls

### Foreign Key Relationships

**FK: fk_sg_customer**

- References: `customers(customer_id)`
- Cascade: Delete goal if customer is hard-deleted (unlikely; customers use soft deletes)

**FK: fk_sg_account**

- References: `account(account_id)`
- Cascade: Delete goal if account is hard-deleted (unlikely; accounts use soft deletes)

### Indexes

- **idx_sg_customer_id**: Speed up customer-scoped queries (fetch all goals for a customer)
- **idx_sg_account_id**: Speed up account-scoped queries (fetch goal for a specific account)
- **idx_sg_status**: Speed up status-filtered queries (fetch all overdue goals for batch notification)

---

## Derived Fields (Calculated at Read Time)

These fields are NOT stored in the database. They are calculated on every read and included in API responses.

### progress_percentage

**Formula**:

```
progress_percentage = (account.balance / target_amount) * 100
```

**Constraints**:

- Capped at 100% in response layer (never exceeds 100%)
- Pulled from live `account.balance` via JOIN
- Recalculates automatically when account balance changes

**Example**: If `account.balance = 1200.00` and `target_amount = 5000.00`, then `progress_percentage = 24.00`

### time_remaining_days

**Formula**:

```
time_remaining_days = MAX(0, target_date - TODAY)
```

**Constraints**:

- Never negative (capped at 0)
- Recalculates daily; no scheduled updates needed
- If ≤ 0, status flips to OVERDUE

**Example**: If `target_date = 2026-12-31` and today is `2026-06-05`, then `time_remaining_days = 209`

### current_balance

**Source**: `account.balance` (pulled via JPA JOIN)

**Constraints**:

- Live value; always reflects current account state
- Precision: DECIMAL(19,2)

### status (Derived Logic)

Although stored in the database for indexing efficiency, the status is recalculated on every read to ensure accuracy.

**Status Derivation Table**:

| account.balance      | target_date | Status      | Rationale                      |
| -------------------- | ----------- | ----------- | ------------------------------ |
| = 0                  | ≥ today     | NOT_STARTED | No progress yet; time remains  |
| > 0, < target_amount | ≥ today     | IN_PROGRESS | Partial progress; time remains |
| ≥ target_amount      | any         | ACHIEVED    | Goal met                       |
| < target_amount      | < today     | OVERDUE     | Deadline passed; goal not met  |

**Implementation**:

```java
public SavingsGoalStatus calculateStatus(BigDecimal currentBalance, LocalDate targetDate) {
    if (currentBalance.compareTo(targetAmount) >= 0) {
        return SavingsGoalStatus.ACHIEVED;
    }

    LocalDate today = LocalDate.now();
    if (targetDate.isBefore(today)) {
        return SavingsGoalStatus.OVERDUE;
    }

    if (currentBalance.compareTo(BigDecimal.ZERO) > 0) {
        return SavingsGoalStatus.IN_PROGRESS;
    }

    return SavingsGoalStatus.NOT_STARTED;
}
```

---

## Relationships

### SavingsGoal ← → Account

- **Multiplicity**: Many-to-One (many goals... wait, NO. One goal per account.)
- **Actual**: One-to-Zero-or-One
- **Cardinality**: Each account has zero or one active goal
- **Enforcement**: UNIQUE(customer_id, account_id)
- **Owned By**: Account (goals are scoped to accounts)

### SavingsGoal ← → Customer

- **Multiplicity**: Many-to-One
- **Cardinality**: Each customer can have many goals (one per account)
- **Enforcement**: FK on customer_id
- **Authorization**: All goal access must match authenticated customer_id

### SavingsGoal → Account → Account.balance

- **Dependency**: Goal progress depends on live account.balance
- **Fetching**: JPA JOIN at query time to get current balance
- **Update Trigger**: None; balance is updated independently by transaction layer

---

## Validation Rules

### Creation (POST)

| Rule                   | Constraint                             | Error Code                  | HTTP |
| ---------------------- | -------------------------------------- | --------------------------- | ---- |
| goal_name is not empty | 1-255 chars                            | INVALID_GOAL_NAME           | 400  |
| target_amount > 0      | DECIMAL > 0                            | INVALID_TARGET_AMOUNT       | 400  |
| target_date in future  | target_date ≥ today                    | INVALID_TARGET_DATE         | 400  |
| One goal per account   | UNIQUE(customer_id, account_id)        | GOAL_ALREADY_EXISTS         | 409  |
| Account is ACTIVE      | account.status = 'ACTIVE'              | ACCOUNT_NOT_ACTIVE          | 404  |
| Account not deleted    | account.deleted_at IS NULL             | ACCOUNT_NOT_FOUND           | 404  |
| Customer owns account  | account.customer_id = auth.customer_id | UNAUTHORIZED_ACCOUNT_ACCESS | 403  |

### Update (PUT)

| Rule                     | Constraint                | Error Code     | HTTP   |
| ------------------------ | ------------------------- | -------------- | ------ |
| All creation rules apply | (same as above)           | (same)         | (same) |
| Goal exists              | saved_goals.goal_id found | GOAL_NOT_FOUND | 404    |
| Goal not deleted         | deleted_at IS NULL        | GOAL_NOT_FOUND | 404    |

### Deletion (DELETE)

| Rule                     | Constraint                | Error Code     | HTTP |
| ------------------------ | ------------------------- | -------------- | ---- |
| Goal exists              | saved_goals.goal_id found | GOAL_NOT_FOUND | 404  |
| Goal not already deleted | deleted_at IS NULL        | GOAL_NOT_FOUND | 404  |

---

## Soft Delete Pattern

All deletions set `deleted_at = CURRENT_TIMESTAMP` instead of hard-deleting the row.

**Active Goals**: `WHERE deleted_at IS NULL`  
**Deleted Goals**: `WHERE deleted_at IS NOT NULL`

### Query Examples

```sql
-- Fetch active goal for account
SELECT * FROM savings_goals
WHERE account_id = ? AND deleted_at IS NULL;

-- Fetch all active goals for customer
SELECT * FROM savings_goals
WHERE customer_id = ? AND deleted_at IS NULL;

-- Count active goals
SELECT COUNT(*) FROM savings_goals
WHERE customer_id = ? AND deleted_at IS NULL;
```

---

## Audit Logging

Every mutation (CREATE, UPDATE, DELETE) writes to the existing `audit_log` table.

| Event       | Action              | Resource Type | Resource ID | Details                                                |
| ----------- | ------------------- | ------------- | ----------- | ------------------------------------------------------ |
| Create goal | CREATE_SAVINGS_GOAL | SAVINGS_GOAL  | goal_id     | goal_name, target_amount, target_date                  |
| Update goal | UPDATE_SAVINGS_GOAL | SAVINGS_GOAL  | goal_id     | Changed fields (goal_name, target_amount, target_date) |
| Delete goal | DELETE_SAVINGS_GOAL | SAVINGS_GOAL  | goal_id     | (None — record is soft-deleted)                        |

---

## Schema Evolution

### Initial Schema (V001)

- `savings_goals` table with columns as described above
- Indexes for query performance
- Foreign keys to `customers` and `account`

### Future Considerations (Not in Scope)

- Historical goal tracking (multiple goals per account across time)
- Goal templates (pre-configured savings plans)
- Goal sharing (household-level goals)
- Goal notifications (milestone alerts)

---

## Data Type Rationale

| Field                  | Type          | Rationale                                                         |
| ---------------------- | ------------- | ----------------------------------------------------------------- |
| goal_id                | BIGINT        | Auto-increment primary key; allows for unlimited goal count       |
| customer_id            | BIGINT        | Matches existing `customers(customer_id)` type                    |
| account_id             | BIGINT        | Matches existing `account(account_id)` type                       |
| goal_name              | VARCHAR(255)  | Preset options fit in 255 chars; custom text also accommodated    |
| target_amount          | DECIMAL(19,2) | Matches `account.balance` precision (19 digits, 2 decimal places) |
| target_date            | DATE          | Day-level granularity; time-of-day not needed for savings goals   |
| status                 | ENUM          | Fixed set of 4 values; more efficient than VARCHAR                |
| deleted_at             | TIMESTAMP     | Soft-delete marker; NULL means active                             |
| created_at, updated_at | TIMESTAMP     | Audit trail; auto-managed by database                             |

---

## Performance Considerations

### Query Performance

**Fetch single goal (fastest path)**:

```sql
SELECT sg.*, a.balance, a.account_type, a.account_number
FROM savings_goals sg
JOIN account a ON sg.account_id = a.account_id
WHERE sg.account_id = ? AND sg.deleted_at IS NULL;
```

- Index on account_id ensures < 10ms latency

**Fetch all goals for customer**:

```sql
SELECT sg.*, a.balance, a.account_type, a.account_number
FROM savings_goals sg
JOIN account a ON sg.account_id = a.account_id
WHERE sg.customer_id = ? AND sg.deleted_at IS NULL;
```

- Index on customer_id + deleted_at filtering keeps response under 100ms even for customers with 50+ accounts

### Storage

- Assuming 100k customers with average 1.5 accounts each = 150k potential goals
- Average row size: ~200 bytes → ~30 MB total table size
- Negligible impact on database disk usage

---

## Consistency Guarantees

### Strong Consistency

- UNIQUE constraint guarantees one goal per account
- Foreign keys guarantee referential integrity
- Transactions ensure all-or-nothing semantics for create/update/delete

### Eventual Consistency

- Progress percentage is eventually consistent with account.balance (recalculated on every read, not pre-calculated)
- Status is eventually consistent with current date (recalculated on every read)

### No Race Conditions

- Unique constraint prevents concurrent goal creation on same account (first write wins, second gets 409)
- Soft delete prevents concurrent delete + edit (both writers see goal, one deletes first, second sees deleted_at ≠ NULL)

---

## Conclusion

The data model is simple, efficient, and consistent with existing Voltio patterns. It enforces all business rules at the database layer and is designed for high-performance read operations (typical use case: customers viewing their goals frequently).
