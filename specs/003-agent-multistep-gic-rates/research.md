# Research: Multi-Step Agent Responses via MCP + RAG (GIC Rate Inquiries)

## Decision 1: Combine MCP and RAG in one native tool-calling turn
- Decision: Keep Spring AI native tool-calling loop and register MCP ToolCallbackProvider together with existing tool beans in one ChatClient defaultTools call.
- Rationale: Meets FR-003 and FR-012 while avoiding a separate planner component or extra API round-trip.
- Alternatives considered:
  - Build explicit plan-then-execute orchestration component: rejected as out of scope and unnecessary for ticket acceptance.
  - Route MCP calls through custom backend endpoint first: rejected because it duplicates tool framework responsibilities.

## Decision 2: MCP server availability must not block backend startup
- Decision: Move MCP server connection into base application.properties and use lazy MCP client initialization to prevent startup coupling.
- Rationale: Required by FR-005 and FR-006; supports Story 3 degraded mode.
- Alternatives considered:
  - Keep mcptest profile-only connection: rejected because feature becomes default chatbot capability.
  - Eager initialization with retry loops at boot: rejected because startup remains coupled to sidecar availability.

## Decision 3: Tool-failure behavior remains conversational, not request-failing
- Decision: Preserve spring.ai.tools.throw-exception-on-error=false and let model receive tool-failure signal text.
- Rationale: Required by FR-007 and CER-003; allows degraded but successful chat turn responses.
- Alternatives considered:
  - Force throw-on-error=true and map to HTTP errors: rejected because it breaks current chatbot interaction model.

## Decision 4: Per-turn tool tracking uses reset and drain pattern
- Decision: Implement ToolSelectionTracker following SavingsChatCitationTracker and PendingActionTracker pattern (reset at turn start, record per invocation, drain once at persistence time).
- Rationale: Consistent with existing architecture and safe for single-turn request scope used today.
- Alternatives considered:
  - Parse tool names from model content: rejected as fragile and indirect.
  - Persist each tool call directly from tool methods: rejected due to tighter repository coupling and duplicated persistence logic.

## Decision 5: Persist tools_used on chat_interaction_log only
- Decision: Add tools_used text column to chat_interaction_log, pipe-delimited in stable invocation order.
- Rationale: Required by FR-009; reuses existing resourceId pointer from audit log without changing shared AuditService signature.
- Alternatives considered:
  - Extend audit_log schema or AuditService signature: rejected as cross-cutting and out of scope.
  - Add a separate tool_usage table: rejected as unnecessary complexity for current requirements.

## Decision 6: Incremental knowledge-base ingestion must be file-aware
- Decision: Replace table non-empty short-circuit with per-file presence check using source metadata already written into vector documents.
- Rationale: Required by FR-010; allows adding 07-gics-explained.md without re-ingesting old files.
- Alternatives considered:
  - Truncate and rebuild every startup: rejected due to cost and churn.
  - Keep current table-count gate: rejected because it blocks new article ingestion in seeded environments.

## Decision 7: MCP module rename scope
- Decision: Rename mcp-test-server to voltio-rates-mcp-server (module directory, artifactId, name/description, and config/document references).
- Rationale: Matches FR-011 and removes misleading disposable-server wording.
- Alternatives considered:
  - Keep name and only change description: rejected because module identity remains misleading.
  - Rename package namespace immediately everywhere: deferred as optional; can be staged to reduce risk.

## Decision 8: GIC rate source and term synchronization policy
- Decision: Keep static mocked rates in MCP module and document manual synchronization with backend GicTerm values.
- Rationale: Required by OCR-003 and assumption on module independence.
- Alternatives considered:
  - Share backend enum as dependency: rejected because modules must remain independent.
  - Pull rates from backend API: rejected because it defeats external-tool demonstration intent.

## Resolved Clarifications
- No unresolved NEEDS CLARIFICATION items remain for this feature.
