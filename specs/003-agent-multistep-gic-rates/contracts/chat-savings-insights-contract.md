# Contract: POST /api/chat/savings-insights (Unchanged Public API)

## Scope

This feature keeps the public chat request/response contract unchanged.

## Request

- Existing ChatQueryRequest shape unchanged.
- No new request fields added for MCP or multi-step orchestration.

## Response

- Existing ChatQueryResponse shape unchanged.
- No tools_used field returned to API clients.
- Multi-step behavior is observable only via response content quality and internal logs.

## Behavioral Contract Additions

- A single response may now combine:
  - live MCP tool output (GIC rates)
  - RAG-derived educational content
- If MCP lookup fails, response still returns conversationally with degraded explanation.

## Non-Goals

- No new endpoint.
- No explicit plan object in API response.
