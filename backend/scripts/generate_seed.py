#!/usr/bin/env python3
"""
Generates fake-but-consistent seed data for the local H2 database.

Produces two files next to this script:
  seed_demo_data.sql    - inserts 100 customers, their accounts, ~12 months
                          of transaction history, and savings goals
  unseed_demo_data.sql  - removes everything the seed file inserted

Design rules that keep the data consistent with the application's logic:

  * All seeded rows use IDs in a reserved high range so they can never collide
    with rows the running application creates:
        customers   900001 .. 900100
        accounts    900001 .. 9003xx
        transactions TRANSACTION_ID prefixed with 'SEED-'
  * ACCOUNT.BALANCE is computed by replaying the generated transactions, so the
    stored balance always equals opening balance + credits - debits.
  * No transaction can drive an account balance negative.
  * CUSTOMERS.TYPE is written as a TINYINT ordinal (0=PERSON, 1=COMPANY)
    because Customer.type has no @Enumerated annotation and JPA therefore
    defaults to ORDINAL.
  * Categories are restricted to the exact strings in
    SpendingInsightService.VALID_CATEGORIES.
  * TIMESTAMP and EXTERNAL_TRANSACTION_ID are written explicitly because raw
    SQL bypasses the @PrePersist hook on the Transaction entity.
  * Savings goals target non-CHECKING accounts only, at most one per account
    (the entity declares a UNIQUE constraint on customer_id + account_id).
    SavingsGoalService derives status and progress at read time from
    account.balance vs target_amount, so each goal's TARGET_AMOUNT is chosen
    backwards from the account's already-computed balance. That keeps the
    stored STATUS identical to what deriveStatus() returns, instead of the
    two disagreeing the moment the app reads the row.

Usage:
    python generate_seed.py
    python generate_seed.py --customers 100 --months 12 --seed 42
"""

import argparse
import collections
import random
from datetime import date, datetime, timedelta

# ---------------------------------------------------------------------------
# Reserved ID ranges - chosen so seeded rows never collide with app-created rows
# ---------------------------------------------------------------------------
CUSTOMER_ID_BASE = 900000
ACCOUNT_ID_BASE = 900000
GOAL_ID_BASE = 900000
TXN_PREFIX = "SEED-"

# ---------------------------------------------------------------------------
# Domain vocabulary - mirrors the entity enums
# ---------------------------------------------------------------------------
ACCOUNT_TYPES = ["CHECKING", "SAVINGS", "TFSA", "RRSP"]

# Exact strings from SpendingInsightService.VALID_CATEGORIES
CAT_HOUSING = "Housing"
CAT_TRANSPORT = "Transport"
CAT_FOOD = "Food & Drink"
CAT_ENTERTAINMENT = "Entertainment"
CAT_SHOPPING = "Shopping"
CAT_UTILITIES = "Utilities"
CAT_HEALTH = "Health"
CAT_INCOME = "Income"

FIRST_NAMES = [
    "Aisha", "Liam", "Sofia", "Noah", "Priya", "Ethan", "Mei", "Lucas",
    "Amara", "Oliver", "Yuki", "Mateo", "Zara", "Henry", "Nina", "Omar",
    "Chloe", "Raj", "Elena", "Felix", "Ingrid", "Tomas", "Leila", "Andre",
    "Hana", "Viktor", "Rosa", "Dmitri", "Sana", "Caleb", "Marta", "Kenji",
    "Fatima", "Diego", "Anya", "Malik", "清", "Ines", "Bram", "Tavia",
]

LAST_NAMES = [
    "Okafor", "Nguyen", "Rossi", "Patel", "Kim", "Silva", "Dubois", "Haddad",
    "Novak", "Andersen", "Costa", "Fischer", "Moreau", "Yamamoto", "Kowalski",
    "Ferreira", "Ivanov", "Bergstrom", "Tremblay", "Mensah", "Larsen",
    "Cabrera", "Voss", "Rahman", "Kaur", "Lindqvist", "Bianchi", "Osei",
]

COMPANY_NAMES = [
    "Northwind Logistics", "Blue Harbour Trading", "Cedar Peak Consulting",
    "Halcyon Foods", "Ironwood Manufacturing", "Lumen Analytics",
    "Meridian Freight", "Quartz Digital", "Riverstone Supply",
    "Summit Fabrication",
]

STREETS = [
    "King St W", "Bay St", "Rue Sainte-Catherine", "Granville St", "Portage Ave",
    "Barrington St", "Whyte Ave", "Yonge St", "Robson St", "Elgin St",
]

CITIES = [
    ("Toronto", "ON"), ("Montreal", "QC"), ("Vancouver", "BC"),
    ("Calgary", "AB"), ("Ottawa", "ON"), ("Halifax", "NS"),
    ("Winnipeg", "MB"), ("Edmonton", "AB"), ("Victoria", "BC"),
]

EMPLOYERS = [
    "ACME Corp", "Northwind Logistics", "Lumen Analytics", "Cedar Peak",
    "Halcyon Foods", "Meridian Freight", "Quartz Digital", "Riverstone Supply",
]

# (merchant, category, low, high) - amounts in dollars
MERCHANTS = [
    ("TIM HORTONS #",      CAT_FOOD,           4.25,   28.90),
    ("LOBLAWS #",          CAT_FOOD,          38.00,  210.00),
    ("UBER EATS",          CAT_FOOD,          18.50,   74.00),
    ("SUSHI KAN",          CAT_FOOD,          24.00,   96.00),
    ("PRESTO FARE",        CAT_TRANSPORT,      3.35,  156.00),
    ("PETRO-CANADA #",     CAT_TRANSPORT,     42.00,  118.00),
    ("UBER TRIP",          CAT_TRANSPORT,      9.80,   62.00),
    ("CINEPLEX ODEON",     CAT_ENTERTAINMENT, 14.99,   78.00),
    ("SPOTIFY PREMIUM",    CAT_ENTERTAINMENT, 10.99,   16.99),
    ("STEAM GAMES",        CAT_ENTERTAINMENT,  8.99,   89.99),
    ("AMAZON.CA",          CAT_SHOPPING,      12.40,  340.00),
    ("CANADIAN TIRE #",    CAT_SHOPPING,      22.00,  280.00),
    ("WINNERS #",          CAT_SHOPPING,      31.00,  190.00),
    ("HYDRO ONE",          CAT_UTILITIES,     62.00,  185.00),
    ("ROGERS COMMUNICATIONS", CAT_UTILITIES,  75.00,  145.00),
    ("ENBRIDGE GAS",       CAT_UTILITIES,     38.00,  160.00),
    ("SHOPPERS DRUG MART", CAT_HEALTH,        11.50,  128.00),
    ("GOODLIFE FITNESS",   CAT_HEALTH,        44.99,   79.99),
    ("DENTALCARE ASSOC",   CAT_HEALTH,        95.00,  420.00),
]

# Merchants deliberately left uncategorised (CATEGORY = NULL) so the
# CategoryResolver fallback path in SpendingInsightService gets exercised.
UNCATEGORISED_RATE = 0.12

# Goal names - the preset values a user picks from, plus some free text
GOAL_NAMES = [
    "Travel", "Emergency Fund", "Home Down Payment", "New Car",
    "Wedding", "Education", "Retirement Top-Up", "Home Renovation",
    "Kitchen remodel", "Trip to Japan", "Baby fund", "Debt payoff buffer",
]

# Share of eligible (non-CHECKING, ACTIVE) accounts that get a goal, and the
# target mix of derived statuses among them.
GOAL_COVERAGE = 0.72
GOAL_STATUS_MIX = [
    ("IN_PROGRESS", 0.46),   # balance > 0, below target, deadline ahead
    ("ACHIEVED",    0.24),   # balance >= target
    ("OVERDUE",     0.22),   # deadline passed, still short
    ("NOT_STARTED", 0.08),   # balance = 0, deadline ahead
]

# ---------------------------------------------------------------------------
# Risk profiles
#
# RiskScoreService scores a customer on three factors, each banded in
# risk-score-rules.yaml and combined with weights 0.5 / 0.35 / 0.15:
#
#   SPENDING_INCOME_RATIO  total DEBIT / total CREDIT over the last 3 months
#   SAVING_BALANCE         total balance / average monthly spend ("months of
#                          coverage")
#   GOAL_PROGRESS          target-weighted mean of goal completion percent
#
# Without profiles every generated customer looks the same: rent is a modest
# slice of salary and the balance floor caps everyday spending, so the ratio
# always lands under 0.5 and coverage runs into the tens of months - every
# customer scores LOW. These profiles spread customers across all four bands
# so each one is reachable in local testing.
#
# The balance floor still holds: "strained" customers drain their balance
# toward zero across the window rather than below it, which is what produces
# both a high ratio and thin coverage.
#
#   rent_frac        rent as a share of monthly income
#   spend_frac       everyday card spend as a share of income, per month
#   open_mult        opening balance multiplier (thin balance => low coverage)
#   contrib_rate     chance a savings account gets its monthly contribution
#   goal_progress    (lo, hi) fraction of target already saved
#   share            portion of the customer base with this profile
# ---------------------------------------------------------------------------
RISK_PROFILES = [
    {
        "name": "healthy",           # -> LOW
        "rent_frac": (0.20, 0.28),
        "spend_frac": (0.10, 0.18),
        "open_mult": (1.00, 1.00),
        "contrib_rate": 0.90,
        "goal_progress": (0.80, 0.99),
        "share": 0.45,
    },
    {
        "name": "comfortable",       # -> MODERATE
        "rent_frac": (0.32, 0.40),
        "spend_frac": (0.28, 0.36),
        "open_mult": (0.22, 0.34),
        "contrib_rate": 0.30,
        "goal_progress": (0.35, 0.58),
        "share": 0.30,
    },
    {
        "name": "stretched",         # -> ELEVATED
        "rent_frac": (0.40, 0.48),
        "spend_frac": (0.44, 0.52),
        "open_mult": (0.08, 0.16),
        "contrib_rate": 0.10,
        "goal_progress": (0.12, 0.30),
        "share": 0.17,
    },
    {
        "name": "strained",          # -> HIGH
        "rent_frac": (0.42, 0.50),
        "spend_frac": (0.54, 0.66),
        "open_mult": (0.05, 0.10),
        "contrib_rate": 0.0,
        "goal_progress": (0.02, 0.09),
        "share": 0.08,
    },
]


def sql_str(value):
    """Quote a Python value as a SQL string literal, or NULL."""
    if value is None:
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"


def money(value):
    """Round to 2dp the way NUMERIC(19,2) will store it."""
    return round(value + 1e-9, 2)


def ts(dt):
    """Format a datetime as an H2 TIMESTAMP literal."""
    return "TIMESTAMP '" + dt.strftime("%Y-%m-%d %H:%M:%S") + "'"


def month_starts(months, end_month):
    """Return the first day of each month, oldest first, ending at end_month."""
    out = []
    y, m = end_month.year, end_month.month
    for _ in range(months):
        out.append(date(y, m, 1))
        m -= 1
        if m == 0:
            m = 12
            y -= 1
    return list(reversed(out))


def pick_status(rng):
    """Draw a goal status from GOAL_STATUS_MIX."""
    roll = rng.random()
    cumulative = 0.0
    for name, share in GOAL_STATUS_MIX:
        cumulative += share
        if roll < cumulative:
            return name
    return GOAL_STATUS_MIX[-1][0]


def pick_risk_profile(rng):
    """Draw a risk profile from RISK_PROFILES by its share."""
    roll = rng.random()
    cumulative = 0.0
    for profile in RISK_PROFILES:
        cumulative += profile["share"]
        if roll < cumulative:
            return profile
    return RISK_PROFILES[-1]


def build_goals(rng, accounts, today):
    """Create savings goals on non-CHECKING ACTIVE accounts.

    SavingsGoalService.deriveStatus() recomputes status on every read from
    account.balance, target_amount and target_date, in this precedence:

        balance >= target                      -> ACHIEVED
        target_date < today AND balance < target -> OVERDUE
        balance > 0                            -> IN_PROGRESS
        otherwise                              -> NOT_STARTED

    So the target amount and date here are derived from the account's balance
    to land on the intended status. Writing a status that the service would
    disagree with would make the seeded rows contradict themselves on read.
    """
    goals = []
    next_goal_id = GOAL_ID_BASE + 1

    for acct in accounts:
        if acct["type"] == "CHECKING" or acct["status"] != "ACTIVE":
            continue
        if rng.random() > GOAL_COVERAGE:
            continue

        balance = acct["balance"]
        wanted = pick_status(rng)

        # A zero-balance account can only ever read as NOT_STARTED or OVERDUE,
        # and no seeded ACTIVE account ends at zero, so treat balance > 0 as
        # given and force NOT_STARTED onto a fresh zero-target account instead.
        if wanted == "NOT_STARTED":
            # Needs balance = 0, which contradicts the seeded balance, so fall
            # back to IN_PROGRESS rather than emit a self-inconsistent row.
            wanted = "IN_PROGRESS"

        # SavingsGoalService computes progress as balance / target * 100, so
        # inverting the profile's intended progress gives the target that
        # reads back at that percentage. GOAL_PROGRESS is scored on this.
        progress_lo, progress_hi = acct.get("goal_progress", (0.35, 0.95))
        progress = rng.uniform(progress_lo, progress_hi)

        # A profile that saves little cannot also have met its goal, so
        # redirect ACHIEVED to the profile's own progress range instead of
        # overriding it back to full.
        if wanted == "ACHIEVED" and progress_hi < 0.75:
            wanted = "OVERDUE" if rng.random() < 0.5 else "IN_PROGRESS"

        if wanted == "ACHIEVED":
            # target at or below balance
            target = money(balance * rng.uniform(0.55, 0.98))
            target_date = today + timedelta(days=rng.randint(-240, 400))
        elif wanted == "OVERDUE":
            # deadline in the past, target still above balance
            target = money(balance / min(progress, 0.95))
            target_date = today - timedelta(days=rng.randint(5, 300))
        else:  # IN_PROGRESS
            target = money(balance / min(progress, 0.95))
            target_date = today + timedelta(days=rng.randint(20, 730))

        # Guard against a degenerate target of zero (target_amount must be > 0)
        if target < 100:
            target = money(rng.uniform(500, 5000) + balance)
            target_date = today + timedelta(days=rng.randint(60, 540))
            wanted = "IN_PROGRESS"

        created = acct["created"] + timedelta(days=rng.randint(1, 60))

        goals.append({
            "id": next_goal_id,
            "customer_id": acct["customer_id"],
            "account_id": acct["id"],
            "name": rng.choice(GOAL_NAMES),
            "target": target,
            "target_date": target_date,
            "status": wanted,
            "created": created,
        })
        next_goal_id += 1

    return goals


def build(rng, num_customers, months, today):
    """Generate all rows. Returns (customers, accounts, transactions)."""
    customers, accounts, transactions = [], [], []
    starts = month_starts(months, today)
    next_account_id = ACCOUNT_ID_BASE + 1
    txn_counter = 0

    for i in range(num_customers):
        customer_id = CUSTOMER_ID_BASE + 1 + i

        # ~8% of customers are companies
        is_company = rng.random() < 0.08
        if is_company:
            name = rng.choice(COMPANY_NAMES) + " " + rng.choice(
                ["Inc", "Ltd", "Group", "Holdings"])
            type_ordinal = 1                       # CustomerType.COMPANY
            dob = date(rng.randint(1985, 2020), rng.randint(1, 12),
                       rng.randint(1, 28))         # incorporation date
        else:
            name = rng.choice(FIRST_NAMES) + " " + rng.choice(LAST_NAMES)
            type_ordinal = 0                       # CustomerType.PERSON
            dob = date(rng.randint(1955, 2005), rng.randint(1, 12),
                       rng.randint(1, 28))

        city, prov = rng.choice(CITIES)
        address = "%d %s, %s, %s" % (
            rng.randint(1, 2400), rng.choice(STREETS), city, prov)

        # 92% KYC verified - the rest are useful negative-path test cases
        kyc = rng.random() < 0.92
        created = datetime.combine(starts[0], datetime.min.time()) - timedelta(
            days=rng.randint(30, 900))

        customers.append({
            "id": customer_id, "name": name, "address": address,
            "type": type_ordinal, "dob": dob, "kyc": kyc, "created": created,
        })

        # --- accounts: always a CHECKING, plus 1-2 others of distinct type ---
        extra_types = rng.sample(["SAVINGS", "TFSA", "RRSP"],
                                 rng.choice([1, 1, 2]))
        cust_account_types = ["CHECKING"] + extra_types

        # Monthly salary drives this customer's whole cash-flow profile
        monthly_income = money(rng.uniform(2600, 9800)) if not is_company \
            else money(rng.uniform(14000, 52000))
        employer = rng.choice(EMPLOYERS)

        # Risk profile fixes this customer's spending and saving behaviour so
        # the seeded population covers every RiskScoreService band.
        profile = pick_risk_profile(rng)
        rent_frac = rng.uniform(*profile["rent_frac"])
        spend_frac = rng.uniform(*profile["spend_frac"])
        open_mult = rng.uniform(*profile["open_mult"])
        customers[-1]["profile"] = profile["name"]

        for idx, acct_type in enumerate(cust_account_types):
            account_id = next_account_id
            next_account_id += 1
            is_primary = (idx == 0)

            # Opening balance before any seeded transaction. open_mult thins
            # the balance for higher-risk profiles, which is what drives their
            # months-of-coverage down.
            if acct_type == "CHECKING":
                balance = money(rng.uniform(400, 5200) * open_mult)
            elif acct_type == "SAVINGS":
                balance = money(rng.uniform(1500, 48000) * open_mult)
            else:                                   # TFSA / RRSP
                balance = money(rng.uniform(0, 32000) * open_mult)

            # A small number of non-primary accounts are FROZEN or CLOSED
            roll = rng.random()
            if not is_primary and roll < 0.04:
                status = "FROZEN"
            elif not is_primary and roll < 0.06:
                status = "CLOSED"
            else:
                status = "ACTIVE"

            interest = {
                "CHECKING": None,
                "SAVINGS": round(rng.uniform(0.015, 0.042), 4),
                "TFSA": round(rng.uniform(0.020, 0.055), 4),
                "RRSP": round(rng.uniform(0.025, 0.060), 4),
            }[acct_type]

            acct = {
                "id": account_id, "customer_id": customer_id,
                "type": acct_type, "status": status,
                "number": "ACC%010d" % account_id,
                "interest": interest,
                "limit": money(rng.choice([1000, 3000, 3000, 5000, 10000])),
                "created": created,
                "opening": balance,
                "goal_progress": profile["goal_progress"],
            }

            # Closed/frozen accounts get no seeded activity
            if status != "ACTIVE":
                acct["balance"] = balance
                acct["closed_at"] = datetime.combine(
                    starts[-1], datetime.min.time()) if status == "CLOSED" \
                    else None
                accounts.append(acct)
                continue
            acct["closed_at"] = None

            # ---------------- monthly transaction history ----------------
            for m_start in starts:
                if acct_type == "CHECKING":
                    # Salary in, then living expenses out
                    pay_day = m_start + timedelta(days=rng.choice([0, 1, 14]))
                    pay_dt = datetime.combine(pay_day, datetime.min.time()) \
                        + timedelta(hours=9, minutes=rng.randint(0, 50))
                    amt = money(monthly_income * rng.uniform(0.97, 1.03))
                    balance = money(balance + amt)
                    txn_counter += 1
                    transactions.append({
                        "id": "%s%06d" % (TXN_PREFIX, txn_counter),
                        "account_id": account_id, "amount": amt,
                        "direction": "CREDIT", "status": "SUCCESS",
                        "when": pay_dt,
                        "description": "Payroll deposit - " + employer,
                        "sender": employer + " Payroll",
                        "receiver": acct["number"],
                        "category": CAT_INCOME,
                    })

                    # Rent / mortgage - one fixed large debit
                    rent = money(monthly_income * rent_frac)
                    if balance - rent > 0:
                        rent_dt = datetime.combine(
                            m_start + timedelta(days=rng.choice([0, 1, 2])),
                            datetime.min.time()) + timedelta(
                                hours=8, minutes=rng.randint(0, 40))
                        balance = money(balance - rent)
                        txn_counter += 1
                        transactions.append({
                            "id": "%s%06d" % (TXN_PREFIX, txn_counter),
                            "account_id": account_id, "amount": rent,
                            "direction": "DEBIT", "status": "SUCCESS",
                            "when": rent_dt,
                            "description": "Rent payment",
                            "sender": acct["number"],
                            "receiver": "Property Management",
                            "category": CAT_HOUSING,
                        })

                    # Everyday card spending. The profile sets a monthly
                    # budget as a share of income; each purchase keeps its
                    # merchant-realistic shape but is scaled so the month's
                    # total lands on that budget. This is what moves the
                    # spending/income ratio between profiles.
                    txn_count = rng.randint(12, 26)
                    budget = monthly_income * spend_frac
                    raw = [rng.uniform(lo, hi) for lo, hi in
                           [(m[2], m[3]) for m in
                            [rng.choice(MERCHANTS) for _ in range(txn_count)]]]
                    scale = budget / sum(raw) if sum(raw) > 0 else 0.0
                    for raw_amt in raw:
                        merchant, cat, lo, hi = rng.choice(MERCHANTS)
                        amt = money(raw_amt * scale)
                        if amt <= 0:
                            continue
                        if balance - amt <= 0:
                            continue        # never let the balance go negative
                        day = m_start + timedelta(days=rng.randint(0, 27))
                        when = datetime.combine(day, datetime.min.time()) \
                            + timedelta(hours=rng.randint(7, 22),
                                        minutes=rng.randint(0, 59))
                        label = merchant
                        if merchant.endswith("#"):
                            label = merchant + str(rng.randint(1000, 9999))
                        balance = money(balance - amt)
                        txn_counter += 1
                        transactions.append({
                            "id": "%s%06d" % (TXN_PREFIX, txn_counter),
                            "account_id": account_id, "amount": amt,
                            "direction": "DEBIT", "status": "SUCCESS",
                            "when": when, "description": label,
                            "sender": acct["number"], "receiver": label,
                            "category": None
                            if rng.random() < UNCATEGORISED_RATE else cat,
                        })
                else:
                    # Savings / TFSA / RRSP - a monthly contribution, and
                    # occasionally a withdrawal. Higher-risk profiles
                    # contribute rarely or never, so their balance stays thin.
                    if rng.random() < profile["contrib_rate"]:
                        amt = money(rng.uniform(100, 1200))
                        day = m_start + timedelta(days=rng.randint(1, 26))
                        when = datetime.combine(day, datetime.min.time()) \
                            + timedelta(hours=rng.randint(8, 20),
                                        minutes=rng.randint(0, 59))
                        balance = money(balance + amt)
                        txn_counter += 1
                        transactions.append({
                            "id": "%s%06d" % (TXN_PREFIX, txn_counter),
                            "account_id": account_id, "amount": amt,
                            "direction": "TRANSFER", "status": "SUCCESS",
                            "when": when,
                            "description": "Monthly contribution",
                            "sender": "ACC%010d" % (
                                ACCOUNT_ID_BASE + 1 + idx),
                            "receiver": acct["number"],
                            "category": None,
                        })
                    if rng.random() < 0.12:
                        amt = money(rng.uniform(80, 900))
                        if balance - amt > 0:
                            day = m_start + timedelta(days=rng.randint(1, 26))
                            when = datetime.combine(
                                day, datetime.min.time()) + timedelta(
                                    hours=rng.randint(8, 20),
                                    minutes=rng.randint(0, 59))
                            balance = money(balance - amt)
                            txn_counter += 1
                            transactions.append({
                                "id": "%s%06d" % (TXN_PREFIX, txn_counter),
                                "account_id": account_id, "amount": amt,
                                "direction": "TRANSFER", "status": "SUCCESS",
                                "when": when,
                                "description": "Withdrawal to chequing",
                                "sender": acct["number"],
                                "receiver": "ACC%010d" % (ACCOUNT_ID_BASE + 1),
                                "category": None,
                            })

            acct["balance"] = balance
            accounts.append(acct)

    return customers, accounts, transactions


def write_seed(path, customers, accounts, transactions, goals, args):
    lines = []
    w = lines.append
    w("-- ---------------------------------------------------------------")
    w("-- Demo seed data for local development.")
    w("-- GENERATED FILE - edit generate_seed.py and re-run instead.")
    w("--")
    w("--   customers    : %d  (CUSTOMER_ID %d..%d)"
      % (len(customers), CUSTOMER_ID_BASE + 1,
         CUSTOMER_ID_BASE + len(customers)))
    w("--   accounts     : %d  (ACCOUNT_ID  %d..%d)"
      % (len(accounts), ACCOUNT_ID_BASE + 1, ACCOUNT_ID_BASE + len(accounts)))
    w("--   transactions : %d (TRANSACTION_ID LIKE '%s%%')"
      % (len(transactions), TXN_PREFIX))
    w("--   savings goals: %d  (GOAL_ID     %d..%d)"
      % (len(goals), GOAL_ID_BASE + 1, GOAL_ID_BASE + len(goals)))
    w("--   history      : %d months ending %s"
      % (args.months, args.today))
    w("--   rng seed     : %d  (same seed => byte-identical output)"
      % args.seed)
    w("--")
    w("-- Balances are computed by replaying the transactions below, so each")
    w("-- ACCOUNT.BALANCE equals its opening balance plus credits minus")
    w("-- debits. No account is ever driven negative.")
    w("--")
    w("-- Each customer is generated from a risk profile so RiskScoreService")
    w("-- has data in every band. Customer IDs by profile:")
    for prof in RISK_PROFILES:
        ids = [str(c["id"]) for c in customers
               if c.get("profile") == prof["name"]]
        w("--   %-12s (%2d): %s" % (prof["name"], len(ids), ", ".join(ids)))
    w("--")
    w("-- Run unseed_demo_data.sql to remove every row inserted here.")
    w("-- ---------------------------------------------------------------")
    w("")

    w("-- ---------------------------- customers ----------------------------")
    w("-- TYPE is a TINYINT ordinal: 0 = PERSON, 1 = COMPANY")
    for c in customers:
        w("INSERT INTO CUSTOMERS (CUSTOMER_ID, NAME, ADDRESS, TYPE, "
          "DATE_OF_BIRTH, KYC_VERIFIED, DELETED_AT, CREATED_AT, UPDATED_AT) "
          "VALUES (%d, %s, %s, %d, DATE '%s', %s, NULL, %s, %s);"
          % (c["id"], sql_str(c["name"]), sql_str(c["address"]), c["type"],
             c["dob"].isoformat(), "TRUE" if c["kyc"] else "FALSE",
             ts(c["created"]), ts(c["created"])))
    w("")

    w("-- ----------------------------- accounts ----------------------------")
    for a in accounts:
        w("INSERT INTO ACCOUNT (ACCOUNT_ID, CUSTOMER_ID, ACCOUNT_TYPE, STATUS, "
          "BALANCE, INTEREST_RATE, ACCOUNT_NUMBER, DAILY_TRANSFER_LIMIT, "
          "DELETED_AT, CLOSED_AT, VERSION, CREATED_AT, UPDATED_AT) "
          "VALUES (%d, %d, '%s', '%s', %.2f, %s, %s, %.2f, NULL, %s, 0, %s, %s);"
          % (a["id"], a["customer_id"], a["type"], a["status"], a["balance"],
             "NULL" if a["interest"] is None else "%.4f" % a["interest"],
             sql_str(a["number"]), a["limit"],
             "NULL" if a["closed_at"] is None else ts(a["closed_at"]),
             ts(a["created"]), ts(a["created"])))
    w("")

    w("-- --------------------------- transactions --------------------------")
    w("-- TIMESTAMP and EXTERNAL_TRANSACTION_ID are set explicitly because")
    w("-- raw SQL bypasses the @PrePersist hook on the Transaction entity.")
    for t in sorted(transactions, key=lambda r: (r["when"], r["id"])):
        w("INSERT INTO BANK_TRANSACTION (TRANSACTION_ID, ACCOUNT_ID, AMOUNT, "
          "DIRECTION, STATUS, TIMESTAMP, DESCRIPTION, SENDER_INFO, "
          "RECEIVER_INFO, IDEMPOTENCY_KEY, CATEGORY, EXTERNAL_TRANSACTION_ID) "
          "VALUES (%s, %d, %.2f, '%s', '%s', %s, %s, %s, %s, NULL, %s, "
          "RANDOM_UUID());"
          % (sql_str(t["id"]), t["account_id"], t["amount"], t["direction"],
             t["status"], ts(t["when"]), sql_str(t["description"]),
             sql_str(t["sender"]), sql_str(t["receiver"]),
             sql_str(t["category"])))
    w("")

    w("-- --------------------------- savings goals -------------------------")
    w("-- STATUS is stored, but SavingsGoalService recomputes it on every read")
    w("-- from account.balance vs target_amount/target_date. Targets below are")
    w("-- derived from each account's balance so both agree.")
    for g in goals:
        w("INSERT INTO SAVINGS_GOALS (GOAL_ID, CUSTOMER_ID, ACCOUNT_ID, "
          "GOAL_NAME, TARGET_AMOUNT, TARGET_DATE, STATUS, DELETED_AT, "
          "CREATED_AT, UPDATED_AT) "
          "VALUES (%d, %d, %d, %s, %.2f, DATE '%s', '%s', NULL, %s, %s);"
          % (g["id"], g["customer_id"], g["account_id"], sql_str(g["name"]),
             g["target"], g["target_date"].isoformat(), g["status"],
             ts(g["created"]), ts(g["created"])))
    w("")

    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines))


def write_unseed(path):
    content = """\
-- ---------------------------------------------------------------
-- Removes every row inserted by seed_demo_data.sql.
-- GENERATED FILE - edit generate_seed.py and re-run instead.
--
-- Safe to run more than once, and safe to run when the seed was
-- never applied: each statement simply matches zero rows.
--
-- Deletion order follows the foreign keys: savings goals and
-- transactions reference accounts, and accounts reference customers.
-- ---------------------------------------------------------------

DELETE FROM SAVINGS_GOALS WHERE CUSTOMER_ID BETWEEN 900001 AND 999999;

DELETE FROM SAVINGS_GOALS
 WHERE ACCOUNT_ID IN (SELECT ACCOUNT_ID FROM ACCOUNT
                       WHERE CUSTOMER_ID BETWEEN 900001 AND 999999);

DELETE FROM BANK_TRANSACTION WHERE TRANSACTION_ID LIKE 'SEED-%';

DELETE FROM BANK_TRANSACTION
 WHERE ACCOUNT_ID IN (SELECT ACCOUNT_ID FROM ACCOUNT
                       WHERE CUSTOMER_ID BETWEEN 900001 AND 999999);

DELETE FROM ACCOUNT WHERE CUSTOMER_ID BETWEEN 900001 AND 999999;

DELETE FROM CUSTOMERS WHERE CUSTOMER_ID BETWEEN 900001 AND 999999;
"""
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(content)


def main():
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--customers", type=int, default=100)
    p.add_argument("--months", type=int, default=12)
    p.add_argument("--seed", type=int, default=42,
                   help="RNG seed; same value reproduces the same file")
    p.add_argument("--today", default=date.today().isoformat(),
                   help="last month of history, YYYY-MM-DD")
    args = p.parse_args()
    args.today = date.fromisoformat(args.today)

    rng = random.Random(args.seed)
    customers, accounts, transactions = build(
        rng, args.customers, args.months, args.today)
    goals = build_goals(rng, accounts, args.today)

    import os
    here = os.path.dirname(os.path.abspath(__file__))
    seed_path = os.path.join(here, "seed_demo_data.sql")
    unseed_path = os.path.join(here, "unseed_demo_data.sql")

    write_seed(seed_path, customers, accounts, transactions, goals, args)
    write_unseed(unseed_path)

    by_status = collections.Counter(g["status"] for g in goals)
    print("customers    : %d" % len(customers))
    print("accounts     : %d" % len(accounts))
    print("transactions : %d" % len(transactions))
    print("savings goals: %d  (%s)" % (
        len(goals),
        ", ".join("%s=%d" % kv for kv in sorted(by_status.items()))))
    print("wrote %s" % seed_path)
    print("wrote %s" % unseed_path)


if __name__ == "__main__":
    main()
