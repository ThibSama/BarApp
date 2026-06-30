-- ==========================================================================
-- Le Bar'app - V4: order table number + payment method, and catalog
-- description columns retained by the UI.
--
-- All changes are additive and migration-safe: new columns are first added
-- NULLable, existing rows are backfilled with a documented sentinel, and only
-- then are the NOT NULL / CHECK constraints applied. No existing data is
-- deleted and no applied migration is modified.
-- ==========================================================================

-- ---------------------------------------------------------------------------
-- customer_order.table_number
-- Positive integer with a documented upper bound (1..999). Existing rows (if
-- any) predate the feature and are backfilled with the lowest valid table (1).
-- ---------------------------------------------------------------------------
ALTER TABLE customer_order ADD COLUMN table_number INTEGER;

UPDATE customer_order SET table_number = 1 WHERE table_number IS NULL;

ALTER TABLE customer_order ALTER COLUMN table_number SET NOT NULL;

ALTER TABLE customer_order
    ADD CONSTRAINT ck_customer_order_table_number
    CHECK (table_number BETWEEN 1 AND 999);

-- ---------------------------------------------------------------------------
-- customer_order.payment_method
-- Constrained set mirroring the application's supported PaymentMethod enum.
-- Existing rows are backfilled with CASH_AT_COUNTER (counter payment), the
-- safe default for historical anonymous orders.
-- ---------------------------------------------------------------------------
ALTER TABLE customer_order ADD COLUMN payment_method VARCHAR(30);

UPDATE customer_order SET payment_method = 'CASH_AT_COUNTER' WHERE payment_method IS NULL;

ALTER TABLE customer_order ALTER COLUMN payment_method SET NOT NULL;

ALTER TABLE customer_order
    ADD CONSTRAINT ck_customer_order_payment_method
    CHECK (payment_method IN (
        'CASH_AT_COUNTER',
        'CARD_AT_COUNTER',
        'CARD_IN_APP',
        'APPLE_PAY',
        'GOOGLE_PAY'
    ));

-- ---------------------------------------------------------------------------
-- category.description (optional short text shown/edited in the barmaker UI)
-- ---------------------------------------------------------------------------
ALTER TABLE category ADD COLUMN description VARCHAR(255);

-- ---------------------------------------------------------------------------
-- cocktail.short_description (optional teaser shown on cards / edited in the UI)
-- ---------------------------------------------------------------------------
ALTER TABLE cocktail ADD COLUMN short_description VARCHAR(255);
