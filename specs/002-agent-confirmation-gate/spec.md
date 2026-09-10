# Feature Specification: Agent Confirmation Gate for Financial Actions

**Feature Branch**: `[to-be-set-by-git-hook]`

**Created**: August 26, 2026

**Status**: Draft

**Input**: User description: "AC4 from the MCP Agent ticket - the Agent must never autonomously complete financial or account-changing actions without confirmation. Prove this out end-to-end using a real mutating action (money transfer between the customer's own accounts) called from the Savings Insight chatbot's agentic tool-calling loop, with a generic mechanism other mutating tools (including future MCP-sourced ones) can reuse."

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Agent proposes a transfer instead of executing it (Priority: P1)

A customer asks the Savings Insight chatbot to move money between two of their own accounts (e.g. "transfer $200 from my checking to my savings"). The agent understands the request but does not move any money. Instead it replies with a plain-language summary of what it would do and a confirmation the customer must act on before anything happens.

**Why this priority**: This is the core safety property the ticket asks for (AC4). Without it, nothing else in this feature has a reason to exist.

**Independent Test**: Send a transfer request to `/api/chat/savings-insights` and verify no transaction is created on either account, the response is HTTP 200, and the response body contains a pending confirmation with a token, a human-readable summary, and an expiry.

**Acceptance Scenarios**:

1. **Given** a customer with a Checking and a Savings account, **When** they ask the chatbot to transfer a valid amount between those accounts, **Then** the agent's reply describes the proposed transfer and the response includes a `pending_confirmation` object; no balance changes on either account.
2. **Given** the same request, **When** the proposal is created, **Then** a `PendingAgentAction` row is persisted with status `PENDING`, the correct customer id, action type `TRANSFER`, the transfer parameters, and an expiry roughly 5 minutes out.
3. **Given** a customer asks the chatbot to transfer money to or from an account that is not theirs, **When** the agent evaluates the request, **Then** no `PendingAgentAction` is created and the customer is told the account isn't available to them.

---

### User Story 2 - Customer confirms a proposed transfer (Priority: P1)

Having seen the proposal, the customer takes an explicit confirming action (a Confirm button in the chat UI) and the transfer actually executes, using the same account-transfer logic the rest of the app already uses.

**Why this priority**: A proposal that can never be carried out isn't useful; this closes the loop and is equally core to demonstrating AC4 (confirm-then-execute, not just refuse).

**Independent Test**: Propose a transfer, capture the returned token, call `POST /api/chat/confirmations/{token}` as the same customer, and verify both accounts' balances change correctly and the pending action's status becomes final (e.g. `EXECUTED`).

**Acceptance Scenarios**:

1. **Given** a valid, unexpired, unconsumed confirmation token belonging to the calling customer, **When** they call the confirmation endpoint, **Then** the transfer executes via the existing `MonetaryOperationService.transfer` path, both accounts reflect the new balances, and the endpoint returns the same shape the existing `/api/accounts/transfer` success response returns.
2. **Given** a transfer has been confirmed and executed, **When** the same token is submitted again, **Then** the second call is rejected (HTTP 409) and no second transfer occurs.
3. **Given** the confirmation endpoint is called, **When** it succeeds or fails, **Then** the same confirmation token is passed through as the `Idempotency-Key` to `MonetaryOperationService.transfer`, so a duplicate network retry of the confirm click cannot double-execute.

---

### User Story 3 - Proposal is never acted on (Priority: P2)

A customer asks for a transfer, sees the proposal, and simply never confirms it - they close the chat, ask something else, or come back later. Nothing about their accounts changes, and the option to confirm eventually goes away.

**Why this priority**: Necessary for correctness and for keeping stale pending actions from lingering indefinitely, but the system already behaves safely by default while a proposal sits unconfirmed - lower urgency than Stories 1-2.

**Independent Test**: Propose a transfer, wait past the expiry window (or seed a `PendingAgentAction` with an already-past `expiresAt`), then attempt to confirm and verify it is rejected as expired and no transfer occurs.

**Acceptance Scenarios**:

1. **Given** a `PendingAgentAction` whose `expiresAt` has passed, **When** the customer attempts to confirm it, **Then** the endpoint returns HTTP 410 and the row's status becomes `EXPIRED`; no transfer occurs.
2. **Given** an unconfirmed, unexpired proposal, **When** the customer asks the agent an unrelated question, **Then** the prior proposal remains pending and unaffected (asking something else does not implicitly confirm or cancel it).

---

### User Story 4 - Invalid or mismatched confirmation attempts are rejected (Priority: P2)

Someone attempts to confirm a token that isn't theirs, doesn't exist, or was already used - whether by mistake or by trying to probe the endpoint.

**Why this priority**: Security/correctness backstop around Stories 1-2, not a new user-facing capability.

**Independent Test**: Attempt to confirm a token as a different authenticated customer than the one it was issued to, and verify it is rejected without revealing that the token belongs to someone else.

**Acceptance Scenarios**:

1. **Given** a token issued to customer A, **When** customer B (authenticated) attempts to confirm it, **Then** the endpoint returns HTTP 404 (not 403 - the response must not confirm the token exists for another customer), and no transfer occurs.
2. **Given** a token that does not exist, **When** anyone attempts to confirm it, **Then** the endpoint returns HTTP 404.
3. **Given** a token already in status `EXECUTED` or `EXPIRED`, **When** it is submitted again, **Then** the endpoint returns HTTP 409 and no transfer occurs.

---

### User Story 5 - Every proposal and confirmation decision is audited (Priority: P1)

Per AC5/CFG-03, every step of this flow - not just the final executed transfer - leaves a trail in the shared audit log: that a transfer was proposed, and separately, whether it was confirmed, denied, or left to expire.

**Why this priority**: Directly required by the ticket (AC5) and is what makes AC4 verifiable after the fact rather than just trusted.

**Independent Test**: Propose then confirm a transfer, then query the audit log for that customer and verify two distinct entries exist (proposed, confirmed) referencing the same resource.

**Acceptance Scenarios**:

1. **Given** a transfer proposal is created, **When** `ConfirmationGateService.propose` completes, **Then** an audit entry is written with action `AGENT_ACTION_PROPOSED`, the customer as actor, and `resourceId` set to the confirmation token.
2. **Given** a confirmation attempt (successful or not), **When** it completes, **Then** an audit entry is written with action `AGENT_ACTION_CONFIRMED` or `AGENT_ACTION_DENIED` (with the denial reason as outcome), `resourceId` set to the same token.
3. **Given** writing an audit entry fails for any reason, **When** that happens during either step, **Then** the failure is logged but does not prevent the proposal or the transfer from completing - consistent with how audit failures are already handled in `SavingsInsightChatService`.

---

### Edge Cases

- What happens if the customer asks for a transfer with an invalid amount (zero, negative, more decimal places than currency allows)? The `proposeTransfer` tool validates this itself before creating any pending action and the agent explains why it can't propose the transfer.
- What happens if the customer's account balance changes between proposing and confirming (e.g. another transfer or withdrawal already spent the funds)? The confirm endpoint does not re-validate balance itself; it delegates to the existing `MonetaryOperationService.transfer`, which already enforces this, and its failure is surfaced as the confirm endpoint's response rather than duplicated business logic.
- What happens if the customer sends multiple transfer requests to the agent before confirming the first? Each produces its own independent `PendingAgentAction` and token; confirming one does not affect the others, and a stale one can still expire or be confirmed independently, up to whatever token it names.
- What happens if the frontend never received or lost the `pending_confirmation` payload (e.g. a page refresh)? The proposal still exists server-side until it expires; there is no requirement in this feature to let a customer look up or resurface a lost token by browsing - a new request to the agent is the supported path.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST expose a `proposeTransfer` tool to the Savings Insight chatbot's agentic tool-calling loop that validates a transfer request and creates a pending, unexecuted proposal rather than moving funds.
- **FR-002**: System MUST NOT give the chatbot's tool-calling loop any tool capable of directly executing a transfer, freeze/unfreeze, standing order change, or other account-changing action - only proposing one.
- **FR-003**: System MUST persist each proposal as a `PendingAgentAction` with a unique, unguessable token, the owning customer id, action type, the action's parameters, a human-readable summary, a status, and an expiry (~5 minutes from creation).
- **FR-004**: System MUST expose a `POST /api/chat/confirmations/{token}` endpoint, authenticated, that executes the underlying action only when the token is valid, unexpired, unconsumed, and owned by the calling customer.
- **FR-005**: System MUST reuse the confirmation token as the idempotency key passed to the underlying mutating service call, so a retried confirm request cannot execute the action twice.
- **FR-006**: System MUST mark a `PendingAgentAction` with a terminal status (`EXECUTED`, `EXPIRED`, or `DENIED`) once it has been acted on or has expired, and MUST reject any further confirmation attempt against a token already in a terminal status.
- **FR-007**: System MUST build the proposal/confirm mechanism (`ConfirmationGateService` and the `PendingAgentAction` model) as a reusable component that a future mutating tool - chat-native or MCP-sourced - can use without duplicating this logic; `TRANSFER` is the first and only action type implemented now.
- **FR-008**: System MUST surface the proposal to the chatbot frontend as structured data (a `pending_confirmation` field on the chat response), not solely as free text, so the UI can render an explicit Confirm/Dismiss control rather than parsing the agent's reply.
- **FR-009**: System MUST record an audit entry (via the existing `AuditService`) both when a proposal is created and when a confirmation attempt is resolved (confirmed or denied), per AC5/CFG-03.
- **FR-010**: System MUST NOT accept a free-text chat reply (e.g. "yes") as confirmation of a financial action; confirmation MUST go through the dedicated endpoint.

### Contract & Error Semantics Requirements _(mandatory for API or integration changes)_

- **CER-001**: `ChatQueryResponse` (`/api/chat/savings-insights`) gains an optional `pending_confirmation` field: `{token, action_type, summary, expires_at}`, present only on a turn where a proposal was created; `null`/absent otherwise. Existing fields (`response`, `based_on`, `limited_data`, `blocked`) are unchanged.
- **CER-002**: `POST /api/chat/confirmations/{token}` request contract: no request body required. Response contract on success (HTTP 200): the same shape `POST /api/accounts/transfer` already returns on success. Error responses: 404 (token not found, or belongs to a different customer), 409 (already confirmed/executed), 410 (expired). Each case MUST map to a stable, distinguishable error code the frontend can branch on.
- **CER-003**: Frontend error handling for the confirmation call MUST be added to the existing centralized mapping in `src/api/axiosClient.js` / `mapAxiosError`, not parsed ad hoc inside `ChatWidget.jsx`.
- **CER-004**: The confirmation endpoint MUST be idempotent from the caller's perspective for the success case: confirming the same token twice must never execute two transfers, and MUST return a clear, distinct error on the second attempt rather than silently succeeding again.

### Security & Configuration Requirements _(mandatory for endpoint/proxy/auth changes)_

- **SCR-001**: `POST /api/chat/confirmations/{token}` MUST require authentication and MUST verify the resolved customer id matches the `PendingAgentAction`'s owning customer id before doing anything else.
- **SCR-002**: A token belonging to a different customer MUST be indistinguishable, from the response, from a token that does not exist at all (HTTP 404 in both cases) - the endpoint MUST NOT leak whether a given token is valid for someone else.
- **SCR-003**: The `proposeTransfer` tool MUST verify both the source and destination accounts belong to the requesting customer (reusing existing `AccountService` ownership checks) before creating a pending action - a customer must not be able to get the agent to propose moving money into or out of an account they don't own.
- **SCR-004**: The confirmation token MUST be generated with enough entropy that it cannot practically be guessed (e.g. a random UUID), since it is the sole credential the confirm endpoint checks beyond the caller's own authentication.
- **SCR-005**: `PendingAgentAction` parameters MUST NOT be trusted as re-validated at confirm time beyond what `MonetaryOperationService.transfer` itself already enforces (e.g. current balance) - the confirm step executes what was proposed, it does not re-derive it from fresh user input.

### Key Entities _(include if feature involves data)_

- **PendingAgentAction**: A proposed-but-not-yet-executed action originating from the agent. Attributes: `id`, `token` (unique, unguessable), `customerId`, `actionType` (`TRANSFER` for now), `parametersJson` (validated action parameters), `humanSummary` (text shown to the customer), `status` (`PENDING` / `EXECUTED` / `EXPIRED` / `DENIED`), `createdAt`, `expiresAt`. Persisted on the primary datasource (same store as `StandingOrderEntity`, `AccountControlAuditEvent`), not the chatbot's separate Postgres/pgvector datasource.
- **Account / Customer / Transaction**: Existing entities, unchanged. The confirm step's actual execution reuses `MonetaryOperationService.transfer` and its existing `TransferRequest`/`TransferResponse` contract as-is.
- **AuditLogEntity**: Existing entity (CFG-03). This feature adds two new action codes (`AGENT_ACTION_PROPOSED`, `AGENT_ACTION_CONFIRMED`/`AGENT_ACTION_DENIED`) to the existing shared audit trail; no schema change.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: 100% of chatbot-initiated transfer requests result in a `PendingAgentAction` and zero balance change at the moment of proposal - no code path exists by which the chatbot's tool-calling loop can move funds without a separate confirm call.
- **SC-002**: 100% of confirmation attempts against an expired, already-consumed, or not-owned token are rejected without executing a transfer.
- **SC-003**: Every proposal and every confirmation resolution (success or denial) produces exactly one corresponding audit log entry - no missing entries, no duplicates, verified for at least the scenarios in User Story 5.
- **SC-004**: A confirmed, valid transfer executes and is reflected in both accounts' balances within the same request/response cycle as the confirm call (no polling required).
- **SC-005**: Retrying the same confirm request (e.g. due to a network retry on the frontend) never results in more than one executed transfer for that token.

## Assumptions

- **Scope of "financial/account-changing action" for this pass**: Only money transfer between the customer's own accounts is wired end-to-end. Standing orders and account freeze/unfreeze are explicitly out of scope for this pass but are the intended next users of the same `ConfirmationGateService`/`PendingAgentAction` mechanism (see FR-007).
- **Confirmation channel**: Confirmation is a structured, explicit action (a button calling a dedicated endpoint), not a free-text chat reply, per the earlier design decision - this trades a small frontend contract change for not relying on parsing intent out of natural language for a money-moving action.
- **Expiry window**: ~5 minutes is treated as a reasonable default for this pass; no requirement here to make it configurable.
- **No new frontend confirmation surface beyond the chat widget**: The Confirm/Dismiss control lives inside the existing `ChatWidget`, rendered as part of the assistant's message when `pending_confirmation` is present - no separate page or notification center is introduced.
- **Backend stack**: Existing authentication (`JwtAuthenticationFilter`/`CustomUserPrincipal`) and the existing `AuditService` (CFG-03) are reused as-is; no new security or logging infrastructure is introduced.
