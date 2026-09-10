# Tasks: Multi-Step Agent Responses via MCP + RAG (GIC Rate Inquiries)

**Input**: Design documents from `/specs/003-agent-multistep-gic-rates/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md, contracts/

**Tests**: Tests are required because this feature changes chat behavior, internal contracts, MCP integration, and resilience semantics.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish feature scaffolding and baseline verification points.

- [X] T001 Verify current chatbot and MCP integration touchpoints in backend/src/main/java/com/group1/banking/config/ChatbotAiConfig.java
- [X] T002 Verify current chat-turn persistence flow in backend/src/main/java/com/group1/banking/repository/ChatInteractionLogRepository.java
- [X] T003 [P] Add empty knowledge-base article placeholder in backend/src/main/resources/knowledge-base/07-gics-explained.md
- [X] T004 [P] Add test class placeholders for new tracking and MCP behavior in backend/src/test/java/com/group1/banking/service/impl/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build reusable tool tracking, schema support, and MCP registration infrastructure required by all stories.

**CRITICAL**: No user story work begins until this phase is complete.

- [X] T005 Create per-turn tracker component in backend/src/main/java/com/group1/banking/service/impl/ToolSelectionTracker.java
- [X] T006 [P] Add `tools_used` schema evolution in backend/src/main/java/com/group1/banking/repository/ChatInteractionLogRepository.java
- [X] T007 Extend chat interaction log write signature to accept tools list in backend/src/main/java/com/group1/banking/repository/ChatInteractionLogRepository.java
- [X] T008 Wire tracker reset and drain points in backend/src/main/java/com/group1/banking/service/impl/SavingsInsightChatService.java
- [X] T009 [P] Register MCP `ToolCallbackProvider` alongside existing tools in backend/src/main/java/com/group1/banking/config/ChatbotAiConfig.java
- [X] T010 Move rates MCP connection config to default profile with lazy initialization in backend/src/main/resources/application.properties
- [X] T011 [P] Reduce or remove obsolete rate-server settings in backend/src/main/resources/application-mcptest.properties
- [X] T012 Add foundational repository and tracker tests in backend/src/test/java/com/group1/banking/repository/ChatInteractionLogRepositoryTest.java and backend/src/test/java/com/group1/banking/service/impl/ToolSelectionTrackerTest.java

**Checkpoint**: Tool tracking, persistence, and base MCP wiring are available for story implementation.

---

## Phase 3: User Story 1 - Agent Combines MCP Rates + RAG in One Response (Priority: P1) MVP

**Goal**: Answer GIC inquiries with a single response that combines live MCP rate output and RAG educational guidance.

**Independent Test**: Ask a GIC-rate suitability question through `/api/chat/savings-insights` and verify response includes both term rate and GIC education concepts.

### Tests for User Story 1

- [X] T013 [P] [US1] Add multi-tool invocation response test in backend/src/test/java/com/group1/banking/service/impl/SavingsInsightChatServiceTest.java
- [X] T014 [P] [US1] Add ChatClient configuration test asserting MCP provider is included in default tools in backend/src/test/java/com/group1/banking/config/ChatbotAiConfigTest.java
- [X] T015 [P] [US1] Add MCP tool output formatting test for all GIC terms in mcp-test-server/src/test/java/com/voltio/mcptestserver/GicRateToolTest.java

### Implementation for User Story 1

- [X] T016 [US1] Add GIC rate MCP tool implementation in mcp-test-server/src/main/java/com/voltio/mcptestserver/GicRateTool.java
- [X] T017 [US1] Register GIC rate tool in MCP server tool config in mcp-test-server/src/main/java/com/voltio/mcptestserver/McpServerToolConfig.java
- [X] T018 [US1] Add GIC multi-step prompt guidance in backend/src/main/java/com/group1/banking/config/ChatbotAiConfig.java
- [X] T019 [US1] Add GIC educational knowledge article content in backend/src/main/resources/knowledge-base/07-gics-explained.md
- [X] T020 [US1] Ensure no explicit plan-object/extra API-roundtrip path is introduced in backend/src/main/java/com/group1/banking/service/impl/SavingsInsightChatService.java

---

## Phase 4: User Story 2 - Persist Selected Tools for Every Chat Turn (Priority: P1)

**Goal**: Persist the names of tools actually invoked per turn in `chat_interaction_log.tools_used`.

**Independent Test**: Trigger a turn invoking MCP + in-process tools and verify persisted row includes both names; trigger greeting turn and verify empty/absent tools list.

### Tests for User Story 2

- [X] T021 [P] [US2] Add persistence test for non-empty tools list serialization in backend/src/test/java/com/group1/banking/repository/ChatInteractionLogRepositoryTest.java
- [X] T022 [P] [US2] Add persistence test for tool-less turn storing null tools_used in backend/src/test/java/com/group1/banking/repository/ChatInteractionLogRepositoryTest.java
- [X] T023 [P] [US2] Add service test verifying all tool families are captured (SavingsChatTools, TransferChatTool, MCP) in backend/src/test/java/com/group1/banking/service/impl/SavingsInsightChatServiceTest.java

### Implementation for User Story 2

- [X] T024 [US2] Add tool-recording hooks to existing in-process tools in backend/src/main/java/com/group1/banking/service/impl/SavingsChatTools.java
- [X] T025 [US2] Add tool-recording hook to transfer tool in backend/src/main/java/com/group1/banking/service/impl/TransferChatTool.java
- [X] T026 [US2] Add tool-recording bridge for MCP tool callbacks in backend/src/main/java/com/group1/banking/config/ChatbotAiConfig.java
- [X] T027 [US2] Persist drained tool names with chat log writes in backend/src/main/java/com/group1/banking/service/impl/SavingsInsightChatService.java
- [X] T028 [US2] Keep audit linkage unchanged while relying on chat log resource pointer in backend/src/main/java/com/group1/banking/service/AuditService.java

---

## Phase 5: User Story 3 - Backend and Chat Stay Available if Rates MCP Server Is Down (Priority: P2)

**Goal**: Backend startup and chat responses remain available when rates MCP server is unavailable.

**Independent Test**: Start backend with MCP server down, then ask GIC-rate question and verify successful degraded response instead of request failure.

### Tests for User Story 3

- [X] T029 [P] [US3] Add backend startup resilience test with unavailable MCP endpoint in backend/src/test/java/com/group1/banking/config/McpClientDiagnosticsTest.java
- [X] T030 [P] [US3] Add degraded chat response test for MCP tool outage in backend/src/test/java/com/group1/banking/service/impl/SavingsInsightChatServiceTest.java
- [X] T031 [P] [US3] Add regression test that tool exceptions do not fail request when `throw-exception-on-error=false` in backend/src/test/java/com/group1/banking/service/impl/SavingsInsightChatServiceTest.java

### Implementation for User Story 3

- [X] T032 [US3] Set lazy MCP client initialization and connection properties in backend/src/main/resources/application.properties
- [X] T033 [US3] Preserve conversational fallback behavior for MCP tool failures in backend/src/main/java/com/group1/banking/service/impl/SavingsInsightChatService.java
- [X] T034 [US3] Update diagnostics wording to production MCP role in backend/src/main/java/com/group1/banking/config/McpClientDiagnostics.java

---

## Phase 6: User Story 4 - Incremental Knowledge Ingestion for New Files (Priority: P2)

**Goal**: Newly added knowledge files ingest into seeded environments without duplicating existing documents.

**Independent Test**: Seed with files 01-06, add file 07, restart, and verify all seven sources exist without duplicates.

### Tests for User Story 4

- [X] T035 [P] [US4] Add ingestion test for seeded-table incremental file ingest in backend/src/test/java/com/group1/banking/service/impl/KnowledgeBaseIngestionRunnerTest.java
- [X] T036 [P] [US4] Add first-run ingest test for all knowledge files in backend/src/test/java/com/group1/banking/service/impl/KnowledgeBaseIngestionRunnerTest.java
- [X] T037 [P] [US4] Add no-duplicate re-run ingest test in backend/src/test/java/com/group1/banking/service/impl/KnowledgeBaseIngestionRunnerTest.java

### Implementation for User Story 4

- [X] T038 [US4] Replace table-count ingest gate with per-source-file existence check in backend/src/main/java/com/group1/banking/service/impl/KnowledgeBaseIngestionRunner.java
- [X] T039 [US4] Keep source metadata assignment stable for dedupe key in backend/src/main/java/com/group1/banking/service/impl/KnowledgeBaseIngestionRunner.java
- [X] T040 [US4] Add startup logs clarifying incremental ingest outcomes in backend/src/main/java/com/group1/banking/service/impl/KnowledgeBaseIngestionRunner.java

---

## Phase 7: User Story 5 - MCP Module Name and Description Match Real Purpose (Priority: P3)

**Goal**: MCP module naming and descriptions clearly reflect production-relevant GIC rate tool role.

**Independent Test**: Verify module path/artifact/description and config references no longer describe disposable-only test scaffolding.

### Tests for User Story 5

- [X] T041 [P] [US5] Add module metadata verification test for updated artifact/name/description in voltio-rates-mcp-server/pom.xml
- [X] T042 [P] [US5] Add configuration reference consistency checks in backend/src/main/resources/application.properties and backend/src/main/resources/application-mcptest.properties

### Implementation for User Story 5

- [X] T043 [US5] Rename module directory from mcp-test-server/ to voltio-rates-mcp-server/
- [X] T044 [US5] Update Maven artifactId, project name, and description in voltio-rates-mcp-server/pom.xml
- [X] T045 [US5] Update package comments and tool descriptions for non-disposable purpose in voltio-rates-mcp-server/src/main/java/com/voltio/mcptestserver/PingTool.java
- [X] T046 [US5] Update backend config comments and onboarding references to renamed module in backend/src/main/resources/application.properties

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Complete regression validation, observability checks, and release-readiness.

- [X] T047 [P] Verify unchanged public chat contract behavior in backend/src/test/java/com/group1/banking/controller/SavingsInsightChatControllerTest.java
- [X] T048 [P] Verify centralized error-mapping behavior remains unchanged in src/api/axiosClient.js
- [X] T049 [P] Verify ChatWidget behavior across Classic and Neon themes for unchanged chat contract in src/components/ChatWidget.jsx
- [ ] T050 [P] Run targeted backend suites for tracker, MCP, ingestion, and logging in backend/src/test/java/com/group1/banking/
- [ ] T051 [P] Run targeted MCP module test suite in voltio-rates-mcp-server/src/test/java/com/voltio/mcptestserver/
- [ ] T052 Run full backend test suite in backend/
- [ ] T053 Run full frontend test suite in src/test/
- [X] T054 Update local runbook/startup notes for MCP dependency behavior in README.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup completion and blocks all user stories.
- **User Story Phases (Phase 3-7)**: Depend on Foundational completion.
- **Polish (Phase 8)**: Depends on completion of desired story phases.

### User Story Dependencies

- **US1 (P1)**: Starts immediately after Foundational; delivers core multi-step capability.
- **US2 (P1)**: Depends on Foundational and should land with US1 for traceability completeness.
- **US3 (P2)**: Depends on MCP wiring from US1.
- **US4 (P2)**: Independent of US3; depends only on existing knowledge ingest pipeline.
- **US5 (P3)**: Mostly independent refactor/docs; safest after US1 MCP tool lands.

### Within Each User Story

- Tests first and failing before implementation.
- Infrastructure and model changes before orchestration wiring.
- Backend behavior before documentation and polish checks.

---

## Parallel Execution Examples

### US1 Parallel Example

- T013, T014, and T015 can run in parallel.
- T016 and T019 can run in parallel, then T017 and T018, then T020.

### US2 Parallel Example

- T021, T022, and T023 can run in parallel.
- T024 and T025 can run in parallel, then T026 and T027.

### US3 Parallel Example

- T029, T030, and T031 can run in parallel.
- T032 and T034 can run in parallel, then T033.

### US4 Parallel Example

- T035, T036, and T037 can run in parallel.
- T038 and T040 can run in parallel, then T039.

### US5 Parallel Example

- T041 and T042 can run in parallel.
- T044 and T045 can run in parallel after T043.

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Complete Phase 1 and Phase 2.
2. Implement US1 (multi-step MCP + RAG response).
3. Implement US2 (tools_used logging for all tool types).
4. Validate SC-001 and SC-002 before expanding scope.

### Incremental Delivery

1. Add US3 resilience behavior.
2. Add US4 incremental ingestion behavior.
3. Add US5 module rename and doc alignment.
4. Run cross-cutting regression and release checks.

### Parallel Team Strategy

1. **Backend orchestration track**: ChatbotAiConfig, SavingsInsightChatService, trackers, repository.
2. **MCP server track**: GicRateTool + module rename and metadata updates.
3. **Knowledge pipeline track**: KnowledgeBaseIngestionRunner + knowledge article and tests.

---

## Notes

- Tasks follow strict checklist format: `- [ ] T### [P?] [US?] Description with file path`.
- Story labels are included only in user-story phases.
- This breakdown preserves the spec constraint that no explicit plan object or extra chat endpoint is introduced.
