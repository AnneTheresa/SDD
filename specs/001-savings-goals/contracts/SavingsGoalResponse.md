# SavingsGoalResponse Contract

**Version**: 1.0  
**Date**: June 5, 2026  
**Used by**: `GET /accounts/{account_id}/goals` | `POST /accounts/{account_id}/goals` | `PUT /accounts/{account_id}/goals/{goal_id}` | `GET /customers/{customer_id}/goals`

---

## Overview

The `SavingsGoalResponse` DTO returns the complete state of a savings goal, including live-calculated progress and status. Progress percentage and status are never stored; they are calculated on every read to ensure accuracy.

---

## Fields

| Field               | Type              | Source                      | Calculated? | Description                                             |
| ------------------- | ----------------- | --------------------------- | ----------- | ------------------------------------------------------- |
| goal_id             | integer           | savings_goals.goal_id       | no          | Unique goal identifier                                  |
| account_id          | integer           | savings_goals.account_id    | no          | Account goal is linked to                               |
| account_number      | string            | account.account_number      | no          | Account number (display)                                |
| account_type        | enum              | account.account_type        | no          | Account type (SAVINGS, CHEQUING, RRSP, GIC)             |
| goal_name           | string            | savings_goals.goal_name     | no          | What are you saving for?                                |
| target_amount       | decimal(19,2)     | savings_goals.target_amount | no          | How much do you plan to save?                           |
| target_date         | date              | savings_goals.target_date   | no          | By when?                                                |
| current_balance     | decimal(19,2)     | account.balance             | no          | Live account balance (pulled at read time)              |
| progress_percentage | decimal(5,2)      | calculated                  | **yes**     | (current_balance / target_amount) \* 100, capped at 100 |
| time_remaining_days | integer           | calculated                  | **yes**     | target_date - TODAY, never negative                     |
| status              | enum              | calculated                  | **yes**     | NOT_STARTED, IN_PROGRESS, ACHIEVED, OVERDUE             |
| created_at          | string (ISO 8601) | savings_goals.created_at    | no          | Goal creation time (immutable)                          |
| updated_at          | string (ISO 8601) | savings_goals.updated_at    | no          | Last modification time                                  |

---

## Field Details

### goal_id

**Type**: integer (BIGINT in database)  
**Example**: `1`  
**Immutable**: Yes  
**Source**: Database auto-increment

---

### account_id

**Type**: integer (BIGINT in database)  
**Example**: `42`  
**Immutable**: Yes (on creation; cannot move goal to different account)  
**Source**: savings_goals.account_id

---

### account_number

**Type**: string  
**Example**: `"ACC-00042"`  
**Display Only**: Yes (informational for UI)  
**Source**: account.account_number (joined at read time)

---

### account_type

**Type**: enum  
**Valid Values**: `"SAVINGS"`, `"CHEQUING"`, `"RRSP"`, `"GIC"`, etc.  
**Example**: `"SAVINGS"`  
**Display Only**: Yes (informational for UI)  
**Source**: account.account_type (joined at read time)

---

### goal_name

**Type**: string  
**Example**: `"Travel"` or `"Family vacation"`  
**Length**: 1-255 characters  
**Immutable**: No (can be edited)  
**Source**: savings_goals.goal_name

---

### target_amount

**Type**: decimal(19,2)  
**Example**: `5000.00`  
**Immutable**: No (can be edited)  
**Currency**: CAD (assumed; same as account)  
**Precision**: Always 2 decimal places in response  
**Source**: savings_goals.target_amount

---

### target_date

**Type**: date (ISO 8601 format: "YYYY-MM-DD")  
**Example**: `"2026-12-31"`  
**Immutable**: No (can be edited)  
**Source**: savings_goals.target_date

---

### current_balance

**Type**: decimal(19,2)  
**Example**: `1200.00`  
**Immutable**: No (reflects live account balance; changes independently)  
**Source**: account.balance (joined at read time, NOT stored in savings_goals)  
**Note**: This is the live account balance, pulled from the account table. It is NOT the manually-entered "amount saved toward goal". It represents the total funds available in the account.

---

### progress_percentage

**Type**: decimal(5,2)  
**Range**: 0 to 100 (inclusive)  
**Example**: `24.00`  
**Formula**:

```
progress_percentage = (current_balance / target_amount) * 100
```

**Capping**: Response layer ensures maximum of 100% (never exceeds 100%)

**Calculation Example**:

- current_balance = 1200.00
- target_amount = 5000.00
- progress_percentage = (1200 / 5000) \* 100 = 24.00

**Immutable**: No (recalculated on every read)  
**Stored in Database**: No (calculated only)

---

### time_remaining_days

**Type**: integer  
**Range**: 0 to unlimited  
**Example**: `209`  
**Formula**:

```
time_remaining_days = MAX(0, target_date - TODAY)
```

**Behavior**:

- If target_date is today: returns `0`
- If target_date has passed: returns `0` (never negative)
- If target_date is in future: returns positive integer (days remaining)

**Immutable**: No (changes daily; recalculated on every read)  
**Stored in Database**: No (calculated only)

---

### status

**Type**: enum  
**Valid Values**:

- `"NOT_STARTED"` — No progress yet; time remains
- `"IN_PROGRESS"` — Partial progress toward goal; time remains
- `"ACHIEVED"` — Goal met (current_balance ≥ target_amount)
- `"OVERDUE"` — Deadline passed; goal not yet met

**Derivation Logic**:

```
if current_balance >= target_amount:
    status = "ACHIEVED"
else if target_date < today:
    status = "OVERDUE"
else if current_balance > 0:
    status = "IN_PROGRESS"
else:
    status = "NOT_STARTED"
```

**Immutable**: No (recalculated on every read)  
**Stored in Database**: Yes (for indexing efficiency; recalculated on read to ensure accuracy)

**Status Transition Table**:

| Condition                                   | Status      |
| ------------------------------------------- | ----------- |
| balance = 0, deadline not reached           | NOT_STARTED |
| balance > 0, < target, deadline not reached | IN_PROGRESS |
| balance ≥ target                            | ACHIEVED    |
| balance < target, deadline passed           | OVERDUE     |

---

### created_at

**Type**: string (ISO 8601 timestamp)  
**Example**: `"2026-06-05T10:00:00Z"`  
**Timezone**: UTC (always)  
**Immutable**: Yes  
**Source**: savings_goals.created_at (set at creation time)

---

### updated_at

**Type**: string (ISO 8601 timestamp)  
**Example**: `"2026-06-05T14:30:00Z"`  
**Timezone**: UTC (always)  
**Immutable**: No (updated on every modification)  
**Source**: savings_goals.updated_at (auto-updated by database on INSERT/UPDATE)

---

## Examples

### Example 1: Goal In Progress

```json
{
  "goal_id": 1,
  "account_id": 42,
  "account_number": "ACC-00042",
  "account_type": "SAVINGS",
  "goal_name": "Travel",
  "target_amount": 5000.0,
  "target_date": "2026-12-31",
  "current_balance": 1200.0,
  "progress_percentage": 24.0,
  "time_remaining_days": 209,
  "status": "IN_PROGRESS",
  "created_at": "2026-06-05T10:00:00Z",
  "updated_at": "2026-06-05T14:30:00Z"
}
```

### Example 2: Goal Achieved

```json
{
  "goal_id": 3,
  "account_id": 99,
  "account_number": "ACC-00099",
  "account_type": "CHEQUING",
  "goal_name": "Emergency Fund",
  "target_amount": 3000.0,
  "target_date": "2027-06-30",
  "current_balance": 3500.0,
  "progress_percentage": 100.0,
  "time_remaining_days": 390,
  "status": "ACHIEVED",
  "created_at": "2026-01-15T08:00:00Z",
  "updated_at": "2026-05-20T12:15:00Z"
}
```

### Example 3: Goal Overdue

```json
{
  "goal_id": 2,
  "account_id": 87,
  "account_number": "ACC-00087",
  "account_type": "CHEQUING",
  "goal_name": "Car",
  "target_amount": 10000.0,
  "target_date": "2026-03-01",
  "current_balance": 2500.0,
  "progress_percentage": 25.0,
  "time_remaining_days": 0,
  "status": "OVERDUE",
  "created_at": "2025-09-10T09:30:00Z",
  "updated_at": "2026-06-05T16:00:00Z"
}
```

### Example 4: Goal Not Started

```json
{
  "goal_id": 4,
  "account_id": 55,
  "account_number": "ACC-00055",
  "account_type": "RRSP",
  "goal_name": "Retirement",
  "target_amount": 50000.0,
  "target_date": "2031-12-31",
  "current_balance": 0.0,
  "progress_percentage": 0.0,
  "time_remaining_days": 1974,
  "status": "NOT_STARTED",
  "created_at": "2026-06-05T11:00:00Z",
  "updated_at": "2026-06-05T11:00:00Z"
}
```

---

## Bulk Response

**Endpoint**: `GET /customers/{customer_id}/goals`

**Response Type**: Array of SavingsGoalResponse objects

**Example**:

```json
[
  {
    "goal_id": 1,
    "account_id": 42,
    "account_number": "ACC-00042",
    "account_type": "SAVINGS",
    "goal_name": "Travel",
    "target_amount": 5000.0,
    "target_date": "2026-12-31",
    "current_balance": 1200.0,
    "progress_percentage": 24.0,
    "time_remaining_days": 209,
    "status": "IN_PROGRESS",
    "created_at": "2026-06-05T10:00:00Z",
    "updated_at": "2026-06-05T14:30:00Z"
  },
  {
    "goal_id": 2,
    "account_id": 87,
    "account_number": "ACC-00087",
    "account_type": "CHEQUING",
    "goal_name": "Emergency Fund",
    "target_amount": 3000.0,
    "target_date": "2026-03-01",
    "current_balance": 800.0,
    "progress_percentage": 26.67,
    "time_remaining_days": 0,
    "status": "OVERDUE",
    "created_at": "2026-05-01T14:30:00Z",
    "updated_at": "2026-05-01T14:30:00Z"
  }
]
```

---

## JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "goal_id": { "type": "integer" },
    "account_id": { "type": "integer" },
    "account_number": { "type": "string" },
    "account_type": {
      "type": "string",
      "enum": ["SAVINGS", "CHEQUING", "RRSP", "GIC"]
    },
    "goal_name": { "type": "string", "minLength": 1, "maxLength": 255 },
    "target_amount": { "type": "number", "multipleOf": 0.01 },
    "target_date": { "type": "string", "format": "date" },
    "current_balance": { "type": "number", "multipleOf": 0.01 },
    "progress_percentage": {
      "type": "number",
      "minimum": 0,
      "maximum": 100,
      "multipleOf": 0.01
    },
    "time_remaining_days": { "type": "integer", "minimum": 0 },
    "status": {
      "type": "string",
      "enum": ["NOT_STARTED", "IN_PROGRESS", "ACHIEVED", "OVERDUE"]
    },
    "created_at": { "type": "string", "format": "date-time" },
    "updated_at": { "type": "string", "format": "date-time" }
  },
  "required": [
    "goal_id",
    "account_id",
    "goal_name",
    "target_amount",
    "target_date",
    "current_balance",
    "progress_percentage",
    "time_remaining_days",
    "status",
    "created_at",
    "updated_at"
  ],
  "additionalProperties": false
}
```

---

## Version History

### v1.0 (June 5, 2026)

- Initial contract
- 12 fields (3 stored, 3 informational joins, 3 calculated, 3 audit)
- Progress percentage calculated at read time
- Status derived from balance and target_date
- Time remaining never negative
