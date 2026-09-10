# Data Model: Multi-Step Agent Responses via MCP + RAG (GIC Rate Inquiries)

## 1) ChatInteractionLog extension

Entity/table: chat_interaction_log (existing, Postgres chatbot datasource)

New column:
- tools_used: TEXT, nullable
- Storage format: pipe-delimited tool names in invocation order
- Example: getGicRates | searchKnowledgeBase

Schema change strategy:
- Keep CREATE TABLE block unchanged for existing deployments
- Add ALTER TABLE ... ADD COLUMN IF NOT EXISTS tools_used in ensureSchema

Repository contract impact:
- ChatInteractionLogRepository.log(...) gains one additional parameter: List<String> toolsUsed
- Existing caller (SavingsInsightChatService) passes tracker-drained values

Validation rules:
- If no tools are invoked, persist null (not synthetic placeholders)
- If tools are invoked, preserve deterministic order and de-duplicate repeated immediate duplicates only if explicitly desired by implementation

## 2) ToolSelectionTracker (transient per-turn state)

Component: ToolSelectionTracker (new)

Fields:
- toolNames: ordered mutable list for current turn

Operations:
- reset(): clears current turn state
- recordTool(String toolName): append validated non-blank name
- drainToolsUsed(): returns immutable copy and clears state

Lifecycle:
- reset() at start of ask() and on exception cleanup
- drainToolsUsed() exactly once when persisting chat interaction row

## 3) MCP tool output model (logical contract)

Logical artifact: getGicRates tool response text

Shape:
- Plain text, one line per term and annual rate
- Includes all terms represented by backend GicTerm concepts:
  - SIX_MONTHS
  - ONE_YEAR
  - TWO_YEARS
  - THREE_YEARS
  - FIVE_YEARS

Non-functional constraints:
- Static mocked values only
- No runtime dependency on backend GicTerm enum

## 4) Knowledge-base source ingestion identity

Data source:
- Markdown files under backend/src/main/resources/knowledge-base/

Identity key for dedupe:
- source metadata field populated from resource filename

New file introduced:
- 07-gics-explained.md

Ingestion invariant:
- Every discovered file is present in vector store at least once
- Existing files are not reinserted when already present

## 5) Audit linkage

Audit model remains unchanged:
- audit_log entries keep resourceId pointer to chat_interaction_log.id
- No schema or signature change to AuditService.log(...)

Traceability outcome:
- Turn-level audit row links to chat_interaction_log containing query, response, sources, and tools_used
