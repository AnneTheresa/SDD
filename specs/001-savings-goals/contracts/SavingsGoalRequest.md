# SavingsGoalRequest Contract

**Version**: 1.0  
**Date**: June 5, 2026  
**Used by**: `POST /accounts/{account_id}/goals` | `PUT /accounts/{account_id}/goals/{goal_id}`

---

## Overview

The `SavingsGoalRequest` DTO encapsulates user input for creating or updating a savings goal. It contains the three questions answered by the customer:

1. What are you saving for? → `goal_name`
2. How much do you plan to save? → `target_amount`
3. By when? → `target_date`

Current account balance is NOT included in the request — it is always fetched from the live account record.

---

## Fields

| Field         | Type    | Required | Length                | Format          | Constraints                           |
| ------------- | ------- | -------- | --------------------- | --------------- | ------------------------------------- |
| goal_name     | string  | yes      | 1-255                 | free-text       | Non-empty; preset or custom value     |
| target_amount | decimal | yes      | 19 digits, 2 decimals | e.g., "5000.00" | > 0; precision DECIMAL(19,2)          |
| target_date   | string  | yes      | 10 chars              | "YYYY-MM-DD"    | ≥ today (ISO 8601 date only, no time) |

---

## Field Details

### goal_name

**Description**: The answer to "What are you saving for?"

**Valid Values**:

- Preset: `"Emergency Fund"`, `"Travel"`, `"Tuition"`, `"Home"`, `"Car"`, `"Retirement"`
- Custom: Any non-empty string up to 255 characters (e.g., `"Family vacation"`, `"New laptop"`)

**Validation**:

- Must not be empty or whitespace-only
- No length restriction on backend (stored as VARCHAR(255))
- Frontend may suggest presets to reduce duplicates

**Example**:

```json
"goal_name": "Travel"
```

### target_amount

**Description**: The answer to "How much do you plan to save?"

**Valid Values**:

- Any decimal number > 0
- Precision: up to 19 digits with 2 decimal places
- Currency: Assumed to be same as account currency (CAD for Voltio)

**Validation**:

- Must be strictly > 0 (not ≥)
- Must be a valid decimal (e.g., "5000.50", "100.00", not "5000.5a")
- Backend rounds to 2 decimals on save

**Example**:

```json
"target_amount": 5000.00
```

### target_date

**Description**: The answer to "By when?"

**Valid Values**:

- Date in ISO 8601 format: `"YYYY-MM-DD"` (e.g., `"2026-12-31"`)
- Must be today or in the future

**Validation**:

- On CREATE: Must be ≥ today
- On UPDATE: May allow past dates if customer extends a missed deadline (backend decides)
- Backend rejects invalid date formats
- Backend rejects dates more than 50 years in future (optional business rule)

**Example**:

```json
"target_date": "2026-12-31"
```

---

## JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "goal_name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 255
    },
    "target_amount": {
      "type": "number",
      "exclusiveMinimum": 0,
      "multipleOf": 0.01
    },
    "target_date": {
      "type": "string",
      "format": "date"
    }
  },
  "required": ["goal_name", "target_amount", "target_date"],
  "additionalProperties": false
}
```

---

## Examples

### Example 1: Create Emergency Fund Goal

**Request**:

```http
POST /accounts/42/goals
Content-Type: application/json

{
  "goal_name": "Emergency Fund",
  "target_amount": 3000.00,
  "target_date": "2026-12-31"
}
```

**Response** (201 Created): See [SavingsGoalResponse.md](SavingsGoalResponse.md)

---

### Example 2: Update Travel Goal (Increase Target)

**Request**:

```http
PUT /accounts/42/goals/1
Content-Type: application/json

{
  "goal_name": "Travel",
  "target_amount": 6000.00,
  "target_date": "2027-03-01"
}
```

**Response** (200 OK): See [SavingsGoalResponse.md](SavingsGoalResponse.md)

---

### Example 3: Custom Goal Name

**Request**:

```http
POST /accounts/87/goals
Content-Type: application/json

{
  "goal_name": "New Laptop",
  "target_amount": 1500.50,
  "target_date": "2026-09-15"
}
```

---

## Validation Errors

**400 Bad Request** responses and their mappings:

| Error                    | Cause                                             | Frontend Message                        |
| ------------------------ | ------------------------------------------------- | --------------------------------------- |
| `INVALID_GOAL_NAME`      | goal_name is empty or missing                     | "What are you saving for? is required"  |
| `INVALID_TARGET_AMOUNT`  | target_amount ≤ 0 or invalid format               | "Target amount must be greater than $0" |
| `INVALID_TARGET_DATE`    | target_date is in past (CREATE) or invalid format | "Target date must be in the future"     |
| `MISSING_REQUIRED_FIELD` | Any required field is missing                     | "[Field name] is required"              |

---

## Usage Notes

- Backend does not validate preset values — any string is accepted for goal_name
- Frontend should suggest presets for UX consistency, but backend accepts any value
- Balance is never included in request; always fetched from account at response time
- All fields are required; null or partial updates not supported (must provide all three)

---

## Version History

### v1.0 (June 5, 2026)

- Initial contract
- Three required fields: goal_name, target_amount, target_date
- Current balance derived from account; not in request
