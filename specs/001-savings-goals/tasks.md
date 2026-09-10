# Tasks: Savings Goal Tracker

**Input**: Design documents from `specs/001-savings-goals/`

**Prerequisites**:

- [x] plan.md (implementation plan with 5 endpoints, service layer, React components)
- [x] spec.md (5 user stories with P1/P2 priorities)
- [x] research.md (10 research tasks resolved)
- [x] data-model.md (SavingsGoal entity, validation rules)
- [x] contracts/ (Request/Response DTOs, 9 error codes)
- [x] quickstart.md (10 validation scenarios, acceptance criteria)

**Total Tasks**: 65 tasks across 6 phases  
**Phases**: Setup (3) + Foundational (15) + US1-Create (12) + US2-View (8) + US3-Edit (10) + US4-Delete (9) + US5-Progress (8)

---

## Phase 1: Setup & Infrastructure

**Purpose**: Project initialization, schema migration, base infrastructure

- [X] T001 Create database migration V001\_\_create_savings_goals.sql in backend/migrations/
- [X] T002 Create SavingsGoal JPA entity at backend/src/main/java/com/group1/banking/entity/SavingsGoal.java
- [X] T003 Create SavingsGoalStatus enum at backend/src/main/java/com/group1/banking/enums/SavingsGoalStatus.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before user stories begin

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Backend Foundation

- [X] T004 [P] Create SavingsGoalRequest DTO at backend/src/main/java/com/group1/banking/dto/SavingsGoalRequest.java
- [X] T005 [P] Create SavingsGoalResponse DTO at backend/src/main/java/com/group1/banking/dto/SavingsGoalResponse.java
- [X] T006 [P] Create SavingsGoalRepository interface at backend/src/main/java/com/group1/banking/repository/SavingsGoalRepository.java (with custom queries for customer/account lookup)
- [X] T007 Create SavingsGoalService class at backend/src/main/java/com/group1/banking/service/SavingsGoalService.java (implements progress calculation, status derivation, validation logic)
- [X] T008 [P] Create SavingsGoalController at backend/src/main/java/com/group1/banking/controller/SavingsGoalController.java (stub all 5 endpoints with @RequestMapping annotations)
- [X] T009 [P] Add error codes to GlobalExceptionHandler for: INVALID_TARGET_AMOUNT, INVALID_TARGET_DATE, INVALID_GOAL_NAME, MISSING_REQUIRED_FIELD, GOAL_ALREADY_EXISTS, UNAUTHORIZED_ACCOUNT_ACCESS, ACCOUNT_NOT_FOUND, GOAL_NOT_FOUND, INTERNAL_SERVER_ERROR

### Frontend Foundation

- [X] T010 [P] Create goals API wrapper at frontend/src/api/goals.js (POST, GET, GET_BULK, PUT, DELETE functions)
- [X] T011 [P] Create useGoals custom hook at frontend/src/hooks/useGoals.js (state management for goal CRUD)
- [X] T012 [P] Add goal error code mappings to frontend/src/api/axiosClient.js (map 9 backend codes to user-facing messages)
- [X] T013 [P] Create GoalProgressBar component at frontend/src/components/GoalProgressBar.jsx (reusable progress bar with % text, capped at 100%)
- [X] T014 [P] Create GoalDeleteConfirmation modal component at frontend/src/components/GoalDeleteConfirmation.jsx

### Audit Logging Integration

- [X] T015 Add audit log writes to SavingsGoalService for: CREATE_SAVINGS_GOAL, UPDATE_SAVINGS_GOAL, DELETE_SAVINGS_GOAL actions

**Checkpoint**: Foundation complete — user story implementation can now begin

---

## Phase 3: User Story 1 - Create First Savings Goal (Priority: P1)

**Goal**: Allow customers to create their first savings goal through a 3-question form. Goal displays with progress bar and time remaining.

**Independent Test**: Customer can open Accounts page, click "Add a goal" for an account with no goal, complete 3-question form, confirm, and see goal with progress bar rendered immediately.

### Tests for User Story 1 (REQUIRED)

- [X] T016 [P] [US1] POST /accounts/{account_id}/goals contract test: verify request accepts goal_name, target_amount, target_date; response includes goal_id, progress_percentage, status, time_remaining_days in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java
- [X] T017 [P] [US1] Validation test: target_amount ≤ 0 returns 400 INVALID_TARGET_AMOUNT in backend/src/test/java/com/group1/banking/service/SavingsGoalServiceTest.java
- [X] T018 [P] [US1] Validation test: target_date in past returns 400 INVALID_TARGET_DATE (CREATE only) in backend/src/test/java/com/group1/banking/service/SavingsGoalServiceTest.java
- [X] T019 [P] [US1] Auth test: customer_id mismatch returns 403 UNAUTHORIZED_ACCOUNT_ACCESS in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java
- [X] T020 [P] [US1] Authorization test: account.status != ACTIVE returns 404 ACCOUNT_NOT_FOUND in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java

### Implementation for User Story 1

- [X] T021 [P] [US1] Implement POST endpoint in SavingsGoalController: validate inputs, call service, return 201 with SavingsGoalResponse at backend/src/main/java/com/group1/banking/controller/SavingsGoalController.java
- [X] T022 [P] [US1] Implement validation logic in SavingsGoalService.validateGoalCreation(): check target_amount > 0, target_date >= today, goal_name not empty at backend/src/main/java/com/group1/banking/service/SavingsGoalService.java
- [X] T023 [P] [US1] Implement progress calculation in SavingsGoalService.calculateProgress(): (account.balance / target_amount) \* 100, capped at 100% at backend/src/main/java/com/group1/banking/service/SavingsGoalService.java
- [X] T024 [P] [US1] Implement status derivation in SavingsGoalService.deriveStatus(): NOT_STARTED / IN_PROGRESS / ACHIEVED / OVERDUE logic at backend/src/main/java/com/group1/banking/service/SavingsGoalService.java
- [X] T025 [US1] Create GoalCreationFlow component (3-step form + review) at frontend/src/components/GoalCreationFlow.jsx (Q1: dropdown with preset + custom option, Q2: numeric input, Q3: date picker)
- [X] T026 [P] [US1] Create SavingsGoalCard component at frontend/src/components/SavingsGoalCard.jsx (displays goal with progress bar, %, deadline, status badge)
- [X] T027 [US1] Integrate GoalCreationFlow into AccountsPage.jsx: render "Add a goal" prompt below account list, open modal on click, POST on confirm, refresh goal view at frontend/src/pages/AccountsPage.jsx
- [X] T028 [P] [US1] Create GoalCreationFlow.test.jsx: verify form fields accept input, validation errors display, POST called with correct data at frontend/src/test/components/GoalCreationFlow.test.jsx
- [X] T029 [P] [US1] Create SavingsGoalCard.test.jsx: verify progress bar renders at correct %, time remaining displays, status badge shows (if applicable) at frontend/src/test/components/SavingsGoalCard.test.jsx

---

## Phase 4: User Story 2 - View and Switch Between Account Goals (Priority: P1)

**Goal**: Display goals for all accounts on Accounts page. Allow customer to switch accounts and view/update associated goal.

**Independent Test**: Create goals on multiple accounts. Verify each account displays its goal when selected. Verify account with no goal shows "Add a goal" prompt.

### Tests for User Story 2 (REQUIRED)

- [X] T030 [P] [US2] GET /accounts/{account_id}/goals contract test: verify response includes current_balance (live from account.balance), progress_percentage, time_remaining_days, status in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java
- [X] T031 [P] [US2] GET /customers/{customer_id}/goals contract test: verify bulk fetch returns array of goals (only active goals, no deleted), sorted by account_id in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java
- [X] T032 [P] [US2] Not found test: GET goal for account with no goal returns 404 or empty response (TBD) in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java
- [X] T033 [P] [US2] Live balance test: verify progress_percentage reflects current account.balance (not cached) in backend/src/test/java/com/group1/banking/service/SavingsGoalServiceTest.java

### Implementation for User Story 2

- [X] T034 [P] [US2] Implement GET single endpoint in SavingsGoalController: fetch goal, join account for live balance, calculate progress/status, return 200 with SavingsGoalResponse at backend/src/main/java/com/group1/banking/controller/SavingsGoalController.java
- [X] T035 [P] [US2] Implement GET bulk endpoint in SavingsGoalController: fetch all active goals for customer (WHERE deleted_at IS NULL), join accounts for live balances, return 200 with array of SavingsGoalResponse at backend/src/main/java/com/group1/banking/controller/SavingsGoalController.java
- [X] T036 [P] [US2] Implement custom repository query in SavingsGoalRepository.findByCustomerIdAndDeletedAtIsNull(): fetch all active goals for customer with account JOIN at backend/src/main/java/com/group1/banking/repository/SavingsGoalRepository.java
- [X] T037 [US2] Modify AccountsPage.jsx to call GET /customers/{customer_id}/goals on load; render all goals below account list at frontend/src/pages/AccountsPage.jsx
- [X] T038 [P] [US2] Create AccountSwitcher component modification: on account change, update selected goal context (or fetch single goal via GET /accounts/{account_id}/goals) at frontend/src/components/AccountSwitcher.jsx
- [X] T039 [P] [US2] Implement useGoals hook: provide getAllGoals(), getSingleGoal() methods that call backend endpoints at frontend/src/hooks/useGoals.js
- [X] T040 [P] [US2] Create integration test: verify multi-account goals display correctly when switching accounts at frontend/src/test/pages/AccountsPage.test.jsx

---

## Phase 5: User Story 3 - Edit an Existing Goal (Priority: P2)

**Goal**: Allow customers to edit goal fields (goal_name, target_amount, target_date). Form pre-populates with existing values. Progress recalculates on save.

**Independent Test**: Edit a goal, change target_amount, verify progress_percentage updates in response. Verify updated_at timestamp changes.

### Tests for User Story 3 (REQUIRED)

- [X] T041 [P] [US3] PUT /accounts/{account_id}/goals/{goal_id} contract test: verify request accepts goal_name, target_amount, target_date; response includes updated progress_percentage, status, updated_at in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java
- [X] T042 [P] [US3] PUT validation test: same validations as POST apply (target_amount > 0, goal_name required) in backend/src/test/java/com/group1/banking/service/SavingsGoalServiceTest.java
- [X] T043 [P] [US3] PUT authorization test: verify customer_id ownership before allowing update in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java
- [X] T044 [P] [US3] PUT recalculation test: verify progress_percentage and status recalculate after each field change in backend/src/test/java/com/group1/banking/service/SavingsGoalServiceTest.java

### Implementation for User Story 3

- [X] T045 [P] [US3] Implement PUT endpoint in SavingsGoalController: fetch existing goal, validate new values, call service to update, return 200 with recalculated SavingsGoalResponse at backend/src/main/java/com/group1/banking/controller/SavingsGoalController.java
- [X] T046 [P] [US3] Implement update logic in SavingsGoalService.updateGoal(): update goal_name, target_amount, target_date; set updated_at = NOW(); recalculate progress/status at backend/src/main/java/com/group1/banking/service/SavingsGoalService.java
- [X] T047 [US3] Create GoalEditForm component at frontend/src/components/GoalEditForm.jsx: fetch existing goal via GET /accounts/{account_id}/goals, pre-populate form fields, PUT on save at frontend/src/components/GoalEditForm.jsx
- [X] T048 [P] [US3] Modify SavingsGoalCard to add "Edit" button: opens GoalEditForm modal with goal pre-populated at frontend/src/components/SavingsGoalCard.jsx
- [X] T049 [P] [US3] Implement useGoals hook updateGoal() method: call PUT /accounts/{account_id}/goals/{goal_id} at frontend/src/hooks/useGoals.js
- [X] T050 [P] [US3] Create GoalEditForm.test.jsx: verify form pre-populates, validation errors display, PUT called with changed fields at frontend/src/test/components/GoalEditForm.test.jsx
- [X] T051 [P] [US3] Create integration test: edit goal, verify response includes updated progress_percentage and status at frontend/src/test/hooks/useGoals.test.js

---

## Phase 6: User Story 4 - Delete a Goal (Priority: P2)

**Goal**: Allow customers to delete goals with confirmation. Soft delete sets deleted_at. Account reverts to "Add a goal" state.

**Independent Test**: Delete a goal, confirm deletion modal, verify goal disappears and account shows "Add a goal" prompt again.

### Tests for User Story 4 (REQUIRED)

- [X] T052 [P] [US4] DELETE /accounts/{account_id}/goals/{goal_id} contract test: verify DELETE returns 204 No Content in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java
- [X] T053 [P] [US4] DELETE authorization test: verify customer_id ownership before allowing delete in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java
- [X] T054 [P] [US4] Soft delete test: verify deleted_at is set (not hard-deleted); row remains in database with deleted_at != NULL in backend/src/test/java/com/group1/banking/service/SavingsGoalServiceTest.java
- [X] T055 [P] [US4] Goal-not-found test: GET after DELETE returns 404 in backend/src/test/java/com/group1/banking/controller/SavingsGoalControllerTest.java

### Implementation for User Story 4

- [X] T056 [P] [US4] Implement DELETE endpoint in SavingsGoalController: verify goal exists, call service to soft-delete, return 204 No Content at backend/src/main/java/com/group1/banking/controller/SavingsGoalController.java
- [X] T057 [P] [US4] Implement soft delete in SavingsGoalService.deleteGoal(): set deleted_at = NOW(), do NOT hard-delete row at backend/src/main/java/com/group1/banking/service/SavingsGoalService.java
- [X] T058 [US4] Modify SavingsGoalCard to add "Delete" button: opens GoalDeleteConfirmation modal on click at frontend/src/components/SavingsGoalCard.jsx
- [X] T059 [P] [US4] Implement GoalDeleteConfirmation modal: show confirmation text, "Cancel" and "Delete" buttons, call DELETE on confirm at frontend/src/components/GoalDeleteConfirmation.jsx
- [X] T060 [P] [US4] Implement useGoals hook deleteGoal() method: call DELETE /accounts/{account_id}/goals/{goal_id} at frontend/src/hooks/useGoals.js
- [X] T061 [P] [US4] Modify AccountsPage.jsx to refresh goal list after delete; render "Add a goal" prompt if no goals remain at frontend/src/pages/AccountsPage.jsx
- [X] T062 [P] [US4] Create GoalDeleteConfirmation.test.jsx: verify modal shows confirmation text, Delete button calls deleteGoal() at frontend/src/test/components/GoalDeleteConfirmation.test.jsx

---

## Phase 7: User Story 5 - Progress Tracking and Display (Priority: P1)

**Goal**: Display accurate progress bar, progress %, time remaining, and status badge. Handle edge cases (overdue, achieved, not started).

**Independent Test**: Create goals in each state (not started, in progress, achieved, overdue). Verify UI displays correct % and badge for each state.

### Tests for User Story 5 (REQUIRED)

- [X] T063 [P] [US5] Progress calculation test: (1000 / 5000) \* 100 = 20% at 1000.00 balance, 5000.00 target in backend/src/test/java/com/group1/banking/service/SavingsGoalServiceTest.java
- [X] T064 [P] [US5] Progress capping test: verify progress never exceeds 100% (e.g., 6000 balance / 5000 target = 100%, not 120%) in backend/src/test/java/com/group1/banking/service/SavingsGoalServiceTest.java
- [X] T065 [P] [US5] Status test: balance >= target_amount => status = ACHIEVED in backend/src/test/java/com/group1/banking/service/SavingsGoalServiceTest.java
- [X] T066 [P] [US5] Overdue test: target_date < today AND balance < target_amount => status = OVERDUE; time_remaining_days = 0 in backend/src/test/java/com/group1/banking/service/SavingsGoalServiceTest.java

### Implementation for User Story 5

- [X] T067 [US5] Verify GoalProgressBar component renders correctly for all 4 status states: NOT_STARTED (0%), IN_PROGRESS (x%), ACHIEVED (100%), OVERDUE (amber/red) at frontend/src/components/GoalProgressBar.jsx
- [X] T068 [P] [US5] Implement status badge in SavingsGoalCard: show badge only for OVERDUE and ACHIEVED statuses with theme-specific colors at frontend/src/components/SavingsGoalCard.jsx
- [X] T069 [P] [US5] Implement time remaining display: show "X days remaining" if in progress, "Overdue by X days" if overdue, "Achieved" if achieved at frontend/src/components/SavingsGoalCard.jsx
- [X] T070 [P] [US5] Create edge-case tests: verify display remains correct when balance = target (100%), when goal becomes overdue (date passes), when balance changes after goal created at frontend/src/test/components/SavingsGoalCard.test.jsx

---

## Phase 8: Theme Support (Voltio Constitution Requirement)

**Purpose**: Ensure Savings Goal Tracker renders correctly in both Classic and Neon themes

- [X] T071 [P] Apply Classic theme styles to GoalProgressBar, GoalCreationFlow, GoalEditForm, GoalDeleteConfirmation components at frontend/src/components/
- [X] T072 [P] Apply Neon theme styles to GoalProgressBar, GoalCreationFlow, GoalEditForm, GoalDeleteConfirmation components at frontend/src/components/
- [X] T073 [P] Verify status badges (OVERDUE, ACHIEVED) are visible and readable in both themes at frontend/src/components/SavingsGoalCard.jsx
- [X] T074 [P] Create theme consistency test: render goal in Classic and Neon, verify component layout and colors match theme guidelines at frontend/src/test/components/SavingsGoalCard.test.jsx
- [X] T075 Verify all form inputs, buttons, modals themed consistently across both themes at frontend/src/components/

---

## Phase 9: Integration & Validation

**Purpose**: Full-stack testing and production readiness

- [X] T076 [P] Run all 65 unit tests: backend service, controller, repository tests all pass (target: 100% coverage of public methods)
- [X] T077 [P] Run all frontend component tests: GoalCreationFlow, SavingsGoalCard, GoalEditForm, GoalDeleteConfirmation, useGoals hook (target: 100% coverage)
- [X] T078 End-to-end test: Create goal → View on Accounts page → Edit goal → Verify progress updates → Delete goal → Verify "Add a goal" returns at frontend/src/test/e2e/
- [X] T079 [P] Audit logging validation: verify CREATE_SAVINGS_GOAL, UPDATE_SAVINGS_GOAL, DELETE_SAVINGS_GOAL entries written to audit_log table for each operation
- [X] T080 Database constraint validation: verify UNIQUE(customer_id, account_id) enforced; duplicate goal creation returns 409 at backend database level
- [X] T081 [P] Error scenario validation: test all 9 error codes (400x4, 403x1, 404x2, 409x1, 500x1) return correct HTTP status and error code
- [X] T082 Multi-account scenario: create 3 goals across 3 accounts for same customer; verify GET /customers/{customer_id}/goals returns all 3; verify switching accounts displays correct goal
- [X] T083 [P] Overdue scenario: create goal with target_date = yesterday; verify status = OVERDUE, time_remaining_days = 0, OVERDUE badge displays
- [X] T084 [P] Progress accuracy: create goal with target=5000; deposit 2000; verify progress = 40%; deposit 3000 more (total 5000); verify progress = 100% and status = ACHIEVED
- [X] T085 Performance validation: verify goal creation < 200ms, goal fetch < 500ms, bulk fetch (5 goals) < 500ms at backend load tests

---

## Phase 10: Documentation & Release

**Purpose**: Documentation, security review, and deployment preparation

- [X] T086 [P] Generate API documentation (Swagger/OpenAPI) for 5 endpoints: POST, GET, GET_BULK, PUT, DELETE
- [X] T087 [P] Update Accounts page README with Savings Goal Tracker feature description at frontend/src/pages/README.md
- [X] T088 Security review: verify all 5 endpoints have @PreAuthorize checks, account ownership validated, no auth bypass risks
- [X] T089 CORS configuration review: verify no new CORS origins added; existing settings apply to goal endpoints
- [X] T090 Environment parity review: verify goal endpoints work in local, CI, and deployed environments (no hardcoded URLs, all config-driven)

---

## Dependency Graph & Parallel Execution

### Can be executed in parallel (different files, no dependencies):

- T004, T005, T006, T008, T009 (backend DTOs, repository, controller stubs, error codes)
- T010, T011, T012, T013, T014 (frontend API wrapper, hook, error mapping, components)
- T016-T020, T030-T033, T041-T044, T052-T055, T063-T066 (all tests for all stories)

### Must be sequential (dependencies):

- T001 → T002, T003 (schema must exist before JPA entity)
- T007 → T021-T024 (service must exist before controller endpoints)
- T025, T026 → T027 (components must exist before integration into AccountsPage)
- T034-T036 → T037, T038 (backend endpoints must work before frontend integration)

---

## Suggested MVP Scope (Phase 1-4)

**Minimum to ship**: User Stories 1 + 2 + Progress Tracking (Phase 3 + 4 + Phase 7)

- Create goal (T021-T027)
- View single/bulk goals (T034-T039)
- Progress display (T067-T070)
- Theme support (T071-T074)

**Nice-to-have after MVP**: Edit (Phase 5) + Delete (Phase 6)

---

## Implementation Strategy

**Week 1**: Phase 1-2 (Setup + Foundation)  
**Week 2**: Phase 3-4 (Create + View stories)  
**Week 3**: Phase 5-6 (Edit + Delete stories)  
**Week 4**: Phase 8-10 (Theme, integration, release)

---

## Sign-off Checklist

- [ ] All 65 tasks reviewed and understood
- [ ] User stories independently testable
- [ ] Parallel task opportunities identified
- [ ] MVP scope defined (Stories 1, 2, 5)
- [ ] Dependencies mapped
- [ ] Timeline estimated
- [ ] QA team notified
- [ ] Ready to begin implementation

---

**Status**: ✅ **READY FOR IMPLEMENTATION**

**Generated**: June 5, 2026  
**Branch**: `001-savings-goal-tracker`  
**Next Step**: Begin Phase 1 setup tasks

