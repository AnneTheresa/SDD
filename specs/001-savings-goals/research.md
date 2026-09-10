# Research & Clarifications: Savings Goal Tracker

**Status**: ✅ Complete  
**Date**: June 5, 2026  
**Feature**: [plan.md](plan.md)

---

## Research Tasks

### 1. Goal Name Preset Options

**Question**: Which preset options should be offered in the "What are you saving for?" dropdown?

**Research**: Reviewed customer feedback, banking industry standards, and Voltio use cases.

**Decision**:

- Emergency Fund
- Travel
- Tuition
- Home
- Car
- Retirement
- Other (custom free-text)

**Rationale**: These options cover the most common savings goals for personal banking customers. "Other" enables flexibility for edge cases without expanding the preset list indefinitely. Custom text is stored directly in `goal_name` VARCHAR column.

**Alternatives Considered**:

- Generic labels (e.g., "Goal 1", "Goal 2") — rejected because they lack semantic meaning for customers
- Open free-text only — rejected because it creates UI friction and inconsistent data

---

### 2. Balance Source & Progress Calculation

**Question**: Should the current saved amount be user-entered or automatically derived from the account balance?

**Research**: Reviewed existing account balance model (DECIMAL 19,2) and transaction patterns. Examined whether separate tracking was needed.

**Decision**: Automatically derive from `account.balance` at read time. No user input or manual override allowed.

**Rationale**:

- Account balance is the single source of truth and is already kept in sync with all transactions
- Automatic calculation ensures progress is always accurate; no reconciliation needed
- Eliminates the risk of customer confusion or manual entry errors
- Aligns with Voltio principle: "No Silent Degradation" — balance changes are immediately visible

**Alternatives Considered**:

- Store separate `current_saved_amount` column in savings_goals — rejected because it requires synchronization on every transaction, creating race conditions and audit overhead
- Manual entry field — rejected because it defeats the purpose of automated tracking

---

### 3. Eligible Account Types

**Question**: Should only SAVINGS accounts be eligible for goals, or all account types?

**Research**: Examined Voltio account types (SAVINGS, CHEQUING, RRSP, GIC) and customer use cases.

**Decision**: All account types (SAVINGS, CHEQUING, RRSP, GIC, etc.) are eligible, provided `account.status = 'ACTIVE'` and `account.deleted_at IS NULL`.

**Rationale**:

- RRSP accounts may have savings goals for retirement
- GIC accounts may need goal tracking for investment milestones
- No technical reason to exclude any active account type
- Ownership and authorization already validated per account

**Alternatives Considered**:

- SAVINGS accounts only — rejected because it artificially limits goal utility
- ACTIVE status only (no deleted check) — rejected because soft-deleted accounts should not accept new goals

---

### 4. Ownership Validation Chain

**Question**: How is goal ownership determined and validated?

**Research**: Reviewed Voltio's authentication model (`users` table, JWT tokens) and account ownership model.

**Decision**: Ownership chain is `users.customer_id → customers.customer_id → account.customer_id`. All endpoints validate that the authenticated user's `customer_id` matches the `account.customer_id`.

**Rationale**:

- User JWT token contains `customer_id`
- Account ownership is determined by `account.customer_id`
- Goal ownership is transitively determined by account
- Multiple users can exist under one customer (household); all have equal access to that customer's goals

**Alternatives Considered**:

- Store goal owner separately — rejected because it's redundant with account ownership
- Per-user access control — rejected because Voltio's model is customer-scoped, not user-scoped

---

### 5. Overdue Goal Behavior

**Question**: What should happen to goals that have passed their target date without achieving the target?

**Research**: Examined customer expectations, UI UX patterns in similar apps (personal finance trackers).

**Decision**: Overdue goals remain fully visible on the Accounts page with an OVERDUE status badge. No automatic archiving or hiding.

**Rationale**:

- Goals may still be relevant even if overdue (e.g., customer wants to continue saving despite missing deadline)
- Archiving might hide important goals
- Customers can manually delete if they wish
- Status badge makes overdue state clear

**Alternatives Considered**:

- Auto-archive after 30 days — rejected because it could hide legitimate ongoing goals
- Hide overdue goals — rejected because it reduces transparency

---

### 6. Status Calculation Rules

**Question**: How is goal status determined? Should it be stored or calculated?

**Research**: Examined progress states and when each occurs.

**Decision**: Status is derived on every read from `account.balance` and `target_date`. It is not stored.

**Rationale**:

- Ensures status is always accurate without manual updates
- Avoids storage of redundant computed data
- Allows status to update automatically when account balance changes
- Calculation is lightweight: one comparison per read

**Status Logic**:
| Current Balance | Target Date | Status |
|---|---|---|
| 0 | ≥ today | NOT_STARTED |
| > 0, < target | ≥ today | IN_PROGRESS |
| ≥ target | any | ACHIEVED |
| < target | < today | OVERDUE |

**Alternatives Considered**:

- Store status in database — rejected because it requires updates on every account transaction
- Calculate on write only — rejected because status can change without goal mutations (e.g., time passes)

---

### 7. Theme Support Requirement

**Question**: Must the Savings Goal Tracker support both Classic and Neon themes?

**Research**: Reviewed Voltio Constitution Principle IX: "Theme Parity Requirement — any new user-facing feature must be implemented in both themes."

**Decision**: Yes. Both Classic and Neon themes are required before merge.

**Rationale**:

- Voltio is a multi-theme platform; theme support is non-negotiable
- Core product requirement, not an enhancement
- Progress bar, badges, and form elements must be visually consistent across themes

**Alternatives Considered**:

- Single-theme MVP with deferred theme 2 support — rejected because Voltio Constitution makes theme parity mandatory

---

### 8. Time Remaining Calculation

**Question**: How should "time remaining until deadline" be displayed?

**Research**: Reviewed user expectations and existing time formatting patterns in Voltio.

**Decision**:

- Calculated as `target_date - TODAY`
- Returned as non-negative integer (days)
- Once overdue (time remaining < 0), return 0 and flip status to OVERDUE
- UI can display "X days", "Today", or "Overdue by X days"

**Rationale**:

- Non-negative values prevent confusing negative time displays
- Status field indicates overdue state
- Days are human-readable and useful for customers

**Alternatives Considered**:

- Hours/minutes precision — rejected because daily granularity is sufficient for savings goals
- Display as countdown string in response — rejected because formatting is UI layer responsibility

---

### 9. No Money Movement Constraint

**Question**: Can this feature directly modify account balances or transactions?

**Research**: Reviewed specification requirement: "No money movement should occur from this feature. The tracker records and displays intent only."

**Decision**: The Savings Goal Tracker is read-only on `account.balance`, `bank_transaction`, and `standing_orders`. No writes to these tables are permitted.

**Rationale**:

- Goals are aspirational; they don't lock or move funds
- Account balance is updated only by legitimate transactions (deposits, withdrawals, transfers)
- Separating intent tracking from money movement maintains clear audit trail

**Alternatives Considered**:

- Automatically reserve funds — rejected because it adds complexity and changes customer experience
- Lock balance for goal amount — rejected because it blocks legitimate transactions

---

### 10. Soft Delete Pattern

**Question**: Should deleting a goal hard-delete the record or soft-delete?

**Research**: Reviewed Voltio's existing soft-delete pattern (used by `account` and `customers` tables).

**Decision**: Soft delete by setting `deleted_at = CURRENT_TIMESTAMP`. No hard deletes.

**Rationale**:

- Consistent with existing Voltio schema
- Preserves audit trail (audit_log remains intact)
- Allows goal recovery if needed
- Compliance with data retention policies

**Alternatives Considered**:

- Hard delete — rejected because it breaks audit compliance and is inconsistent with existing pattern

---

## Conclusion

All research tasks have been resolved. No outstanding clarifications remain. Specification is ready for Phase 1 design and Phase 2 task generation.
