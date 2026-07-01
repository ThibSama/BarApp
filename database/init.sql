-- ==========================================================================
-- Le Bar'app - consolidated reference schema + demo catalog.
-- Generated to mirror the effective Flyway migrations:
--   backend/src/main/resources/db/migration/V1__create_schema.sql
--   backend/src/main/resources/db/migration/V2__insert_demo_catalog.sql
-- This file is the exam-delivery reference; the application itself is
-- migrated by Flyway, not by this script.
-- ==========================================================================

-- Le Bar'app - initial schema.
-- pgcrypto provides gen_random_uuid() for UUID primary keys.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- app_user (barmaker accounts)
-- ---------------------------------------------------------------------------
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(80)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(120) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'BARMAKER',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_app_user_role CHECK (role IN ('BARMAKER'))
);

CREATE UNIQUE INDEX uk_app_user_username_lower
    ON app_user (LOWER(username));

-- ---------------------------------------------------------------------------
-- category
-- ---------------------------------------------------------------------------
CREATE TABLE category (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(255),                 -- V4
    display_order INTEGER      NOT NULL DEFAULT 0,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_category_display_order CHECK (display_order >= 0)
);

CREATE UNIQUE INDEX uk_category_name_lower
    ON category (LOWER(name));

-- ---------------------------------------------------------------------------
-- ingredient
-- ---------------------------------------------------------------------------
CREATE TABLE ingredient (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_ingredient_name_lower
    ON ingredient (LOWER(name));

-- ---------------------------------------------------------------------------
-- cocktail
-- ---------------------------------------------------------------------------
CREATE TABLE cocktail (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT       NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT         NOT NULL,
    short_description VARCHAR(255),              -- V4
    image_url   VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cocktail_category
        FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uk_cocktail_category_name_lower
    ON cocktail (category_id, LOWER(name));

CREATE INDEX idx_cocktail_category_active
    ON cocktail (category_id, active);

-- ---------------------------------------------------------------------------
-- cocktail_ingredient (association entity: carries display_order + quantity)
-- ---------------------------------------------------------------------------
CREATE TABLE cocktail_ingredient (
    cocktail_id    BIGINT  NOT NULL,
    ingredient_id  BIGINT  NOT NULL,
    display_order  INTEGER NOT NULL DEFAULT 0,
    quantity_label VARCHAR(80),
    PRIMARY KEY (cocktail_id, ingredient_id),
    CONSTRAINT fk_cocktail_ingredient_cocktail
        FOREIGN KEY (cocktail_id) REFERENCES cocktail (id) ON DELETE CASCADE,
    CONSTRAINT fk_cocktail_ingredient_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredient (id) ON DELETE RESTRICT,
    CONSTRAINT ck_cocktail_ingredient_display_order CHECK (display_order >= 0)
);

-- ---------------------------------------------------------------------------
-- cocktail_price
-- ---------------------------------------------------------------------------
CREATE TABLE cocktail_price (
    id          BIGSERIAL PRIMARY KEY,
    cocktail_id BIGINT        NOT NULL,
    size        VARCHAR(1)    NOT NULL,
    price       NUMERIC(10,2) NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cocktail_price_cocktail
        FOREIGN KEY (cocktail_id) REFERENCES cocktail (id) ON DELETE CASCADE,
    CONSTRAINT uk_cocktail_price_size UNIQUE (cocktail_id, size),
    CONSTRAINT ck_cocktail_price_size CHECK (size IN ('S', 'M', 'L')),
    CONSTRAINT ck_cocktail_price_positive CHECK (price > 0)
);

-- ---------------------------------------------------------------------------
-- customer_order (out of scope for this stage; schema only)
-- ---------------------------------------------------------------------------
CREATE TABLE customer_order (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_code  VARCHAR(8)    NOT NULL UNIQUE,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ORDERED',
    total_amount NUMERIC(10,2) NOT NULL,
    table_number INTEGER       NOT NULL,        -- V4
    payment_method VARCHAR(30) NOT NULL,        -- V4
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_customer_order_status
        CHECK (status IN ('ORDERED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT ck_customer_order_total CHECK (total_amount >= 0),
    CONSTRAINT ck_customer_order_table_number CHECK (table_number BETWEEN 1 AND 999),  -- V4
    CONSTRAINT ck_customer_order_payment_method CHECK (payment_method IN (            -- V4
        'CASH_AT_COUNTER', 'CARD_AT_COUNTER', 'CARD_IN_APP', 'APPLE_PAY', 'GOOGLE_PAY'
    )),
    CONSTRAINT ck_customer_order_completed_at CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR
        (status <> 'COMPLETED' AND completed_at IS NULL)
    )
);

CREATE INDEX idx_customer_order_status_created_at
    ON customer_order (status, created_at);

-- ---------------------------------------------------------------------------
-- order_item (out of scope for this stage; schema only)
-- cocktail_id is nullable so ON DELETE SET NULL preserves the snapshot fields.
-- ---------------------------------------------------------------------------
CREATE TABLE order_item (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id               UUID          NOT NULL,
    cocktail_id            BIGINT,
    cocktail_name_snapshot VARCHAR(150)  NOT NULL,
    size                   VARCHAR(1)    NOT NULL,
    unit_price_snapshot    NUMERIC(10,2) NOT NULL,
    preparation_status     VARCHAR(40)   NOT NULL DEFAULT 'PREPARATION_INGREDIENTS',
    sequence_number        INTEGER       NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at           TIMESTAMPTZ,
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id) REFERENCES customer_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_cocktail
        FOREIGN KEY (cocktail_id) REFERENCES cocktail (id) ON DELETE SET NULL,
    CONSTRAINT uk_order_item_sequence UNIQUE (order_id, sequence_number),
    CONSTRAINT ck_order_item_size CHECK (size IN ('S', 'M', 'L')),
    CONSTRAINT ck_order_item_price CHECK (unit_price_snapshot > 0),
    CONSTRAINT ck_order_item_sequence CHECK (sequence_number > 0),
    CONSTRAINT ck_order_item_status CHECK (
        preparation_status IN (
            'PREPARATION_INGREDIENTS',
            'ASSEMBLY',
            'DRESSING',
            'COMPLETED'
        )
    ),
    CONSTRAINT ck_order_item_completed_at CHECK (
        (preparation_status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR
        (preparation_status <> 'COMPLETED' AND completed_at IS NULL)
    )
);

CREATE INDEX idx_order_item_order_id
    ON order_item (order_id);

CREATE INDEX idx_order_item_preparation_status
    ON order_item (preparation_status);

-- ==========================================================================
-- Demo catalog
-- ==========================================================================

-- Demo catalog for Le Bar'app.
-- Deterministic ids are used for catalog entities to keep the seed readable and
-- the relationships explicit. Sequences are realigned at the end so future
-- generated inserts continue past the seeded rows.
--
-- A few INACTIVE rows are included on purpose (an inactive category + cocktail,
-- an inactive ingredient association, an inactive price) so that the public menu
-- filtering can be verified at runtime. They MUST NOT appear in GET /api/menu.
--
-- app_user now contains one demo barmaker (see V2 section below). The
-- password is stored as a BCrypt hash; the plaintext password is NEVER seeded.
--
-- Demo credentials (DEVELOPMENT ONLY):
--   username: barmaker
--   password: barapp-demo-2024

-- ---------------------------------------------------------------------------
-- Categories (3 active + 1 inactive)
-- ---------------------------------------------------------------------------
INSERT INTO category (id, name, display_order, active) VALUES
    (1, 'Classiques', 1, TRUE),
    (2, 'Tropicaux',  2, TRUE),
    (3, 'Mocktails',  3, TRUE),
    (4, 'Promotions expirées', 99, FALSE);

-- ---------------------------------------------------------------------------
-- Ingredients (12 active + 1 inactive)
-- ---------------------------------------------------------------------------
INSERT INTO ingredient (id, name, active) VALUES
    (1,  'Rhum blanc',       TRUE),
    (2,  'Citron vert',      TRUE),
    (3,  'Menthe fraîche',   TRUE),
    (4,  'Eau gazeuse',      TRUE),
    (5,  'Sucre de canne',   TRUE),
    (6,  'Vodka',            TRUE),
    (7,  'Jus de cranberry', TRUE),
    (8,  'Triple sec',       TRUE),
    (9,  'Jus d''ananas',    TRUE),
    (10, 'Crème de coco',    TRUE),
    (11, 'Sirop de grenadine', TRUE),
    (12, 'Limonade',         TRUE),
    (13, 'Colorant de test', FALSE);

-- ---------------------------------------------------------------------------
-- Cocktails (5 active + 1 inactive)
-- ---------------------------------------------------------------------------
INSERT INTO cocktail (id, category_id, name, description, image_url, active) VALUES
    (1, 1, 'Mojito',
        'Rhum blanc, citron vert, menthe fraîche et eau gazeuse, le grand classique rafraîchissant.',
        NULL, TRUE),
    (2, 1, 'Cosmopolitan',
        'Vodka, triple sec et jus de cranberry relevés d''une pointe de citron vert.',
        'https://example.com/img/cosmopolitan.jpg', TRUE),
    (3, 2, 'Piña Colada',
        'Rhum blanc, jus d''ananas et crème de coco pour une évasion tropicale onctueuse.',
        NULL, TRUE),
    (4, 2, 'Sex on the Beach',
        'Vodka, jus de cranberry et jus d''ananas, fruité et estival.',
        NULL, TRUE),
    (5, 3, 'Virgin Mojito',
        'La version sans alcool du Mojito : citron vert, menthe, limonade et eau gazeuse.',
        NULL, TRUE),
    (6, 4, 'Cocktail retiré',
        'Cocktail désactivé : ne doit jamais apparaître dans la carte.',
        NULL, FALSE);

-- ---------------------------------------------------------------------------
-- Cocktail / ingredient associations
-- ---------------------------------------------------------------------------
INSERT INTO cocktail_ingredient (cocktail_id, ingredient_id, display_order, quantity_label) VALUES
    -- Mojito
    (1, 1, 1, '4 cl'),
    (1, 2, 2, '1/2 pièce'),
    (1, 3, 3, '6 feuilles'),
    (1, 5, 4, '2 cl'),
    (1, 4, 5, 'Allongé'),
    (1, 13, 6, 'Trace'),          -- inactive ingredient: must be filtered out
    -- Cosmopolitan
    (2, 6, 1, '4 cl'),
    (2, 8, 2, '2 cl'),
    (2, 7, 3, '3 cl'),
    (2, 2, 4, '1 cl'),
    -- Piña Colada
    (3, 1, 1, '4 cl'),
    (3, 9, 2, '8 cl'),
    (3, 10, 3, '4 cl'),
    -- Sex on the Beach
    (4, 6, 1, '4 cl'),
    (4, 7, 2, '6 cl'),
    (4, 9, 3, '6 cl'),
    -- Virgin Mojito
    (5, 2, 1, '1/2 pièce'),
    (5, 3, 2, '6 feuilles'),
    (5, 12, 3, '8 cl'),
    (5, 4, 4, 'Allongé'),
    (5, 5, 5, '2 cl');

-- ---------------------------------------------------------------------------
-- Prices (id auto-generated). Several cocktails expose S, M and L.
-- ---------------------------------------------------------------------------
INSERT INTO cocktail_price (cocktail_id, size, price, active) VALUES
    -- Mojito: S/M/L
    (1, 'S', 8.50,  TRUE),
    (1, 'M', 10.50, TRUE),
    (1, 'L', 12.50, TRUE),
    -- Cosmopolitan: S/M active, L inactive (must be filtered out)
    (2, 'S', 9.00,  TRUE),
    (2, 'M', 11.00, TRUE),
    (2, 'L', 13.00, FALSE),
    -- Piña Colada: S/M/L
    (3, 'S', 9.50,  TRUE),
    (3, 'M', 11.50, TRUE),
    (3, 'L', 13.50, TRUE),
    -- Sex on the Beach: M/L
    (4, 'M', 11.00, TRUE),
    (4, 'L', 13.00, TRUE),
    -- Virgin Mojito: S/M/L
    (5, 'S', 6.50,  TRUE),
    (5, 'M', 8.00,  TRUE),
    (5, 'L', 9.50,  TRUE);

-- ---------------------------------------------------------------------------
-- Realign sequences past the explicitly seeded ids.
-- ---------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('category', 'id'),   (SELECT MAX(id) FROM category));
SELECT setval(pg_get_serial_sequence('ingredient', 'id'), (SELECT MAX(id) FROM ingredient));
SELECT setval(pg_get_serial_sequence('cocktail', 'id'),   (SELECT MAX(id) FROM cocktail));

-- ==========================================================================
-- Demo barmaker (V3 + V5 password rotation)
-- ==========================================================================
-- One local demo barmaker account. The password is stored as a BCrypt hash
-- (strength 10); the plaintext password is NEVER stored. The hash below is the
-- V5 value (password rotated to "barmaker-test").
-- Demo credentials (DEVELOPMENT / DEMO ONLY — do not use in production):
--   username: barmaker
--   password: barmaker-test
INSERT INTO app_user (username, password_hash, display_name, role, active)
VALUES (
    'barmaker',
    '$2a$10$.J1SmmZCYSJBflqdFRnQieLCiqpw.kK6rHVpJ96IOqWDm1fmaylrm',
    'Barman principal',
    'BARMAKER',
    TRUE
)
ON CONFLICT ((LOWER(username)))
DO NOTHING;
