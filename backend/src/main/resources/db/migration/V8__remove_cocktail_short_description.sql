-- ==========================================================================
-- Le Bar'app - V8: remove the redundant cocktail short_description column.
--
-- The catalogue is simplified to a single required cocktail description
-- (`cocktail.description`). The `short_description` column added by V4 is
-- intentionally dropped; the values it held are discarded on purpose (they were
-- never authoritative and are not merged into `description`).
--
-- Strict DROP COLUMN (no IF EXISTS): after V4 the column MUST exist, so a
-- missing column should surface an inconsistent migration history rather than be
-- silently ignored. V4 (and every other applied migration) is left untouched.
-- No cocktail rows, names, descriptions, images, ingredients, prices, categories
-- or active states are modified.
-- ==========================================================================

ALTER TABLE cocktail
    DROP COLUMN short_description;
