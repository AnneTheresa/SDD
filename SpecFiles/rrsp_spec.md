Feature Specification: **RRSP** Account with **GIC** Investment Support

Feature Branch: [spec/rrsp-gic] Created: April 29, **2026** Status: Draft Input: "As a customer, I want to open an **RRSP** account and invest in GICs so that I can grow my retirement savings securely."

1.1 Feature Overview

This feature enables customers to open a Registered Retirement Savings Plan (**RRSP**) account as a specialized account type within the existing Account system and invest funds into Guaranteed Investment Certificates (GICs).

**RRSP** is implemented as an extension of the existing Account model, and **GIC** investments are separate financial instruments linked to **RRSP** accounts.

Who is this for?

Individual customers who want a secure and predictable way to save for retirement

What outcome does it enable?

Customers can:

Create an **RRSP** account using existing account APIs Deposit funds into the **RRSP** account Invest part of the balance into **GIC** products Earn fixed returns over a defined term Why does the system need this behavior? Support retirement savings use cases within existing account system Enable structured, low-risk investment products Avoid duplicating account infrastructure Maintain a clean extensible banking model 1.2 Scope and Non-Goals In Scope: Creating **RRSP** accounts using existing /accounts **API** Enforcing **RRSP**-specific validation rules Creating **GIC** investments linked to **RRSP** accounts Deducting and restoring balance through **GIC** lifecycle Tracking **GIC** maturity lifecycle (**ACTIVE** → **MATURED**) Viewing **RRSP** accounts with associated **GIC** investments Explicitly Excluded: Separate **RRSP** entity or table Separate **RRSP** service/controller Tax calculations or contribution limits Withdrawal rules or penalties **RRIF** conversion External investment products (stocks, mutual funds) 1.3 Actors and Preconditions Actors: **CUSTOMER** — operates only on owned accounts **ADMIN** — has full access across customers Preconditions: User is authenticated via **JWT** **CUSTOMER** must own the account being accessed Customer must have verified **KYC** (for **RRSP** creation) Customer must not already have an active **RRSP** account Account must exist before **GIC** creation 1.4 Core Behavior and Flows Happy Path — **RRSP** Account Creation

Customer uses existing endpoint:

**POST** /customers/{customerId}/accounts

Request:

{
    *accountType*: *RRSP*,
    *balance*: **10000**
}

Flow:

System validates authentication and ownership System verifies **KYC** status System checks if **RRSP** already exists System creates account with type **RRSP** Account status = **ACTIVE** Account becomes visible in account list Happy Path — **GIC** Creation

New endpoint:

**POST** /accounts/{accountId}/gic

Request:

{
    *principal*: **5000**,
    *termYears*: 3,
    *interestRate*: 4.0
}

Flow:

System validates account exists System checks account type = **RRSP** System validates sufficient balance System deducts principal from account balance System creates GICInvestment linked via accountId System calculates maturity date and maturity amount System sets status = **ACTIVE** ### Maturity Flow System identifies GICs with maturity date reached System updates status: **ACTIVE** → **MATURED** System credits principal + interest back to **RRSP** account balance Decision Points: **KYC** must be verified for **RRSP** creation Only one **RRSP** account per customer **GIC** can only be created for **RRSP** accounts Sufficient balance required for **GIC** creation State Transitions:

**RRSP** Account:

**CREATED** → **ACTIVE**

**GIC** Investment:

**ACTIVE** → **MATURED** 1.5 Business Rules **RRSP** Account Rules: **RRSP** is a valid AccountType Only one active **RRSP** per customer Must pass **KYC** validation Managed using existing Account entity **GIC** Rules: **GIC** is a separate entity linked via accountId Only **RRSP** accounts can hold GICs Only one **ACTIVE** **GIC** per **RRSP** account at a time Principal amount must be deducted from account balance Funds are locked until maturity Non-redeemable before maturity Interest Rules: Interest rate is fixed for term duration Maturity amount = principal + interest Interest depends on selected term Maturity Rules: On maturity: **GIC** marked as **MATURED** Funds returned to **RRSP** account balance 1.6 Data Model (Logical View) Account (existing entity) Account ID Customer ID Account Type (**CHECKING**, **SAVINGS**, **TFSA**, **RRSP**) Balance Status (**ACTIVE**, **CLOSED**) **GIC** Investment (new entity) **GIC** ID Account ID (FK → Account) ### Principal Amount ### Interest Rate Term (years) ### Start Date ### Maturity Date ### Maturity Amount Status (**ACTIVE**, **MATURED**) 1.7 Account Type Extension

Existing Account Types:

**CHECKING** **SAVINGS** **TFSA**

New Account Type:

**RRSP**

System must treat **RRSP** as a valid account type within existing account APIs.

1.8 Error Conditions and Edge Cases **422** — **KYC** not verified **409** — RRSP_ALREADY_EXISTS **400** — accountType not supported **404** — customer not found **401** — unauthorized access **400** — insufficientFunds for **GIC** **400** — INVALID_ACCOUNT_TYPE_FOR_GIC **409** — ACTIVE_GIC_ALREADY_EXISTS **503** — system unavailable 1.9 Non-Functional Requirements 95% of operations complete within 2 seconds All operations must be authenticated All financial operations must be auditable High precision decimal handling required System must maintain 99.5% availability **RBAC** must be enforced across all endpoints 1.10 Acceptance Criteria Positive Scenario 1 — **RRSP** Account Creation

Given a verified **CUSTOMER** or **ADMIN** with no existing **RRSP** account When a request is made via /accounts with accountType = **RRSP** Then system creates an **ACTIVE** **RRSP** account

Positive Scenario 2 — **GIC** Creation

Given an **ACTIVE** **RRSP** account with sufficient balance When a valid **GIC** request is submitted Then system:

Deducts balance Creates **GIC** linked to account Sets status to **ACTIVE** Positive Scenario 3 — **GIC** Maturity

Given a **GIC** has reached maturity date When system processes maturity Then:

**GIC** status becomes **MATURED** Funds are returned to **RRSP** account balance Negative Scenario 1 — **KYC** Not Verified

Given an unverified customer When attempting **RRSP** account creation Then system returns **422**

Negative Scenario 2 — **RRSP** Already Exists

Given customer already has **RRSP** account When creating another **RRSP** account Then system returns **409**

Negative Scenario 3 — Insufficient Funds for **GIC**

Given insufficient account balance When creating a **GIC** Then system returns **400** (insufficientFunds)
