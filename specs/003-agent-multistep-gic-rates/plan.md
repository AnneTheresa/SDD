# Implementation Plan: Multi-Step Agent Responses via MCP + RAG (GIC Rate Inquiries)

**Branch**: `003-agent-multistep-gic-rates` | **Date**: 2026-08-27 | **Spec**: specs/003-agent-multistep-gic-rates/spec.md

**Input**: Feature specification from `/specs/003-agent-multistep-gic-rates/spec.md`

## Summary

Enable the Savings Insight chatbot to answer a single user question by combining two tools in one turn: an MCP-served GIC rate tool and the existing RAG knowledge-base search. Keep chat contracts unchanged while adding internal per-turn tool-selection logging, make MCP startup resilient when the rates server is down, and change knowledge ingestion from table-level gating to per-file incremental ingestion so new articles are picked up in already-seeded environments.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.0, Spring AI 2.0.0), JavaScript/React (Vite)

**Primary Dependencies**: Spring AI ChatClient + ToolCallbackProvider, Spring AI MCP client/server webmvc + SSE, JdbcTemplate, pgvector vector store, React Query, axios

**Storage**: Primary app datasource (MySQL/H2) unchanged; chatbot Postgres datasource (`chat_interaction_log` + pgvector table) extended with `tools_used` column

**Testing**: JUnit 5, Mockito, Spring Boot test slices (`@WebMvcTest`, service/repository tests), Vitest + RTL for chat widget behavior

**Target Platform**: Web banking backend + frontend, plus standalone MCP server process on local/CI/dev hosts

**Project Type**: Full-stack web application with companion standalone MCP server module

**Performance Goals**: Single-turn combined response within existing chat request cycle; no additional API round trip; no backend startup failure when MCP server unavailable

**Constraints**:
- Keep `/api/chat/savings-insights` request/response contract unchanged
- Keep `spring.ai.tools.throw-exception-on-error=false`
- Keep `AuditService.log(...)` six-argument signature unchanged
- Preserve module independence between backend GIC enum and MCP rate tool values

**Scale/Scope**:
- One new MCP tool (`getGicRates`) + one new knowledge file
- Per-turn tool tracking for all existing and new tools
- One new chat log column (`tools_used`) and no new external endpoint

## Constitution Check (Pre-Design Gate)

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] API contract impacts identified (public chat API unchanged; internal tool/logging contracts changed).
- [x] Contract-affecting changes include same-PR updates for backend mappings, tests, and frontend consumers where behavior is visible.
- [x] Error semantics remain standardized (tool failures surfaced to model; frontend keeps centralized mapping).
- [x] Security/CORS implications reviewed (no new public endpoint; existing chat auth path retained).
- [x] Environment parity confirmed (MCP host/port in config; lazy init avoids boot coupling).
- [x] Layer ownership preserved (tool selection and persistence in backend services/repository; UI only displays response).
- [x] Testability gate defined (success path + degraded MCP-down path + logging correctness + ingestion behavior).
- [x] No silent degradation risk accepted without traceability (tool usage persisted; degraded tool calls still return conversational response).

## Phase 0: Research Output

Research decisions are recorded in `specs/003-agent-multistep-gic-rates/research.md` and resolve all open implementation choices:
- MCP integration mode and lazy initialization behavior
- Tool tracking representation and persistence shape
- Incremental knowledge ingestion strategy
- Module rename target and compatibility touchpoints

## Phase 1: Design Output

Generated design artifacts:
- Data model: `specs/003-agent-multistep-gic-rates/data-model.md`
- Contracts: `specs/003-agent-multistep-gic-rates/contracts/`
- Validation guide: `specs/003-agent-multistep-gic-rates/quickstart.md`

## Project Structure

### Documentation (this feature)

```text
specs/003-agent-multistep-gic-rates/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── chat-savings-insights-contract.md
│   ├── mcp-get-gic-rates-contract.md
│   └── chat-log-tools-used-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/group1/banking/config/
│   ├── ChatbotAiConfig.java
│   └── McpClientDiagnostics.java
├── src/main/java/com/group1/banking/service/impl/
│   ├── SavingsInsightChatService.java
│   ├── SavingsChatTools.java
│   ├── TransferChatTool.java
│   ├── KnowledgeBaseIngestionRunner.java
│   └── ToolSelectionTracker.java                # new
├── src/main/java/com/group1/banking/repository/
│   └── ChatInteractionLogRepository.java
├── src/main/resources/
│   ├── application.properties
│   ├── application-mcptest.properties           # likely removed or reduced
│   └── knowledge-base/
│       └── 07-gics-explained.md                # new
└── src/test/java/com/group1/banking/
    ├── service/impl/
    ├── repository/
    └── config/

mcp-test-server/                                  # renamed in this feature
├── pom.xml
├── src/main/java/com/voltio/mcptestserver/
│   ├── McpServerToolConfig.java
│   ├── PingTool.java
│   └── GicRateTool.java                         # new
└── src/main/resources/application.properties

src/
└── components/
    └── ChatWidget.jsx                           # behavior verification only
```

**Structure Decision**: Keep existing web-app + standalone MCP module architecture. Extend backend chat orchestration and logging, add one MCP tool in the server module, and avoid introducing new public API surface.

## Constitution Check (Post-Design Re-check)

- [x] Contract changes are explicit and bounded (public API unchanged; internal contracts documented in contracts files).
- [x] Error-handling flow remains centralized and consistent with existing chatbot behavior.
- [x] Security posture unchanged for public surface; no new unauthenticated route added.
- [x] Environment parity preserved by moving MCP connection config into base config with lazy startup.
- [x] Business logic remains in backend services/repositories; no UI-layer policy logic introduced.
- [x] Test plan covers success, degraded dependency, and business-correctness paths.
- [x] Theme parity impact is minimal (no new visual component required), but quickstart includes visual regression checks for existing chat UI in both themes.

## Complexity Tracking

No constitution violations require exception.
