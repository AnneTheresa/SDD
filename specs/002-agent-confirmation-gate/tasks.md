# Tasks: Agent Confirmation Gate for Financial Actions

**Input**: Design documents from `/specs/002-agent-confirmation-gate/`

**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: Tests are required because this feature changes contracts, money-movement safety controls, auth checks, and error semantics.

**Organization**: Tasks are grouped by user story to support independent implementation and verification.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Validate workspace conventions and prepare feature scaffolding.

- [ ] T001 Confirm backend CRLF line endings for all new files under backend/src/main/java/com/group1/banking/
- [ ] T002 Create feature test placeholders for new backend units in backend/src/test/java/com/group1/banking/
- [ ] T003 [P] Create frontend test placeholder for confirmation-card coverage in src/test/components/ChatWidget.test.jsx
- [ ] T004 [P] Validate existing transfer execution contract reference path in backend/src/main/java/com/group1/banking/service/impl/MonetaryOperationService.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core reusable confirmation-gate infrastructure required before user stories.

**CRITICAL**: No story-phase work starts until this phase is complete.

- [ ] T005 Create PendingAgentAction status enum in backend/src/main/java/com/group1/banking/entity/PendingAgentActionStatus.java
- [ ] T006 [P] Create PendingAgentAction type enum in backend/src/main/java/com/group1/banking/entity/PendingAgentActionType.java
- [ ] T007 Create PendingAgentAction entity in backend/src/main/java/com/group1/banking/entity/PendingAgentActionEntity.java
- [ ] T008 [P] Create PendingAgentAction repository in backend/src/main/java/com/group1/banking/repository/PendingAgentActionRepository.java
- [ ] T009 Create GoneException (410) in backend/src/main/java/com/group1/banking/exception/GoneException.java
- [ ] T010 [P] Register GoneException handler in backend/src/main/java/com/group1/banking/exception/GlobalExceptionHandler.java
- [ ] T011 Create reusable ConfirmationGateService in backend/src/main/java/com/group1/banking/service/ConfirmationGateService.java
- [ ] T012 [P] Create pending confirmation DTO in backend/src/main/java/com/group1/banking/dto/chat/PendingConfirmationView.java
- [ ] T013 [P] Create per-turn pending action tracker in backend/src/main/java/com/group1/banking/service/impl/PendingActionTracker.java
- [ ] T014 Extend chat response contract with pending_confirmation in backend/src/main/java/com/group1/banking/dto/chat/ChatQueryResponse.java
- [ ] T015 [P] Add foundational repository and exception tests in backend/src/test/java/com/group1/banking/repository/PendingAgentActionRepositoryTest.java and backend/src/test/java/com/group1/banking/exception/GlobalExceptionHandlerTest.java
- [ ] T016 Add foundational gate-service tests for propose/confirm token lifecycle in backend/src/test/java/com/group1/banking/service/ConfirmationGateServiceTest.java

**Checkpoint**: Confirmation-gate foundation is ready for story-level behavior.

---

## Phase 3: User Story 1 - Agent Proposes Transfer, Never Executes in Tool Loop (Priority: P1) MVP

**Goal**: The chat agent can propose a transfer and return structured pending confirmation without moving money.

**Independent Test**: Call savings-insights chat endpoint with transfer intent and verify pending_confirmation is present while balances remain unchanged.

### Tests for User Story 1

- [ ] T017 [P] [US1] Add tool-level validation/proposal tests in backend/src/test/java/com/group1/banking/service/impl/TransferChatToolTest.java
- [ ] T018 [P] [US1] Add chat-service response contract test for pending_confirmation attachment in backend/src/test/java/com/group1/banking/service/impl/SavingsInsightChatServiceTest.java
- [ ] T019 [P] [US1] Add business-failure test for non-owned account proposal rejection in backend/src/test/java/com/group1/banking/service/impl/TransferChatToolTest.java

### Implementation for User Story 1

- [ ] T020 [US1] Implement propose-only transfer tool in backend/src/main/java/com/group1/banking/service/impl/TransferChatTool.java
- [ ] T021 [US1] Wire transfer tool and system prompt constraints into AI config in backend/src/main/java/com/group1/banking/config/ChatbotAiConfig.java
- [ ] T022 [US1] Inject pending tracker and actorRole tool context in backend/src/main/java/com/group1/banking/service/impl/SavingsInsightChatService.java
- [ ] T023 [P] [US1] Ensure pending confirmation is drained-once per turn in backend/src/main/java/com/group1/banking/service/impl/PendingActionTracker.java
- [ ] T024 [US1] Validate no direct money movement occurs in tool path by routing all transfer intents through ConfirmationGateService.propose in backend/src/main/java/com/group1/banking/service/impl/TransferChatTool.java

---

## Phase 4: User Story 2 - Customer Explicitly Confirms Proposed Transfer (Priority: P1)

**Goal**: Confirm endpoint executes transfer only for valid token and customer, using token as idempotency key.

**Independent Test**: Propose then confirm a transfer using token, verify transfer result and terminal token behavior.

### Tests for User Story 2

- [ ] T025 [P] [US2] Add controller success-path test for POST /api/chat/confirmations/{token} in backend/src/test/java/com/group1/banking/controller/AgentActionConfirmationControllerTest.java
- [ ] T026 [P] [US2] Add duplicate-confirm conflict test (second call 409, no double execution) in backend/src/test/java/com/group1/banking/controller/AgentActionConfirmationControllerTest.java
- [ ] T027 [P] [US2] Add idempotency-key passthrough assertion to transfer call in backend/src/test/java/com/group1/banking/controller/AgentActionConfirmationControllerTest.java

### Implementation for User Story 2

- [ ] T028 [US2] Implement confirmation endpoint controller in backend/src/main/java/com/group1/banking/controller/AgentActionConfirmationController.java
- [ ] T029 [US2] Deserialize stored transfer parameters and execute MonetaryOperationService.transfer with token idempotency key in backend/src/main/java/com/group1/banking/controller/AgentActionConfirmationController.java
- [ ] T030 [US2] Enforce confirm-and-consume flow before execution in backend/src/main/java/com/group1/banking/service/ConfirmationGateService.java
- [ ] T031 [P] [US2] Add frontend chat API method for confirmations in src/api/chat.js
- [ ] T032 [P] [US2] Add useConfirmAgentAction mutation hook in src/hooks/useSavingsChat.js
- [ ] T033 [US2] Render and wire Confirm action card behavior in src/components/ChatWidget.jsx
- [ ] T034 [US2] Add confirmation-card interaction test coverage in src/test/components/ChatWidget.test.jsx

---

## Phase 5: User Story 5 - Audit Every Proposal and Confirmation Decision (Priority: P1)

**Goal**: Persist auditable trail for proposal creation and confirmation resolution without blocking core flow on audit failure.

**Independent Test**: Propose and confirm a transfer, then verify distinct AGENT_ACTION_PROPOSED and AGENT_ACTION_CONFIRMED or AGENT_ACTION_DENIED events.

### Tests for User Story 5

- [ ] T035 [P] [US5] Add audit-on-proposal test in backend/src/test/java/com/group1/banking/service/ConfirmationGateServiceTest.java
- [ ] T036 [P] [US5] Add audit-on-confirm-deny outcomes test in backend/src/test/java/com/group1/banking/service/ConfirmationGateServiceTest.java
- [ ] T037 [P] [US5] Add non-blocking audit-failure resilience test in backend/src/test/java/com/group1/banking/service/ConfirmationGateServiceTest.java

### Implementation for User Story 5

- [ ] T038 [US5] Emit AGENT_ACTION_PROPOSED audit event from propose flow in backend/src/main/java/com/group1/banking/service/ConfirmationGateService.java
- [ ] T039 [US5] Emit AGENT_ACTION_CONFIRMED and AGENT_ACTION_DENIED with token resource id in backend/src/main/java/com/group1/banking/service/ConfirmationGateService.java
- [ ] T040 [US5] Ensure audit failures are logged but do not fail proposal or confirmation execution in backend/src/main/java/com/group1/banking/service/ConfirmationGateService.java

---

## Phase 6: User Story 3 - Unconfirmed Proposals Expire and Never Execute (Priority: P2)

**Goal**: Expired proposals are rejected with 410 and marked EXPIRED, with no transfer execution.

**Independent Test**: Confirm an expired token and verify 410 response, EXPIRED status, and unchanged balances.

### Tests for User Story 3

- [ ] T041 [P] [US3] Add expired-token rejection test for confirm flow in backend/src/test/java/com/group1/banking/service/ConfirmationGateServiceTest.java
- [ ] T042 [P] [US3] Add controller-level HTTP 410 mapping test in backend/src/test/java/com/group1/banking/controller/AgentActionConfirmationControllerTest.java

### Implementation for User Story 3

- [ ] T043 [US3] Implement expiry transition from PENDING to EXPIRED in confirm flow in backend/src/main/java/com/group1/banking/service/ConfirmationGateService.java
- [ ] T044 [US3] Return stable CONFIRMATION_EXPIRED semantics through exception handler path in backend/src/main/java/com/group1/banking/exception/GlobalExceptionHandler.java
- [ ] T045 [US3] Ensure unrelated chat turns do not mutate existing pending proposal state in backend/src/main/java/com/group1/banking/service/impl/SavingsInsightChatService.java

---

## Phase 7: User Story 4 - Reject Invalid, Missing, or Mismatched Tokens Without Leakage (Priority: P2)

**Goal**: Not-owned and missing tokens are indistinguishable (404), terminal tokens are rejected (409), and no transfer executes.

**Independent Test**: Confirm as another customer and with invalid token and verify 404 behavior plus no execution.

### Tests for User Story 4

- [ ] T046 [P] [US4] Add not-owned token test returning 404 in backend/src/test/java/com/group1/banking/service/ConfirmationGateServiceTest.java
- [ ] T047 [P] [US4] Add missing token test returning 404 in backend/src/test/java/com/group1/banking/controller/AgentActionConfirmationControllerTest.java
- [ ] T048 [P] [US4] Add already-resolved token conflict test returning 409 in backend/src/test/java/com/group1/banking/service/ConfirmationGateServiceTest.java

### Implementation for User Story 4

- [ ] T049 [US4] Filter token lookup by customer ownership before execution in backend/src/main/java/com/group1/banking/service/ConfirmationGateService.java
- [ ] T050 [US4] Return not found semantics for both missing and not-owned tokens in backend/src/main/java/com/group1/banking/service/ConfirmationGateService.java
- [ ] T051 [US4] Enforce terminal-status conflict behavior for EXECUTED and EXPIRED tokens in backend/src/main/java/com/group1/banking/service/ConfirmationGateService.java

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Finalize compatibility, frontend error mapping, and regression confidence.

- [ ] T052 [P] Add centralized frontend error mapping for confirmation codes in src/api/axiosClient.js
- [ ] T053 [P] Verify ChatWidget does not parse ad-hoc backend errors and uses mapped errors only in src/components/ChatWidget.jsx
- [ ] T054 [P] Validate classic and neon theme rendering for confirmation card states in src/components/ChatWidget.jsx
- [ ] T055 [P] Run targeted backend tests for new confirmation flow classes in backend/src/test/java/com/group1/banking/
- [ ] T056 [P] Run targeted frontend tests for chat confirmation UI in src/test/components/ChatWidget.test.jsx
- [ ] T057 Run full backend test suite for regression check in backend/src/test/java/com/group1/banking/
- [ ] T058 Run full frontend test suite for regression check in src/test/

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1): no dependencies.
- Foundational (Phase 2): depends on Setup completion and blocks all user stories.
- User Story phases (Phase 3-7): all depend on Foundational completion.
- Polish (Phase 8): depends on completed story phases selected for release.

### User Story Dependencies

- US1 (P1): starts immediately after Foundational and delivers MVP safety gate behavior.
- US2 (P1): depends on US1 proposal contract and foundational gate service.
- US5 (P1): runs in parallel with US1 and US2 after foundational pieces exist.
- US3 (P2): depends on confirm flow from US2 and gate lifecycle from foundational phase.
- US4 (P2): depends on confirm flow from US2 and ownership/status checks in gate service.

### Within Each User Story

- Write tests first and ensure failures before implementation.
- Backend domain and controller behavior before frontend wiring.
- Contract changes before UI consumer behavior.

---

## Parallel Execution Examples

### US1

- T017, T018, and T019 can run in parallel.
- T020 and T021 can run in parallel once foundational service contracts are stable.

### US2

- T025, T026, and T027 can run in parallel.
- T031 and T032 can run in parallel while backend endpoint implementation (T028-T030) is in progress.

### US5

- T035, T036, and T037 can run in parallel.
- T038 and T039 can run in parallel once shared audit helper structure is set.

### US3

- T041 and T042 can run in parallel.

### US4

- T046, T047, and T048 can run in parallel.

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete US1 (proposal without execution).
3. Complete US2 (explicit confirmation execution).
4. Validate end-to-end AC4 safety property.

### Incremental Delivery

1. Add US5 audit completeness.
2. Add US3 expiry handling.
3. Add US4 non-leaking rejection behavior.
4. Complete polish and regression suites.

### Parallel Team Strategy

1. Backend stream: entity/repository/service/controller + tests.
2. AI/chat stream: tool wiring + chat response contract.
3. Frontend stream: confirmation card UI + API/hook + tests.

---

## Notes

- Task format is strict: `- [ ] T### [P?] [US?] Description with file path`.
- Story labels are present only in user-story phases.
- This plan keeps confirmation out of free-text chat and routes all execution through POST /api/chat/confirmations/{token}.