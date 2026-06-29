-- ==========================================================================
-- Le Bar'app - V3: insert local demo barmaker.
-- This migration inserts ONE demo account with a real BCrypt hash (no
-- plaintext password is stored). It is deterministic and safe against
-- duplicate insertion thanks to the ON CONFLICT guard on the unique
-- functional index uk_app_user_username_lower.
-- ==========================================================================
--
-- Demo credentials (DEVELOPMENT / DEMO ONLY — do not use in production):
--   username: barmaker
--   password: barapp-demo-2024
--
-- The BCrypt hash below was generated with BCryptPasswordEncoder strength 10.
-- It is NOT the plaintext password.
-- ==========================================================================
INSERT INTO app_user (username, password_hash, display_name, role, active)
VALUES (
    'barmaker',
    '$2a$10$w3RNYPGZjHibEiVhIhh2z./crcQfr2imdJb/Rtj68pTXaw/JBt82W',
    'Barman principal',
    'BARMAKER',
    TRUE
)
ON CONFLICT ((LOWER(username)))
DO NOTHING;
