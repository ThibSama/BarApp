-- Demo catalog for Le Bar'app.
-- Deterministic ids are used for catalog entities to keep the seed readable and
-- the relationships explicit. Sequences are realigned at the end so future
-- generated inserts continue past the seeded rows.
--
-- A few INACTIVE rows are included on purpose (an inactive category + cocktail,
-- an inactive ingredient association, an inactive price) so that the public menu
-- filtering can be verified at runtime. They MUST NOT appear in GET /api/menu.
--
-- app_user is intentionally left empty: authentication is out of scope and no
-- plaintext password is seeded. Future barmaker credentials will be created with
-- a BCrypt hash once Spring Security is introduced.

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
