-- ==========================================================================
-- Le Bar'app - V7: use locally hosted photos for the demo cocktails.
--
-- Replaces the temporary / missing demonstration image URLs (Mojito, Piña
-- Colada, Sex on the Beach, Virgin Mojito were NULL; Cosmopolitan pointed at a
-- fake example.com URL) with real, free-licensed photographs downloaded into
-- the repository and served locally by the frontend at /images/cocktails/*.webp.
--
-- Data-only, additive and deterministic:
--   * Only the 5 ACTIVE seeded demo cocktails (ids 1..5) are updated; the
--     inactive cocktail (id 6) is never touched.
--   * Each UPDATE matches both the known seed id AND the expected name so an
--     unexpected row can never be silently modified.
--   * No schema change; V2 (and any earlier applied migration) is not edited.
--   * No external stock-image URL is stored — only local paths.
-- ==========================================================================

UPDATE cocktail SET image_url = '/images/cocktails/mojito.webp'
    WHERE id = 1 AND name = 'Mojito' AND active = TRUE;

UPDATE cocktail SET image_url = '/images/cocktails/cosmopolitan.webp'
    WHERE id = 2 AND name = 'Cosmopolitan' AND active = TRUE;

UPDATE cocktail SET image_url = '/images/cocktails/pina-colada.webp'
    WHERE id = 3 AND name = 'Piña Colada' AND active = TRUE;

UPDATE cocktail SET image_url = '/images/cocktails/sex-on-the-beach.webp'
    WHERE id = 4 AND name = 'Sex on the Beach' AND active = TRUE;

UPDATE cocktail SET image_url = '/images/cocktails/virgin-mojito.webp'
    WHERE id = 5 AND name = 'Virgin Mojito' AND active = TRUE;

-- Post-migration verification: exactly the five expected active demo cocktails
-- must now carry a local image path. Fail loudly otherwise (aborts the
-- migration transaction; nothing is silently left half-applied).
DO $$
DECLARE
    local_count integer;
BEGIN
    SELECT count(*) INTO local_count
    FROM cocktail
    WHERE active = TRUE
      AND image_url IN (
          '/images/cocktails/mojito.webp',
          '/images/cocktails/cosmopolitan.webp',
          '/images/cocktails/pina-colada.webp',
          '/images/cocktails/sex-on-the-beach.webp',
          '/images/cocktails/virgin-mojito.webp'
      );
    IF local_count <> 5 THEN
        RAISE EXCEPTION 'V7 expected 5 active demo cocktails with local images, found %', local_count;
    END IF;
END $$;
