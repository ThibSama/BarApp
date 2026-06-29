-- Le Bar'app - initial schema.
-- pgcrypto provides gen_random_uuid() for UUID primary keys.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- app_user (barmaker accounts; authentication implemented in a later stage)
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
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_customer_order_status
        CHECK (status IN ('ORDERED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT ck_customer_order_total CHECK (total_amount >= 0),
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
