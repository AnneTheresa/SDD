# Contract: chat_interaction_log.tools_used

## Scope

Internal persistence contract for tool-selection traceability.

## Schema

Table: chat_interaction_log

New column:
- tools_used TEXT NULL

Migration strategy:
- ALTER TABLE chat_interaction_log ADD COLUMN IF NOT EXISTS tools_used TEXT

## Write Contract

Repository method:
- ChatInteractionLogRepository.log(..., List<String> toolsUsed)

Serialization:
- Null when no tools invoked.
- Pipe-delimited string when tools invoked.
- Order reflects actual invocation sequence in the turn.

Examples:
- getGicRates | searchKnowledgeBase
- getAccountSummaries | getGicRates | searchKnowledgeBase

## Read/Traceability Contract

- audit_log.resource_id keeps pointing to chat_interaction_log.id.
- Consumers can join to inspect tools_used for Scenario 5 verification.

## Backward Compatibility

- Existing rows without tools_used remain valid.
- Existing callers requiring old log signature must be updated in same change set.
