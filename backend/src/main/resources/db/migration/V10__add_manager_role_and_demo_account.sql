-- ==========================================================================
-- Le Bar'app - V10: introduce the MANAGER role and a demo manager account.
--
-- This migration adds the second (and final) authenticated staff role,
-- MANAGER, alongside the existing BARMAKER role. A manager is an elevated
-- barmaker: it keeps every barmaker capability and additionally administers
-- staff accounts (/api/bar/users/**).
--
-- No historical migration (V1-V9) is modified. The existing demo barmaker
-- account is left completely untouched (same username, password and role) so
-- that authorization differences between a regular barmaker and a manager can
-- be tested side by side.
-- ==========================================================================

-- --------------------------------------------------------------------------
-- 1. Widen the role check constraint to accept BARMAKER and MANAGER.
--    The column default stays 'BARMAKER'.
-- --------------------------------------------------------------------------
ALTER TABLE app_user
    DROP CONSTRAINT ck_app_user_role;

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_role
    CHECK (role IN ('BARMAKER', 'MANAGER'));

-- --------------------------------------------------------------------------
-- 2. Seed one deterministic development manager account.
--
--    Demo credentials (DEVELOPMENT / DEMO ONLY — do not use in production):
--      username:     manager
--      password:     manager-test
--      display name: Manager du bar
--      role:         MANAGER
--
--    The value below is a real BCrypt hash (strength 10) of "manager-test".
--    The plaintext password is NEVER stored. The ON CONFLICT guard on the
--    case-insensitive functional unique index makes this insert idempotent and
--    prevents silently overwriting any unrelated pre-existing "manager" account.
-- --------------------------------------------------------------------------
INSERT INTO app_user (username, password_hash, display_name, role, active)
VALUES (
    'manager',
    '$2a$10$MtBWUqwWb.YWQMZ6rZxDjOxqSmZ52LyGnn.9wi/EDR8aAjOTxgbTa',
    'Manager du bar',
    'MANAGER',
    TRUE
)
ON CONFLICT ((LOWER(username)))
DO NOTHING;
