Implementation Plan: **RRSP** Account with **GIC** Investment Support

Branch: spec/rrsp | Date: 29 April **2026** | Spec: rrsp_spec.md Input: Feature specification from rrsp_spec.md

Summary

This feature extends the existing Account system by introducing **RRSP** as a new account type and adding **GIC** (Guaranteed Investment Certificate) as a separate investment domain.

**RRSP** remains a standard Account entity with AccountType.**RRSP**.

**GIC** is a standalone financial entity linked to **RRSP** accounts.

A customer can have only one **RRSP** account.

An **RRSP** account can have multiple **GIC** investments.

All balance changes are handled exclusively through MonetaryOperationService.

### Technical Context

Language/Version: Java 17 (Spring Boot) Architecture: Controller → Service → Repository Database: Relational (**JPA**/Hibernate) Security: **JWT**-based authentication Transactions: @Transactional service layer Integration: Existing MonetaryOperationService Testing: JUnit, Mockito Build Tool: Maven

Key principle:

Account system remains unchanged **GIC** is an independent domain module MonetaryOperationService is the single source of truth for balance movement ### Constitution Check No business logic duplication in AccountService and GICService No direct balance updates outside MonetaryOperationService No embedding of **GIC** fields inside Account entity All financial operations must be auditable Strict separation of Account and Investment domains ### System Design Overview ## RRSP as Account Type

**RRSP** is implemented by extending:

AccountType enum:

**CHECKING** **SAVINGS** **RRSP**

No new account entity is introduced.

## GIC as Independent Entity

**GIC** is a standalone domain entity:

Each **GIC**:

Belongs to exactly one **RRSP** account Has independent lifecycle Is persisted separately from Account

Relationship:

Account (**RRSP**) → 0..N GICs **GIC** → 1 **RRSP** Account ## Monetary Flow Strategy

All balance changes go through:

MonetaryOperationService:

withdraw → used for **GIC** creation deposit → used for **GIC** maturity payout transfer → unrelated to **RRSP**/**GIC**

No direct account balance mutation inside **GIC** logic.

### Project Structure

Documentation

rrsp_spec.md rrsp_plan.md rrsp_tasks.md

### Backend Structure

backend/src/main/java/com/group1/banking/

Core additions:

entity/**GIC**.java repository/GICRepository.java service/GICService.java controller/GICController.java

Existing reused:

AccountService AccountController MonetaryOperationService ### Data Model Account Structure (source of truth) accountId customer (ManyToOne → Customer) accountType (**CHECKING**, **SAVINGS**, **RRSP**) status (**ACTIVE**, **CLOSED**, etc.) balance (BigDecimal, precision 19, scale 2) interestRate (nullable, precision 12, scale 4) accountNumber (system generated unique identifier) dailyTransferLimit (default **3000**.00 if not set) deletedAt (soft delete support) closedAt (account closure tracking) version (optimistic locking) createdAt (audit) updatedAt (audit)

**RRSP** is simply: AccountType = **RRSP**

**GIC** (new entity) gicId rrspAccountId (FK → Account.accountId) principalAmount interestRate term startDate maturityDate maturityAmount status (**ACTIVE**, **MATURED**) ### Business Rules **RRSP** Rules A customer can have only one active **RRSP** account **RRSP** must pass **KYC** validation **RRSP** uses existing Account lifecycle **GIC** Rules **RRSP** can have multiple **GIC** investments Each **GIC** belongs to exactly one **RRSP** account Each **GIC** has independent lifecycle Sufficient **RRSP** balance required before creation Interest rate is derived from term (not user-controlled long term) Funds are locked at creation time No early redemption allowed (default rule) ### Money Movement Rules

**GIC** Creation:

Withdraw amount from **RRSP** via MonetaryOperationService Create **GIC** record

**GIC** Maturity:

Calculate interest Deposit principal + interest back to **RRSP** ### Service Layer Design AccountService (unchanged responsibility) **RRSP** creation and validation Customer ownership enforcement **KYC** and eligibility checks Ensures only one **RRSP** per customer

No **GIC** logic inside AccountService

GICService (new)

Responsibilities:

Create **GIC** linked to **RRSP** Validate **RRSP** account and balance Ensure **RRSP** ownership Manage **GIC** lifecycle (**ACTIVE** → **MATURED**) Call MonetaryOperationService for fund movement Compute maturity schedule and interest MonetaryOperationService (unchanged)

Responsibilities:

deposit withdraw transfer idempotency handling transaction logging

No **RRSP**/**GIC** awareness

**API** Design Existing Account APIs (reused) **POST** /customers/{customerId}/accounts **GET** /accounts/{accountId} **GET** /customers/{customerId}/accounts

**RRSP** is filtered using:

accountType = **RRSP** New **GIC** APIs ## Create GIC

**POST** /accounts/{accountId}/gics

Request:

amount term

Rules:

account must be **RRSP** sufficient balance required multiple GICs allowed ## Get all GICs for RRSP

**GET** /accounts/{accountId}/gics

Returns list of all GICs for **RRSP** account

## Get GIC by ID

**GET** /gics/{gicId}

## Maturity processing (system/internal)

Triggered via scheduler or batch job

### Error Handling

**422** → **KYC** or eligibility failure **409** → RRSP_ALREADY_EXISTS **400** → insufficient funds / invalid term **404** → account or **GIC** not found **403** → unauthorized access **503** → system failure Non-Functional Requirements 95% requests under 2 seconds Fully transactional operations Idempotent monetary operations via existing system Audit logs for all financial events Scalable to multiple concurrent GICs per **RRSP** Strict **RBAC** enforcement ### Key Architectural Decisions ## RRSP is NOT a separate entity

Only an AccountType extension

## GIC is fully decoupled from Account

Prevents coupling of investment logic with account lifecycle

## RRSP → GIC is 1:N relationship

Supports multiple concurrent investments per **RRSP**

## MonetaryOperationService is the ONLY money engine

Ensures:

consistency auditability reuse of existing logic ## No embedded investment fields in Account

Keeps Account model clean and stable for future **RRIF** expansion

### Testing Strategy

**RRSP** creation (single per customer) **GIC** creation (multiple allowed) Insufficient balance scenarios Maturity payout correctness Authorization rules (admin vs customer) Idempotency behavior during **GIC** creation ### Next Steps Implement **GIC** entity + repository Create GICService (core business logic) Add **RRSP** validation in AccountService Integrate MonetaryOperationService for: withdraw (**GIC** creation) deposit (**GIC** maturity) Add GICController Implement scheduled job for maturity processing Write integration tests: **RRSP** → multiple **GIC** flows concurrency scenarios balance consistency validation
