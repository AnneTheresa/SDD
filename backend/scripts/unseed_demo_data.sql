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
