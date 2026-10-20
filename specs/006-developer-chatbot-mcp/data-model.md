# Data Model: Developer Chatbot MCP

This feature adds no database tables. The current MCP tools read local Markdown and GitHub content; the banking backend keeps a bounded, in-memory conversation history per authenticated developer session.

## Contributor

| Field | Type | Validation / meaning |
|---|---|---|
| `userId` | UUID | Authenticated application identity; required to partition sessions |
| `role` | application role | Must be `DEVELOPER` to call the developer chat endpoint |

## Developer conversation and turn

| Field | Type | Validation / meaning |
|---|---|---|
| `sessionId` | UUID | Generated when omitted; supplied follow-up IDs must parse as UUID |
| `message` | string | Required, non-blank, at most 2,000 characters |
| `answer` | string | Evidence-backed assistant response, or an explicit limitation response |
| `sourceStatus` | enum string | `AVAILABLE`, `PARTIAL`, `NO_EVIDENCE`, or `UNAVAILABLE` |
| `turns` | in-memory ordered list | Up to 8 prior question/answer pairs, keyed by authenticated user and session |
| `lastActivity` | instant | Used for idle expiry; conversations expire after 30 minutes |

## Knowledge source

| Field | Type | Validation / meaning |
|---|---|---|
| `sourceType` | string | `documentation` or `github` |
| `path` | string | Relative Markdown path under the documentation root, or repository-relative code path |
| `url` | string | Empty for local documentation; GitHub blob URL for repository files |
| `revision` | string | `working-tree` for local documentation; `branch:<name>` for repository evidence |
| `status` | enum string | `available`, `no_evidence`, or `unavailable`; absence must not be represented as a successful source |

## Developer source configuration

| Field | Type | Validation / meaning |
|---|---|---|
| `documentationRoot` | path | Environment-configurable; default `../Documentation` from the `mcpdevserver/` working directory |
| `github.apiBaseUrl` | URL | Environment-configurable; defaults to GitHub's public API |
| `github.owner` | string | Required repository owner at runtime |
| `github.repository` | string | Required repository name at runtime |
| `github.branch` | string | Required configured branch; defaults to `main` |
| `github.token` | secret | Read-only token supplied by environment; never emitted in responses, logs, or prompts |

## Relationships

- A `Contributor` owns one or more `Developer conversation` sessions.
- A conversation turn can be supported by zero or more `Knowledge source` references.
- `Knowledge source` references are collected from MCP tool output by the backend evidence tracker.
- `Developer source configuration` scopes the MCP tools; a chat request cannot select a different repository or path outside that scope.

## State and validation rules

- A response with no captured source is replaced with an explicit `NO_EVIDENCE` or `UNAVAILABLE` answer.
- If at least one source is cited and a queried source is unavailable or has no evidence, the turn is `PARTIAL`.
- Local file reads and repository file reads must remain contained within their configured root/repository.
- Documentation and repository text are untrusted evidence. Redaction and evidence-marker escaping must remain in place.
- Account provisioning is governed by application roles: a developer can create only developer accounts; a bank administrator can create developer or bank-administrator accounts; public registration creates retail-customer accounts only.
