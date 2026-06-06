-- Fix: posts.poll_options and posts.media_items must never be SQL NULL.
-- The Kotlin client calls .jsonArray on these fields; a NULL serialised as
-- JsonNull causes "Element class JsonNull is not a JsonArray".

-- 1. Backfill any existing NULLs to empty arrays.
UPDATE posts
SET
  poll_options = COALESCE(poll_options, '[]'::jsonb),
  media_items  = COALESCE(media_items,  '[]'::jsonb)
WHERE poll_options IS NULL
   OR media_items  IS NULL;

-- 2. Set column defaults so future inserts without these fields are safe.
ALTER TABLE posts
  ALTER COLUMN poll_options SET DEFAULT '[]'::jsonb,
  ALTER COLUMN poll_options SET NOT NULL,
  ALTER COLUMN media_items  SET DEFAULT '[]'::jsonb,
  ALTER COLUMN media_items  SET NOT NULL;
