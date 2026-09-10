# Feature Specification: Savings Goal Tracker

**Feature Branch**: `[to-be-set-by-git-hook]`

**Created**: June 5, 2026

**Status**: Draft

**Input**: User description: "Savings Goal Tracker - lives within Accounts section, allows customers to set and monitor one savings goal per account with progress tracking, edit/delete capabilities, and backend persistence."

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Create First Savings Goal (Priority: P1)

A customer accesses the Accounts section for the first time and wants to create a savings goal for one of their accounts. They are prompted to answer three questions about what they're saving for, how much, and by when, then see the goal displayed with a progress bar.

**Why this priority**: Creating a goal is the core feature and entry point for all users. Without this, no other feature delivers value.

**Independent Test**: Can be fully tested by opening the app, navigating to Accounts, clicking to create a goal, answering the three questions, and verifying the goal appears with a progress bar.

**Acceptance Scenarios**:

1. **Given** a customer has one or more accounts and no existing goal for a selected account, **When** they access the Accounts section, **Then** they see a prompt to create a goal for that account.
2. **Given** the customer is prompted to create a goal, **When** they select an account and answer the three questions (goal name, target amount, target date), **Then** the goal is created and displayed with initial progress data.
3. **Given** a goal has been created, **When** the goal is displayed, **Then** a progress bar shows 0% (assuming no funds saved yet) and time remaining until the deadline is visible.

---

### User Story 2 - View and Switch Between Account Goals (Priority: P1)

A customer with multiple accounts wants to view the goal attached to each account and switch between them to monitor progress on different goals.

**Why this priority**: Switching between accounts is fundamental to multi-account customers and essential to the feature's core functionality.

**Independent Test**: Can be fully tested by creating goals on multiple accounts and verifying the UI displays the correct goal when switching between accounts.

**Acceptance Scenarios**:

1. **Given** a customer has multiple accounts with goals, **When** they select a different account from the account switcher, **Then** the goal for that account is displayed (or a prompt to create one if no goal exists).
2. **Given** the customer is viewing a goal for one account, **When** they switch to another account with a different goal, **Then** the progress bar, deadline, and goal details reflect the new account's goal.
3. **Given** a customer switches to an account with no goal yet, **When** they view that account, **Then** they see a prompt to create a new goal.

---

### User Story 3 - Edit an Existing Goal (Priority: P2)

A customer wants to adjust their savings goal because their circumstances have changed. They can reopen the goal form, see their previous answers pre-populated, change only what they need, and the progress recalculates.

**Why this priority**: Edit capability is important for goal management but comes after creation and viewing, since users can delete and recreate if needed.

**Independent Test**: Can be fully tested by editing a goal, changing one or more fields, saving, and verifying the progress percentage and deadline update correctly.

**Acceptance Scenarios**:

1. **Given** a customer has an existing goal, **When** they click the edit button, **Then** the goal form opens with all three previous answers pre-populated.
2. **Given** the form is open with pre-populated values, **When** the customer changes one or more fields and saves, **Then** the goal is updated, validation is applied, and the progress bar recalculates.
3. **Given** a customer tries to edit with invalid values (e.g., negative target amount, past deadline date), **When** they attempt to save, **Then** an error message is shown and the goal is not updated.

---

### User Story 4 - Delete a Goal (Priority: P2)

A customer no longer wants to track a savings goal and wants to remove it from their account. A confirmation prompt appears before deletion, and once deleted, the account is free for a new goal.

**Why this priority**: Delete is a secondary interaction that provides safety and account management but is less frequent than creation and viewing.

**Independent Test**: Can be fully tested by deleting a goal, confirming the deletion, and verifying the account shows a prompt to create a new goal.

**Acceptance Scenarios**:

1. **Given** a customer has an existing goal, **When** they click the delete button, **Then** a confirmation prompt appears asking "Are you sure you want to delete this goal?"
2. **Given** the confirmation prompt is open, **When** the customer confirms the deletion, **Then** the goal is permanently removed and the account shows a create-goal prompt.
3. **Given** a customer confirms the deletion, **When** the deletion is complete, **Then** the customer can create a new goal for that account.

---

### User Story 5 - Progress Tracking and Display (Priority: P1)

The goal tracker displays a progress bar and time remaining so the customer can see how close they are to their target and know when the deadline is.

**Why this priority**: Progress tracking is the core value proposition of the feature. Without accurate tracking, the tool has no purpose.

**Independent Test**: Can be fully tested by creating a goal with known target and checking that the progress bar correctly reflects saved amount versus target.

**Acceptance Scenarios**:

1. **Given** a customer has a goal with target amount $5000 and the account shows $2000 saved, **When** the goal is displayed, **Then** the progress bar shows 40% completion.
2. **Given** a goal with a deadline 60 days away, **When** the goal is displayed, **Then** the time remaining shows "60 days" (or similar human-readable format).
3. **Given** a goal's target date is today, **When** the goal is displayed, **Then** the time remaining shows "0 days" or "Today" without showing negative values.
4. **Given** a goal's deadline has passed, **When** the goal is displayed, **Then** the time remaining shows in an overdue state (e.g., "Overdue by X days") or a warning color.

---

### Edge Cases

- What happens when a customer tries to create a goal for an account that already has one? (Should show option to edit or delete existing goal, or prevent goal creation until existing is removed.)
- How does the system handle when a goal's target date is in the past but the goal was not deleted? (Should display in overdue state.)
- What happens if the current saved amount on an account changes externally (e.g., due to a transaction)? (Progress bar should update automatically to reflect new balance.)
- What if a customer tries to set a target amount of $0 or negative? (Validation should reject and show error message.)
- What if a customer sets the target date to today or in the past? (Validation should reject or require confirmation, depending on UX decision.)

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST allow customers to create one goal per account through a three-question form (goal name/type, target amount, target date/duration).
- **FR-002**: System MUST display the created goal with a progress bar showing percentage of target amount saved, time remaining until deadline, and all goal details.
- **FR-003**: System MUST allow customers to switch between accounts and view the goal linked to each account.
- **FR-004**: System MUST allow customers to edit a goal by reopening the form pre-populated with previous answers and updating any field.
- **FR-005**: System MUST apply validation on goal save: target amount must be greater than 0, target date must be in the future (or today if acceptable per business decision), and any saved amount must be non-negative.
- **FR-006**: System MUST recalculate progress percentage and time remaining automatically after any goal edit.
- **FR-007**: System MUST show a confirmation prompt before permanently deleting a goal.
- **FR-008**: System MUST allow customers to create a new goal for an account after deleting the previous goal.
- **FR-009**: System MUST persist all goal data in the backend (goal name, target amount, target date, saved amount, created date, last updated date).
- **FR-010**: System MUST link each goal to the customer account it belongs to and ensure one goal per account.
- **FR-011**: System MUST NOT execute any money movement. The tracker records and displays intent only; no funds are transferred or locked as a result of goal operations.
- **FR-012**: System MUST return all goal fields (including question-answer pairs) when opening the edit form so the UI can pre-populate previous answers.

### Contract & Error Semantics Requirements _(mandatory for API or integration changes)_

- **CER-001**: A new backend endpoint or DTO for goal creation MUST define stable request/response contracts. Request contract: `{accountId, goalName, targetAmount, targetDate}`. Response contract MUST include goal ID, created timestamp, and all input fields.
- **CER-002**: Endpoints for goal retrieval, update, and deletion MUST define stable contracts and be tested for compatibility with frontend consumers before merge.
- **CER-003**: Backend error cases MUST include validation errors (invalid target amount, past deadline, duplicate goal per account), not-found errors (goal or account not found), and permission errors (customer does not own account/goal). Each MUST map to a stable error code.
- **CER-004**: Frontend MUST map backend error codes in `src/api/axiosClient.js` only. Ad-hoc error parsing in pages or components is prohibited.
- **CER-005**: Goal update operations MUST be idempotent and handle concurrent edits safely (e.g., via optimistic locking or version fields if needed).

### Security & Configuration Requirements _(mandatory for endpoint/proxy/auth changes)_

- **SCR-001**: All goal endpoints MUST enforce customer authentication and authorization. A customer MUST NOT be able to view, edit, or delete goals belonging to another customer.
- **SCR-002**: Account ownership MUST be verified before allowing goal creation, edit, or deletion on that account. A customer MUST NOT be able to create a goal for an account they do not own.
- **SCR-003**: Goal endpoints MUST be protected by JWT token validation in `SecurityConfig` and role-based access control if applicable.
- **SCR-004**: CORS configuration MUST remain consistent with existing Voltio settings; no wildcard origins for production.

### Key Entities _(include if feature involves data)_

- **SavingsGoal**: Represents a customer's savings goal for an account. Attributes: goalId (unique identifier), accountId (foreign key to account), customerId (to enforce authorization), goalName/type (text), targetAmount (decimal, > 0), targetDate (future date or today), createdDate (timestamp), lastUpdatedDate (timestamp), currentSavedAmount (decimal, >= 0), progressPercentage (calculated field, currentSavedAmount / targetAmount \* 100).
- **Account**: Existing entity linked to customer; each account can have zero or one SavingsGoal.
- **Customer**: Existing entity; owns one or more accounts and their associated goals.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Customers can create a savings goal in under 2 minutes (time from accessing Accounts to goal creation confirmation).
- **SC-002**: Progress bar accuracy: displayed progress percentage matches actual saved amount / target amount to within 0.1% after any account transaction.
- **SC-003**: Goal edit completion time: customers can edit an existing goal in under 1 minute.
- **SC-004**: 95% of goal creation attempts result in successful goal creation on the first try (no validation errors or need to retry).
- **SC-005**: Time remaining calculation is updated within 5 seconds of real-time (i.e., if a customer views a goal, the time remaining reflects current time within 5 seconds).
- **SC-006**: All goal data is persisted immediately upon creation, edit, or deletion; no data loss on browser refresh or app restart.
- **SC-007**: Zero unplanned data integrity issues: one goal per account constraint is enforced and not violated due to concurrency or race conditions.

## Assumptions

- **User Base**: Customers have stable internet connectivity and use modern browsers. Mobile support is assumed based on existing Voltio platform support for mobile UIs.
- **Saved Amount**: The "current saved amount" is derived from the account's existing balance at the time the goal is viewed, not a separate tracker. The feature displays intent only and does not perform money movements.
- **Goal Deadline Handling**: Goals with deadlines in the past are allowed to remain (user can delete or edit) and will display in an overdue state. The feature does not auto-delete or auto-archive overdue goals.
- **Validation Rules**: Target amount validation uses only `> 0` check. Currency precision is assumed to follow existing account transaction precision (likely two decimal places). Target date must be today or in the future; past dates are rejected on initial creation but may exist due to time passing after goal creation.
- **Backend Stack**: Existing authentication and authorization mechanisms are reused. New goal endpoints follow existing Voltio REST conventions. Database schema changes are compatible with existing schema (likely relational database with customer/account/goal relationship).
- **Theme Support**: Feature MUST support both Classic and Neon themes as per Voltio Constitution Principle IX. Theme-specific styling or state visualization (e.g., colors, fonts, icons) will be applied consistently.
- **No Dependency on External Services**: Savings goal tracking does not require third-party integrations or external financial planning services.
- **Scope**: Multi-account goal management is in scope (customers can have different goals per account). Cross-account goal grouping or global goal management is out of scope for v1.
