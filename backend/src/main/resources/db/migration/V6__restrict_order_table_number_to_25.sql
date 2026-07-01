-- ==========================================================================
-- Le Bar'app - V6: restrict the customer table number range from 1..999 to
-- 1..25.
--
-- Business rule update (Notion "Développer le panier client"): a valid table
-- number is an integer from 1 to 25 inclusive. This migration REPLACES the
-- existing check constraint introduced by V4; it never edits V4 itself.
--
-- Migration-safe & non-destructive:
--   * No row is altered, clamped or deleted by this migration.
--   * The new constraint is fully VALIDATED, so it will only apply cleanly to a
--     database whose existing rows already satisfy 1..25. Before deploying,
--     audit any pre-existing out-of-range data with:
--
--         SELECT id, table_number
--         FROM customer_order
--         WHERE table_number NOT BETWEEN 1 AND 25;
--
--     Any offending rows are a data-retention decision to resolve out-of-band
--     (this migration deliberately does not silently rewrite order history).
-- ==========================================================================

ALTER TABLE customer_order
    DROP CONSTRAINT IF EXISTS ck_customer_order_table_number;

ALTER TABLE customer_order
    ADD CONSTRAINT ck_customer_order_table_number
    CHECK (table_number BETWEEN 1 AND 25);
