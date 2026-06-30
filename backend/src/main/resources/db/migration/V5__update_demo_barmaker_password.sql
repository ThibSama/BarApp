-- ==========================================================================
-- Le Bar'app - V5: update the local demo barmaker password.
--
-- The development/demo credential password becomes "barmaker-test". This
-- migration is idempotent and works for BOTH cases:
--   * a clean database that just ran V3 (account already present);
--   * an upgraded database whose V3 inserted the previous demo password.
--
-- It UPDATEs the existing case-insensitive "barmaker" account in place, and
-- also INSERTs the account if it is somehow missing, so every database ends up
-- with the new password regardless of starting state.
--
-- Demo credentials (DEVELOPMENT / DEMO ONLY — do not use in production):
--   username: barmaker
--   password: barmaker-test
--
-- The value below is a real BCrypt hash (strength 10) of "barmaker-test".
-- The plaintext password is NEVER stored.
-- ==========================================================================

-- Upgrade path: rotate the password of the existing demo account.
UPDATE app_user
SET password_hash = '$2a$10$.J1SmmZCYSJBflqdFRnQieLCiqpw.kK6rHVpJ96IOqWDm1fmaylrm',
    display_name  = 'Barman principal',
    role          = 'BARMAKER',
    active        = TRUE,
    updated_at    = CURRENT_TIMESTAMP
WHERE LOWER(username) = LOWER('barmaker');

-- Clean/missing path: insert it if the account does not exist yet.
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
