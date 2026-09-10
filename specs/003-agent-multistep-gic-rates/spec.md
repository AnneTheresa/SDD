# Feature Specification: Multi-Step Agent Responses via MCP + RAG (GIC Rate Inquiries)

**Feature Branch**: `[to-be-set-by-git-hook]`

**Created**: August 27, 2026

**Status**: Draft

**Input**: User description: "Scenario 2 from the MCP Agent ticket - the Agent must be able to build and execute a multi-step plan that combines data from more than one source (an MCP service and RAG-retrieved knowledge, per the ticket's own mortgage-inquiry example) and generate a single combined response. Demonstrated with something native to Voltio instead of an invented mortgage product: current GIC rates served by an MCP tool, combined with GIC-education content from the existing RAG knowledge base. Also closes a gap in AC1/Scenario 5 (intent/tool-selection logging) that applies to every chat turn, not just this one, and two smaller operational fixes surfaced while designing this (knowledge-base ingestion is table-content-gated rather than per-file, and `mcp-test-server`'s description no longer matches its purpose once it hosts a real tool)."

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Agent answers a GIC question by combining an MCP tool with RAG knowledge (Priority: P1)

A customer asks the Savings Insight chatbot something like "What's the current rate on a 1-year GIC, and would that make sense for my emergency fund?" The agent calls a new MCP tool to get the live rate and the existing knowledge-base search tool to pull GIC-education content, then combines both into one coherent answer.

**Why this priority**: This is the core capability the ticket asks for (Scenario 2/AC1's "creates an execution plan... executes multi-step requests... combines results from all steps into one coherent answer"). Without it, nothing else in this feature has a reason to exist.

**Independent Test**: Send a GIC-rate question to `/api/chat/savings-insights` and verify the response text contains a rate figure consistent with the MCP tool's current data and also reflects concepts from the new knowledge-base article (e.g. locked-in term, guaranteed return), in a single response.

**Acceptance Scenarios**:

1. **Given** a customer asks for the current rate on a specific GIC term, **When** the agent processes the request, **Then** it calls the MCP rate tool and the reply states a rate matching what that tool currently returns for that term.
2. **Given** a customer asks both for a rate and whether a GIC suits their situation, **When** the agent processes the request, **Then** it calls both the MCP rate tool and the knowledge-base search tool in the same turn, and the reply combines information from both into one response rather than answering only one part.
3. **Given** a customer asks a GIC question, **When** the agent replies, **Then** no separate "plan" object or additional round trip is exposed to the caller - the combination happens within the existing single `/api/chat/savings-insights` request/response cycle, consistent with the existing native tool-calling loop (see Assumptions).

---

### User Story 2 - Selected tools are logged for every chat turn, not just this one (Priority: P1)

Per AC1/Scenario 5, the intent, selected Tool(s)/MCP service(s), and generated response must be logged for every request. Today's chat logging captures the query, reply, outcome, and knowledge-base citations, but not which specific tool(s) were actually invoked - a gap that becomes visible now that a chat turn can involve an MCP-sourced tool alongside in-process ones.

**Why this priority**: Directly required by the ticket (AC1's logging clause, Scenario 5) and is what makes multi-step tool selection verifiable after the fact rather than just trusted. Bundled with Story 1 at P1 because Scenario 2 is the first case where "which tool(s) were selected" (in-process vs. MCP) is actually a meaningful, checkable fact.

**Independent Test**: Send a chat request that triggers two tool calls (e.g. the GIC rate tool and the knowledge-base search tool), then read back the corresponding `chat_interaction_log` row and verify both tool names are present.

**Acceptance Scenarios**:

1. **Given** any chat turn that invokes one or more tools (in-process `@Tool` or MCP-sourced), **When** the turn completes, **Then** the names of every tool actually invoked during that turn are recorded on that turn's `chat_interaction_log` row.
2. **Given** a chat turn that invokes no tools at all (e.g. a simple greeting), **When** the turn completes, **Then** the tool-selection record for that turn is empty/absent, not a false entry.
3. **Given** the existing `SavingsChatTools`/`TransferChatTool` methods that predate this feature, **When** any of them is called during a turn, **Then** they are also captured by the same tool-selection tracking - this is not limited to the new GIC rate tool.

---

### User Story 3 - The backend and the chatbot both stay available when the rates MCP server is down (Priority: P2)

The GIC rate tool depends on a separate process (the renamed MCP server) being reachable. That dependency must not make the whole banking backend fail to start, and a mid-session outage must not crash an otherwise-answerable chat turn.

**Why this priority**: Necessary for the feature to be safe to ship at all, but the "happy path" (Story 1) is the reason this feature exists; this is a resilience backstop.

**Independent Test**: Start the backend with the rates MCP server not running, verify the backend starts successfully; then, with the backend running and the rates server still down, ask a GIC-rate question and verify the chat turn returns a response (degraded, but not an HTTP 500 / generic failure) rather than crashing.

**Acceptance Scenarios**:

1. **Given** the rates MCP server is not running, **When** the backend starts, **Then** the backend starts successfully (the MCP connection is attempted lazily, not eagerly at boot).
2. **Given** the rates MCP server is unreachable, **When** a customer asks a GIC-rate question, **Then** the chat turn still returns a response (the agent explains it can't retrieve current rates right now, optionally still using RAG-only content) rather than the request failing outright.

---

### User Story 4 - A newly added knowledge-base article gets ingested without wiping existing ones (Priority: P2)

The knowledge-base ingestion step today skips entirely if the vector table already has any rows, so an environment that already ingested the original six articles would silently never pick up the new GIC article added by this feature.

**Why this priority**: Blocks Story 1 from actually working end-to-end in any environment that already has knowledge-base content seeded (which is the expected state for anyone who has run this app before); not itself user-facing.

**Independent Test**: Seed the vector store with the existing six articles only (simulating an already-running environment), add the new GIC article to the knowledge-base folder, restart, and verify all seven articles - not just the new one, and without duplicating the original six - are present in the vector store afterward.

**Acceptance Scenarios**:

1. **Given** a vector store already seeded with some knowledge-base articles, **When** the application restarts after a new article is added to the knowledge-base folder, **Then** the new article is ingested and the previously-ingested articles are not duplicated.
2. **Given** a vector store with no knowledge-base content at all, **When** the application starts, **Then** all current knowledge-base articles are ingested, matching today's existing first-run behavior.

---

### User Story 5 - The MCP server's own description matches what it does (Priority: P3)

Once `mcp-test-server` hosts a real, depended-upon tool (the GIC rate lookup), its current self-description ("not part of the banking application... safe to delete") is actively misleading to the next person who reads it.

**Why this priority**: Documentation/naming correctness, not functional behavior - lowest priority, included because it was explicitly requested alongside this feature.

**Independent Test**: Read the module's `pom.xml` description and any other self-description text and confirm it no longer describes the module as disposable, connectivity-proof-only scaffolding.

**Acceptance Scenarios**:

1. **Given** the MCP server module now hosts the GIC rate tool, **When** someone reads its module description, **Then** the description reflects that it hosts real, depended-upon chatbot functionality.
2. **Given** the module is referenced elsewhere (onboarding docs, `application.properties` comments, the opt-in profile file), **When** those references are read, **Then** they use the module's updated name/description consistently rather than mixing old and new.

---

### Edge Cases

- What happens if `GicRateTool`'s mocked rate values drift out of sync with the backend's own `GicTerm` enum over time? Accepted, documented risk (see Assumptions) - the two modules cannot share a dependency, so this is a manual-sync responsibility, not something this feature can enforce automatically.
- What happens if the knowledge-base similarity search doesn't surface the new GIC article for a given phrasing of the question? Existing retrieval-quality behavior (the same `similarity-threshold`/`top-k` config already governs this for all six existing articles); not a new failure mode introduced by this feature.
- What happens if a customer asks a GIC question with no accounts or savings goals on file? The rate lookup and knowledge-base search do not depend on account data, so the agent can still answer; only genuinely personalized parts of a combined answer (e.g. "given your emergency fund goal...") would be omitted, same as existing tools already handle a customer with no goals.
- What happens if the MCP tool call succeeds but returns unexpected/malformed data? Out of scope for this pass - the mocked tool returns a fixed, well-formed shape; malformed-response handling is not a requirement being tested here.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST expose a `getGicRates` tool from the (renamed) MCP server, returning the current annual rate for every GIC term, as mocked data mirroring - but not sharing a dependency on - the backend's `GicTerm` enum values.
- **FR-002**: System MUST add a new knowledge-base article (`07-gics-explained.md`) to `backend/src/main/resources/knowledge-base/`, in the same format as the existing six articles, covering what a GIC is, that principal is locked for the term, guaranteed return vs. a regular savings account, and when a GIC does or doesn't make sense - without a hardcoded rate figure, so the article and the live MCP rate can never drift out of sync with each other.
- **FR-003**: System MUST register the MCP client's `ToolCallbackProvider` alongside the existing `@Tool` bean instances (`savingsChatTools`, `transferChatTool`) in `ChatbotAiConfig`'s `ChatClient` default tools, using `ChatClient.Builder.defaultTools(Object...)`'s existing support for mixing `@Tool` POJOs and a `ToolCallbackProvider` in one call.
- **FR-004**: System MUST update the chatbot's system prompt with guidance on when to call the rate tool and the knowledge-base search tool together for GIC-related questions.
- **FR-005**: System MUST move the MCP client's connection to the rates server from the opt-in `mcptest` Spring profile into the base `application.properties`, since it becomes a real capability the chatbot depends on rather than diagnostic-only scaffolding.
- **FR-006**: System MUST configure that connection so the backend does not fail to start when the rates server is unreachable at boot (e.g. `spring.ai.mcp.client.initialized=false`), deferring the actual connection attempt to first use.
- **FR-007**: System MUST NOT change `spring.ai.tools.throw-exception-on-error` from its current default (`false`), so a tool-execution failure (including an unreachable MCP server) is surfaced to the model as a message it can respond to, rather than failing the whole chat request.
- **FR-008**: System MUST track, per chat turn, the name of every tool actually invoked (in-process `@Tool` or MCP-sourced), using the same reset-per-turn/drain-once pattern as the existing `SavingsChatCitationTracker` and `PendingActionTracker`, covering all existing tool methods (`SavingsChatTools`, `TransferChatTool`) as well as the new `getGicRates` tool - not only the tools added by this feature.
- **FR-009**: System MUST persist the per-turn tool-selection list on that turn's `chat_interaction_log` row (a new `tools_used` column, added the same way the existing `sources` column was - an `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` in `ChatInteractionLogRepository.ensureSchema()` - and passed through `ChatInteractionLogRepository.log(...)` as an additional parameter), reachable from the shared audit log's existing `resourceId` pointer to that row. This MUST NOT change `AuditService.log(...)`'s existing six-parameter signature, which is shared by unrelated audit call sites across the app.
- **FR-010**: System MUST change `KnowledgeBaseIngestionRunner`'s ingestion check from "does the vector table have any rows at all" to a per-source-file check (e.g. checking existing `source` metadata/filenames already present in the vector store against the files found on disk), so newly added articles are ingested into an already-seeded environment without re-ingesting or duplicating articles already present.
- **FR-011**: System MUST rename the `mcp-test-server` module (directory, Maven artifact id, and its description text in `pom.xml`, `application.properties`, and anywhere else it is referenced) to reflect that it now hosts real, depended-upon chatbot functionality rather than purely disposable connectivity-proof scaffolding. Exact new name is an implementation-time decision; `PingTool` and its round-trip diagnostic purpose may remain alongside the new tool.
- **FR-012**: System MUST NOT introduce a separate, explicit "planning" component or a distinct plan-then-execute phase; multi-step tool selection continues to run through Spring AI's existing native tool-calling loop. This is a deliberate scope decision (see Assumptions), not an oversight.

### Contract & Error Semantics Requirements _(mandatory for API or integration changes)_

- **CER-001**: `POST /api/chat/savings-insights`'s request/response contract (`ChatQueryRequest`/`ChatQueryResponse`) is unchanged by this feature - no new fields are added to the API response; the new `tools_used` data (FR-009) is internal/traceability-only, not returned to the caller.
- **CER-002**: The MCP `getGicRates` tool's result format (the text the model receives) MUST list every GIC term and its current annual rate in a form the model can read directly, following the same plain-text style `SavingsChatTools.getAccountSummaries()` already uses for its tool results.
- **CER-003**: A failed or timed-out call to the rates MCP tool MUST surface to the model as a `RuntimeException`-derived tool error (per Spring AI's default `ToolExecutionExceptionProcessor` behavior, see FR-007), not as an unhandled exception that aborts the `ChatClient` call.

### Operational & Configuration Requirements _(mandatory for this feature)_

- **OCR-001**: The rates MCP server MUST be startable independently of the backend (its own Maven module, its own port), consistent with `mcp-test-server`'s existing standalone deployment model.
- **OCR-002**: Local run instructions (onboarding docs) MUST document that the rates MCP server needs to be started for GIC-rate questions to work, while making clear (per Story 3) that the backend itself does not require it to be running in order to start.
- **OCR-003**: The GIC rate values returned by the MCP tool MUST NOT be sourced from, or shared via code dependency with, the backend's own `GicTerm` enum - the two Maven modules remain independent, and keeping the values in sync is a manual, documented responsibility (see Edge Cases and Assumptions).

### Key Entities _(include if feature involves data)_

- **GicRateTool** (new, in the renamed MCP server module): stateless; exposes one MCP tool (`getGicRates`) returning a fixed, mocked rate table. No persistence of its own.
- **chat_interaction_log** (existing table, chatbot's Postgres datasource): gains one new column, `tools_used` (TEXT, pipe-joined tool names, same storage pattern as the existing `sources` column), added via `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` so existing deployments pick it up without a manual migration step.
- **Knowledge-base article** (new file, `07-gics-explained.md`): same shape/ingestion path as the six existing articles under `backend/src/main/resources/knowledge-base/`.
- **AuditLogEntity**: existing entity, unchanged by this feature (see FR-009) - the existing `CHATBOT_QUERY` audit action and its `resourceId` pointer into `chat_interaction_log` already give access to the new `tools_used` data without a schema change to the shared audit table.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: A GIC-rate question answered by the chatbot reflects both the MCP tool's current rate data and content from the new knowledge-base article within a single response, verified for at least the scenarios in User Story 1.
- **SC-002**: 100% of chat turns that invoke at least one tool (in-process or MCP) have a non-empty `tools_used` record on their `chat_interaction_log` row; 0% of tool-less turns (e.g. greetings) have a false/spurious entry.
- **SC-003**: The backend starts successfully with the rates MCP server stopped, in 100% of attempts.
- **SC-004**: A GIC-rate question asked while the rates MCP server is stopped returns a chat response (not a request failure) in 100% of attempts.
- **SC-005**: Restarting the application after adding a new knowledge-base article to an already-seeded vector store results in exactly one copy of each article (old and new) being present - no duplicates, nothing missing.

## Assumptions

- **No explicit "plan" artifact**: Per the earlier architecture decision on this ticket, "creates an execution plan before invoking any Tool" is satisfied by the model's own sequential tool-calling reasoning (Spring AI's existing native loop) combined with the FR-008/FR-009 tool-selection logging, not by a new, separate planning component. The ticket explicitly leaves the internal selection mechanism unspecified as a business requirement.
- **Scope of "multi-step" for this pass**: Only the GIC rate + knowledge-base combination is wired end-to-end as the concrete demonstration of Scenario 2. Other potential multi-step combinations (e.g. involving `TransferChatTool`) are not addressed by this feature.
- **Mocked rate data only**: `getGicRates` returns static/mocked values, standing in for a real external rates-publishing service; no live vendor integration is in scope.
- **Tool-selection logging lives on the chat log, not the shared audit table**: `AuditService`/`AuditLogEntity` is shared across unrelated features (freeze/unfreeze, standing orders, etc.); extending its fixed six-parameter signature to carry a tool list was considered and rejected as out of scope for this feature. The existing `resourceId`-to-`chat_interaction_log` pointer (already used for `sources`/citations) is the established pattern this reuses.
- **Backend stack**: Existing chatbot infrastructure (`ChatbotAiConfig`, `SavingsInsightChatService`, `SavingsChatCitationTracker`, `PendingActionTracker`, `ChatInteractionLogRepository`) is reused and extended as-is; no new datastore or messaging infrastructure is introduced.
