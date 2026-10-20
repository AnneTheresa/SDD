# Implementation Plan: Developer Chatbot MCP

**Branch**: `006-developer-chatbot-mcp` | **Date**: 2026-10-20 | **Spec**: [source feature spec](../005-developer-repo-chatbot/spec.md)

**Input**: Feature specification currently stored at `specs/005-developer-repo-chatbot/spec.md`; this branch-scoped plan is placed under `specs/006-developer-chatbot-mcp/`.

## Summary

Correct the developer MCP server's documentation root so it reads the contributor documentation in `Documentation/` rather than defaulting to feature specs. Make documentation-first answers for contributor setup and architecture questions explicit, verify that citations/statuses from documentation and GitHub reach the chat UI, and validate that current GitHub owner/repository/branch/token configuration matches the repository now owned by the user. Preserve the existing developer-only chat endpoint, read-only source tools, and evidence-required failure behavior.

## Technical Context

**Language/Version**: Java 21; JavaScript/React in the web client

**Primary Dependencies**: Spring Boot 4.1.1 and Spring AI 2.0.1 in `mcpdevserver/`; Spring AI chat client and MCP SSE client in `backend/`; React, React Query, Axios, and Vitest in the frontend

**Storage**: No new storage. Developer conversation context is held in bounded in-memory sessions; documentation is read from Markdown files and source code from the configured GitHub repository.

**Testing**: JUnit 5, Mockito, Spring Boot tests for MCP/backend; Vitest and React Testing Library for the developer chat UI

**Target Platform**: Local/CI/deployed full-stack banking application and its private standalone MCP server

**Project Type**: Full-stack web application with a standalone Spring MCP server

**Performance Goals**: Keep documentation reads within the existing chat request; bounded Markdown results (up to 5) and excerpts (up to 4,000 characters); do not introduce an external index or dependency.

**Constraints**:
- Keep `POST /api/developer/chat` and its request/response contract unchanged unless focused tests demonstrate a requirement gap.
- Configure documentation and GitHub access through environment-specific settings; do not add secrets to source.
- Keep GitHub access scoped to one configured repository and branch and read-only.
- Keep the MCP service isolated from customer chat and keep its local listener on loopback.
- Preserve source citations, content redaction, path containment, untrusted-evidence handling, and explicit source failure statuses.
- Do not expose the MCP endpoint publicly or weaken existing application authorization/CORS.

**Scale/Scope**: One contributor documentation folder, one configured GitHub repository/branch, four read-only MCP tools, one developer chat endpoint and existing frontend page.

## Constitution Check (Pre-Design Gate)

- [x] API contract impact identified: preserve the existing developer chat API; MCP source paths/configuration are internal integration contracts.
- [x] Contract-affecting changes include tests and any affected frontend citations/status presentation.
- [x] Error semantics remain explicit through source statuses and existing backend/frontend error mapping.
- [x] Security review identified `DEVELOPER` endpoint authorization, read-only GitHub access, private MCP exposure, and no CORS expansion.
- [x] Environment parity uses `DOCUMENTATION_ROOT`, GitHub scope/token variables, and the existing MCP URL setting; no committed secrets or environment-specific hostnames.
- [x] Layer ownership is preserved: source reads in MCP tools, orchestration/evidence in backend services, presentation in the React developer chat page.
- [x] Testability covers source success, auth/config failures, partial outages, and unsupported questions.
- [x] No silent degradation: evidence-free model answers remain withheld and source outages remain visible.
- [x] UI behavior remains consistent in both Classic and Neon themes; no theme-specific redesign is in scope.

## Research Summary

Decisions and rationale are recorded in [research.md](./research.md). The critical finding is that both the MCP default and the local setup guide currently target `../specs`; from the documented `mcpdevserver/` launch directory the contributor documentation root is `../Documentation`. The GitHub integration is already scoped/configurable, so ownership change requires validating runtime owner/name/branch and token permissions rather than putting a new credential in code.

## Applied Guidelines

- Follow the project constitution's contract-before-code, least-privilege, environment-parity, layer-ownership, testability, no-silent-degradation, and theme-parity requirements.
- Preserve the MCP server's current bounded Markdown search/read, path containment, redaction, and source marker conventions.
- No additional local framework-specific migration guidelines were found in the repository.

## Implementation Steps

### Phase 1: Correct documentation source configuration

#### Step 1.1: Point the MCP server at contributor documentation

Update the default in `mcpdevserver/src/main/resources/application.properties` and any configuration default as needed so launching from `mcpdevserver/` searches `../Documentation`. Keep `DOCUMENTATION_ROOT` overrideable and clarify relative path resolution. Update `SETUP.md` to stop describing `../specs` as the default and document the new directory and expected launch working directory.

**Requirements**: REQ-002, REQ-004, REQ-006, REQ-016

#### Step 1.2: Prove recursive documentation discovery and citations

Extend `DocumentationToolsTest` to search nested content under a temporary Documentation root, return the expected relative source path, reject reads outside the root, and report missing/unreadable configuration explicitly. Keep existing tests for redaction and evidence-marker escaping.

**Requirements**: REQ-002, REQ-004, REQ-006, REQ-008, REQ-009

### Phase 2: Ground answers in the correct evidence

#### Step 2.1: Define documentation-first and code-first tool use

Refine the developer chat system guidance in `backend/src/main/java/com/group1/banking/config/ChatbotAiConfig.java`: use documentation for setup and contributor guidance, GitHub for code-specific facts, and both when needed; distinguish the source types; cite only returned evidence; state when tools fail or sources conflict. Keep developer tools isolated from customer chat and test the tool wiring.

Decode Spring AI's serialized MCP text results before evidence tracking. If the chat/model request fails or GitHub is unavailable without documentation evidence, directly search the dedicated documentation tool and return a cited excerpt when it contains relevant content.

**Requirements**: REQ-002, REQ-003, REQ-005, REQ-006, REQ-007, REQ-008

#### Step 2.2: Validate GitHub repository scope and credential configuration

Verify `GITHUB_REPOSITORY_OWNER`, `GITHUB_REPOSITORY_NAME`, `GITHUB_REPOSITORY_BRANCH`, `GITHUB_READ_ONLY_TOKEN`, and the API base URL against the current repository. Update setup guidance and stale test fixtures (including the existing owner-specific GitHub and frontend citation fixtures) to use the configured repository scope. Test authenticated code search and file reads, repository-scope enforcement, missing/invalid credentials, and safe unavailable statuses without logging credential values.

**Requirements**: REQ-003, REQ-004, REQ-006, REQ-007, REQ-009, REQ-011, REQ-015, REQ-016

#### Step 2.3: Preserve evidence tracking through the chat response

Add/extend backend tests to prove documentation and GitHub tool outputs become `DeveloperChatSource` entries and source statuses, that prior-turn context does not become a citation, and that no uncited repository-specific model text escapes when sources return no evidence.

**Requirements**: REQ-004, REQ-005, REQ-006, REQ-011, REQ-013

### Phase 3: Preserve the authorized developer experience

#### Step 3.1: Verify authenticated developer chat and source presentation

Keep the existing `DeveloperChatController`, DTOs, `src/api/developerChat.js`, `src/hooks/useDeveloperChat.js`, and `src/pages/DeveloperChatPage.jsx` contract stable. Test that documentation citations display their relative path and source type, GitHub citations remain clickable, revision/status is visible, and unavailable/no-evidence outcomes are not misrepresented.

**Requirements**: REQ-001, REQ-004, REQ-006, REQ-010, REQ-011, REQ-012, REQ-013

#### Step 3.2: Validate role boundaries and account provisioning

Retain `DEVELOPER` authorization on the chat endpoint and review the existing protected provisioning behavior in `AuthController` and `AuthServiceImpl`: developers provision only developers; bank administrators may provision developers or bank administrators; public registration remains retail-only. Add/adjust tests for each success and denied role path; do not add role-creation controls to the frontend.

**Requirements**: REQ-001, REQ-007, REQ-009, REQ-014, REQ-017, REQ-018

### Phase 4: Verify source degradation and end-to-end quality

#### Step 4.1: Exercise independent source availability

Test GitHub unavailable/documentation available, documentation unavailable/GitHub available, both unavailable, and no matching evidence. Verify explicit status and evidence boundaries and confirm customer-facing chat remains independent of the developer MCP server.

**Requirements**: REQ-006, REQ-010, REQ-013

#### Step 4.2: Run a representative answer-quality evaluation

Create or extend a deterministic evaluation set with at least 30 development questions spanning Documentation-only, GitHub-only, combined, conflicting, unavailable, and no-evidence scenarios. Measure correct answers and relevant source references against SC-001; run the contributor usability check and theme-parity verification for any user-visible changes.

**Requirements**: REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-011, REQ-014

#### Step 4.3: Complete operational documentation and smoke validation

Update `SETUP.md` and `Documentation/README.md` so the Documentation folder's role and MCP configuration are clear. Follow [quickstart.md](./quickstart.md) for local MCP/backend/frontend tests, source-outage behavior, auth boundaries, and end-to-end citations.

**Requirements**: REQ-002, REQ-006, REQ-010, REQ-015, REQ-016, REQ-017

## Task Breakdown

### Phase 1: Documentation source

- [X] T001 [Plan:1.1] Change the documentation-root default to `../Documentation` and preserve environment override in `mcpdevserver/src/main/resources/application.properties` and `mcpdevserver/src/main/java/com/voltio/mcpdevserver/config/McpDevServerProperties.java`.
- [X] T002 [P] [US2] [Plan:1.2] Add nested-file, relative-citation, root-containment, and unavailable-root cases in `mcpdevserver/src/test/java/com/voltio/mcpdevserver/tools/DocumentationToolsTest.java`.
- [X] T003 [P] [US2] [Plan:1.1,4.3] Correct the documentation-root and launch guidance in `SETUP.md` and describe the intended Documentation corpus in `Documentation/README.md`.

### Phase 2: Evidence and GitHub configuration

- [X] T004 [US1] [Plan:2.1] Update developer-specific source-selection and citation instructions in `backend/src/main/java/com/group1/banking/config/ChatbotAiConfig.java` without changing customer chat tools.
- [X] T005 [P] [US1] [Plan:2.1] Extend `backend/src/test/java/com/group1/banking/config/ChatbotAiConfigTest.java` to verify developer/customer tool isolation and developer evidence tracking; add MCP connection-isolation and Spring client-collection wiring regression tests.
- [ ] T006 [US3] [Plan:2.2] Document and verify the current read-only GitHub owner/repository/branch/token settings in `SETUP.md` and `mcpdevserver/src/main/resources/application.properties`, without adding a secret to source.
- [X] T007 [P] [US3] [Plan:2.2] Update configured-owner fixtures and cover authentication/configuration failures in `mcpdevserver/src/test/java/com/voltio/mcpdevserver/tools/GitHubRepositoryToolsTest.java` and `src/pages/DeveloperChatPage.test.jsx`.
- [X] T008 [US3] [Plan:2.3] Extend `backend/src/test/java/com/group1/banking/service/impl/DeveloperChatServiceTest.java` and add `backend/src/test/java/com/group1/banking/service/impl/DeveloperChatEvidenceTrackerTest.java` for citations, statuses, partial outages, no evidence, and untrusted prior turns.

### Phase 3: Access and user experience

- [ ] T009 [P] [US1] [Plan:3.1] Verify stable response and error contract in `backend/src/test/java/com/group1/banking/controller/DeveloperChatControllerTest.java` and render documentation/GitHub source types and statuses in `src/pages/DeveloperChatPage.test.jsx`.
- [ ] T010 [US1] [Plan:3.2] Add permitted/denied provisioning-role cases in `backend/src/test/java/com/group1/banking/controller/AuthControllerTest.java` and `backend/src/test/java/com/group1/banking/service/AuthServiceImplTest.java`; review `backend/src/main/java/com/group1/banking/config/SecurityConfig.java` and `src/pages/` for public role controls.

### Phase 4: Evaluation and verification

- [X] T011 [US4] [Plan:4.1] Add source-outage and customer-chat independence regression cases in `backend/src/test/java/com/group1/banking/service/impl/DeveloperChatServiceTest.java` and `backend/src/test/java/com/group1/banking/config/ChatbotAiConfigTest.java`.
- [ ] T012 [US1] [Plan:4.2] Add a repeatable 30-question source-grounded evaluation set in `backend/src/test/resources/developer-chat-evaluation.json` and assertions in `backend/src/test/java/com/group1/banking/service/impl/DeveloperChatEvaluationTest.java` for answer correctness, source type/path, and failure disclosure.
- [ ] T013 [Plan:4.3] From each module directory, run `.\mvnw.cmd test` in `mcpdevserver/` and `backend/`, then run `npm test -- src/pages/DeveloperChatPage.test.jsx` from the repository root; resolve only failures introduced by this feature.
- [ ] T014 [US4] [Plan:4.2,4.3] Execute the scenarios in `specs/006-developer-chatbot-mcp/quickstart.md`, measure SC-001 against the 30-question set, and record Classic/Neon parity for any visible UI change.

## Project Structure

### Documentation (this feature)

```text
specs/006-developer-chatbot-mcp/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    ├── developer-chat-http-contract.md
    ├── developer-mcp-tools-contract.md
    └── developer-source-configuration-contract.md
```

### Source Code (repository root)

```text
Documentation/
├── README.md
└── onboarding/project-overview/architecture-overview.md

mcpdevserver/
├── src/main/java/com/voltio/mcpdevserver/config/
├── src/main/java/com/voltio/mcpdevserver/tools/
├── src/main/resources/application.properties
└── src/test/java/com/voltio/mcpdevserver/tools/

backend/src/
├── main/java/com/group1/banking/config/ChatbotAiConfig.java
├── main/java/com/group1/banking/controller/DeveloperChatController.java
├── main/java/com/group1/banking/service/impl/DeveloperChatService.java
├── main/java/com/group1/banking/service/impl/DeveloperChatEvidenceTracker.java
└── test/java/com/group1/banking/

src/
├── api/developerChat.js
├── hooks/useDeveloperChat.js
└── pages/DeveloperChatPage.jsx
```

**Structure Decision**: Keep the existing full-stack app and separate MCP server. The MCP server remains the bounded read-only adapter for Documentation and GitHub, while the backend retains authorization, model orchestration, evidence tracking, and response contract ownership.

## Constitution Check (Post-Design Re-check)

- [x] No public API shape change is planned; internal MCP source configuration and evidence behavior are documented.
- [x] The plan includes focused controller/service/MCP/frontend tests for visible and contract behavior.
- [x] Source and authorization failures remain explicit and do not become uncited model answers.
- [x] Secrets remain environment-supplied; MCP remote exposure remains private and the local binding remains loopback.
- [x] Documentation and GitHub roots are environment-configurable and do not rely on hardcoded deployment paths.
- [x] Responsibilities remain in existing configuration, MCP tools, backend service, and UI layers.
- [x] UI changes, if any, require Classic and Neon verification.

## Requirement Mapping

| REQ ID | Description | Plan Items | Implementation Evidence |
|---|---|---|---|
| REQ-001 | Authorized contributor chatbot distinct from customer chat | 2.1, 3.1, 3.2 | `ChatbotAiConfig.java`, `DeveloperChatController.java`, `DeveloperChatPage.jsx`, role tests |
| REQ-002 | Use relevant dedicated documentation | 1.1, 1.2, 2.1, 4.2, 4.3 | MCP properties, `DocumentationToolsTest.java`, `ChatbotAiConfig.java`, evaluation cases |
| REQ-003 | Inspect configured GitHub repository | 2.1, 2.2, 4.2 | `GitHubRepositoryTools.java`, scoped API tests, evaluation cases |
| REQ-004 | Identify supporting docs and repository locations | 1.2, 2.2, 2.3, 3.1, 4.2 | MCP source markers, `DeveloperChatSource.java`, UI citation tests |
| REQ-005 | Distinguish documentation from repository observations | 2.1, 2.3, 4.2 | Developer system guidance, evidence tracker/service tests |
| REQ-006 | Disclose unavailable, stale, or insufficient evidence | 1.1, 1.2, 2.1, 2.2, 2.3, 4.1, 4.2, 4.3 | MCP source status tests, `DeveloperChatServiceTest.java`, outage evaluation |
| REQ-007 | Read-only access restricted to configured repository | 2.1, 2.2, 3.2 | MCP tool scope tests, tool isolation, authorization tests |
| REQ-008 | Treat source content as untrusted evidence | 1.2, 2.1, 2.3 | Redaction/evidence marker tests, prompt guidance, evidence tracker tests |
| REQ-009 | Do not expose credentials or unauthorized content | 1.2, 2.2, 3.2 | Redaction tests, secret-safe configuration guidance, role tests |
| REQ-010 | Developer assistant failure does not disable customer chat | 3.1, 4.1, 4.3 | MCP isolation tests and customer-chat regression tests |
| REQ-011 | Disclose repository version/scope when available | 2.2, 2.3, 3.1, 4.2 | `DeveloperChatSource.java`, branch citation tests, evaluation assertions |
| REQ-012 | Identify API/MCP contracts and consumers | 3.1 | HTTP and MCP contract documents, consumer tests |
| REQ-013 | Stable, distinct authorization/source failure outcomes | 2.3, 3.1, 4.1 | HTTP contract, source status tests, chat service tests |
| REQ-014 | Restrict access to authorized contributors | 3.2, 4.2 | `DeveloperChatControllerTest.java`, security review and role checks |
| REQ-015 | Read-only least-privilege GitHub credential outside source | 2.2, 4.3 | GitHub configuration contract, setup guide, no-secret verification |
| REQ-016 | Configurable source locations and environment settings | 1.1, 2.2, 4.3 | MCP properties, configuration contract, setup guide |
| REQ-017 | Document authorization, endpoint exposure, and CORS impact | 3.2, 4.3 | HTTP/MCP contracts, `SecurityConfig.java` review, no-CORS-expansion checks |
| REQ-018 | Role-limited provisioning and retail-only public registration | 3.2 | `AuthController.java`, `AuthServiceImpl.java`, auth controller/service tests |

## Complexity Tracking

No constitution violations or new architectural components are proposed.
