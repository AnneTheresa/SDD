
# Tasks: RRSP Account with GIC Investment Support

**Input**: `rrsp_spec.md`, `rrsp_plan.md`
**Feature**: RRSP account creation (via existing account API) + GIC investment sub-resource
**Tech Stack**: Java 17, Spring Boot, JPA/Hibernate, H2, JWT, Maven
**Source Root**: `backend/src/main/java/com/group1/banking/`

---

## User Stories

| ID  | Story                                        | Priority |
|-----|----------------------------------------------|----------|
| US1 | Open an RRSP account (existing account API)  | P1       |
| US2 | Create a GIC investment within RRSP          | P2       |
| US3 | Redeem / mature a GIC and credit RRSP        | P3       |
| US4 | Close an RRSP account                        | P4       |

---

## Phase 1: Setup

> Project initialization and enum/type extensions required by all user stories.

- [ ] T001 Add `RRSP` value to `AccountType` enum in `backend/src/main/java/com/group1/banking/entity/AccountType.java`
- [ ] T002 Create `GICStatus` enum (`ACTIVE`, `REDEEMED`) in `backend/src/main/java/com/group1/banking/entity/GICStatus.java`
- [ ] T003 Create `GicTerm` enum (`SIX_MONTHS`, `ONE_YEAR`, `TWO_YEARS`, `THREE_YEARS`, `FIVE_YEARS`) with `getInterestRate()` lookup in `backend/src/main/java/com/group1/banking/entity/GicTerm.java`

---

## Phase 2: Foundational

> Shared infrastructure blocking all user story phases.

- [ ] T004 Create `GicInvestment` JPA entity (fields: `gicId`, `account` FK, `principalAmount`, `interestRate`, `term`, `startDate`, `maturityDate`, `maturityAmount`, `status`, `redeemable`) in `backend/src/main/java/com/group1/banking/entity/GicInvestment.java`
- [ ] T005 [P] Create `GicRepository` extending `JpaRepository<GicInvestment, String>` with `findByAccount_AccountIdAndStatus` method in `backend/src/main/java/com/group1/banking/repository/GicRepository.java`
- [ ] T006 [P] Add `findByAccountTypeAndCustomer_CustomerIdAndDeletedAtIsNull` query method to `AccountRepository` in `backend/src/main/java/com/group1/banking/repository/AccountRepository.java`
- [ ] T007 [P] Create GIC request DTO `CreateGicRequest` (fields: `amount`, `term`) in `backend/src/main/java/com/group1/banking/dto/gic/CreateGicRequest.java`
- [ ] T008 [P] Create GIC response DTO `GicResponse` (fields: `gicId`, `amount`, `interestRate`, `term`, `startDate`, `maturityDate`, `maturityAmount`, `status`) in `backend/src/main/java/com/group1/banking/dto/gic/GicResponse.java`

---

## Phase 3: US1 — Open RRSP Account

> Goal: Authenticated customer can open exactly one RRSP account via `POST /customers/{customerId}/accounts`.
> Independent test: `POST /customers/{customerId}/accounts` with `accountType=RRSP` returns 201, and a second request returns 409.

- [ ] T009 [US1] Add RRSP validation branch in `AccountService.validateCreateRequest` — enforce one RRSP per customer (query AccountRepository for existing RRSP) and KYC verified check; throw `ConflictException("RRSP_ALREADY_EXISTS")` or `UnprocessableException("KYC_REQUIRED")` in `backend/src/main/java/com/group1/banking/service/impl/AccountService.java`
- [ ] T010 [P] [US1] Add RRSP null interest-rate rule in `AccountService.validateCreateRequest` (RRSP does not require interestRate; reject if provided) in `backend/src/main/java/com/group1/banking/service/impl/AccountService.java`
- [ ] T011 [US1] Verify `AccountController` `POST /customers/{customerId}/accounts` passes `accountType=RRSP` through to service without changes (confirm no type whitelist filtering) in `backend/src/main/java/com/group1/banking/controller/AccountController.java`

---

## Phase 4: US2 — Create & Get GIC Investment

> Goal: Customer can create one GIC inside an active RRSP account with sufficient balance; interest rate is derived from term.
> Independent test: `POST /accounts/{accountId}/gic` returns 201 with computed `interestRate` and `maturityDate`; second call returns 409.

- [ ] T012 [US2] Create `GicService` with `createGic(Long accountId, CreateGicRequest)` — validate account is RRSP + ACTIVE, no existing active GIC, sufficient balance, derive interest rate from `GicTerm`, deduct balance, compute `maturityDate` and `maturityAmount`, persist in `backend/src/main/java/com/group1/banking/service/impl/GicService.java`
- [ ] T013 [P] [US2] Implement `getGic(Long accountId)` on `GicService` — return active GIC for the account or throw `NotFoundException("GIC_NOT_FOUND")` in `backend/src/main/java/com/group1/banking/service/impl/GicService.java`
- [ ] T014 [US2] Create `GicController` with `POST /accounts/{accountId}/gic` (→ `createGic`) and `GET /accounts/{accountId}/gic` (→ `getGic`), secured with JWT auth in `backend/src/main/java/com/group1/banking/controller/GicController.java`

---

## Phase 5: US3 — Redeem GIC

> Goal: Customer can redeem an active GIC; principal + interest credited back to RRSP balance; GIC status set to REDEEMED.
> Independent test: `POST /accounts/{accountId}/gic/redeem` returns 200 with correct `payoutAmount`; subsequent call returns 400.

- [ ] T015 [US3] Implement `redeemGic(Long accountId)` on `GicService` — load active GIC, compute payout (`principal + interestAmount`), credit amount back to Account balance, set `GICStatus.REDEEMED` in `backend/src/main/java/com/group1/banking/service/impl/GicService.java`
- [ ] T016 [US3] Add `POST /accounts/{accountId}/gic/redeem` endpoint to `GicController` returning `RedeemGicResponse` (fields: `message`, `payoutAmount`) in `backend/src/main/java/com/group1/banking/controller/GicController.java`
- [ ] T017 [P] [US3] Create `RedeemGicResponse` DTO in `backend/src/main/java/com/group1/banking/dto/gic/RedeemGicResponse.java`

---

## Phase 6: US4 — Close RRSP Account

> Goal: Customer or admin can close an RRSP account only when no active GIC exists and balance is zero.
> Independent test: `POST /accounts/{accountId}/close` with zero balance and no active GIC returns 200; with active GIC returns 400.

- [ ] T018 [US4] Add `closeRrspAccount(Long accountId)` method to `AccountService` — validate account is RRSP + ACTIVE, no active GIC (query GicRepository), balance is zero; set `AccountStatus.CLOSED` and `deletedAt` in `backend/src/main/java/com/group1/banking/service/impl/AccountService.java`
- [ ] T019 [US4] Add `POST /accounts/{accountId}/close` endpoint to `AccountController` calling `closeRrspAccount` in `backend/src/main/java/com/group1/banking/controller/AccountController.java`

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T020 [P] Audit-log all GIC lifecycle events (create, redeem) via existing `AuditLogEntity` / logging infrastructure in `backend/src/main/java/com/group1/banking/service/impl/GicService.java`
- [ ] T021 [P] Ensure `AccountResponse` includes a `gic` field (nullable `GicResponse`) when `accountType=RRSP` and an active GIC exists in `backend/src/main/java/com/group1/banking/dto/customer/AccountResponse.java`
- [ ] T022 [P] Register all new exception error codes (`RRSP_ALREADY_EXISTS`, `GIC_ALREADY_EXISTS`, `INSUFFICIENT_FUNDS`, `INVALID_TERM`, `ACTIVE_GIC_EXISTS`, `INVALID_ACCOUNT_TYPE_FOR_GIC`) in the global exception handler in `backend/src/main/java/com/group1/banking/exception/`
- [ ] T023 Verify security config (`SecurityFilterChain`) permits `/accounts/*/gic` and `/accounts/*/close` endpoints with JWT authentication required in `backend/src/main/java/com/group1/banking/security/`

---

## Dependency Graph

```
T001, T002, T003
    └── T004 (GicInvestment entity)
            ├── T005 (GicRepository)
            ├── T006 (AccountRepository query)
            └── T007, T008 (DTOs)
                    ├── Phase 3 (US1: T009–T011)  — no inter-story deps
                    ├── Phase 4 (US2: T012–T014)  — depends on T005, T007, T008
                    ├── Phase 5 (US3: T015–T017)  — depends on T012
                    └── Phase 6 (US4: T018–T019)  — depends on T005
```

## Parallel Execution Opportunities

| Story | Parallelizable tasks (same story, different files) |
|-------|----------------------------------------------------|
| Setup | T002, T003 (independent enums)                     |
| Found | T005, T006, T007, T008 (after T004 is done)        |
| US1   | T010 (after T009)                                  |
| US2   | T013 (after T012); T014 after T012+T013            |
| US3   | T017 (DTO only, no dep)                            |
| US4   | T019 (after T018)                                  |
| Polish| T020, T021, T022 all independent of each other     |

## Implementation Strategy

**MVP scope** (US1 only): Complete T001–T003, T006, T009–T011. This delivers a working RRSP account that can be created via the existing account API with full validation.

**Increment 2** (US2 + US3): T004, T005, T007, T008, T012–T017. Delivers GIC create, view, and redeem flows.

**Increment 3** (US4 + Polish): T018–T023. Delivers account close and cross-cutting quality concerns.

---

**Total tasks**: 23
**Per user story**: US1 = 3, US2 = 3, US3 = 3, US4 = 2, Setup/Foundation = 8, Polish = 4
**Parallel opportunities**: 12 tasks marked `[P]`
- [ ] FT602 Write unit tests for the fetch function and UI component
