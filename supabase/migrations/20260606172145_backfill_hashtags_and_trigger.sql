-- ============================================================
-- Backfill hashtags + post_hashtags from existing post_text,
-- then add a trigger to keep them in sync on future writes.
-- ============================================================

-- 1. Upsert all unique hashtags extracted from existing posts.
INSERT INTO hashtags (id, tag, usage_count, created_at, updated_at)
SELECT
    gen_random_uuid(),
    lower(tag),
    COUNT(*) AS usage_count,
    NOW(),
    NOW()
FROM (
    SELECT DISTINCT
        p.id AS post_id,
        lower(regexp_replace(m[1], '^#', '')) AS tag
    FROM posts p,
         LATERAL regexp_matches(p.post_text, '#([A-Za-z0-9_]+)', 'g') AS m
    WHERE p.post_text IS NOT NULL
      AND p.is_deleted = false
) extracted
GROUP BY lower(tag)
ON CONFLICT (tag) DO UPDATE
    SET usage_count = EXCLUDED.usage_count,
        updated_at  = NOW();

-- 2. Backfill post_hashtags join table.
INSERT INTO post_hashtags (id, post_id, hashtag_id, created_at)
SELECT
    gen_random_uuid(),
    p.id,
    h.id,
    NOW()
FROM posts p,
     LATERAL regexp_matches(p.post_text, '#([A-Za-z0-9_]+)', 'g') AS m
JOIN hashtags h ON h.tag = lower(m[1])
WHERE p.post_text IS NOT NULL
  AND p.is_deleted = false
ON CONFLICT DO NOTHING;

-- 3. Unique constraint on hashtags.tag (needed for ON CONFLICT above).
--    Only add if it doesn't already exist.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'hashtags_tag_key' AND conrelid = 'hashtags'::regclass
    ) THEN
        ALTER TABLE hashtags ADD CONSTRAINT hashtags_tag_key UNIQUE (tag);
    END IF;
END$$;

-- 4. Unique constraint on post_hashtags(post_id, hashtag_id).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'post_hashtags_post_id_hashtag_id_key' AND conrelid = 'post_hashtags'::regclass
    ) THEN
        ALTER TABLE post_hashtags ADD CONSTRAINT post_hashtags_post_id_hashtag_id_key UNIQUE (post_id, hashtag_id);
    END IF;
END$$;

-- 5. Trigger function: sync hashtags on INSERT or UPDATE of post_text.
CREATE OR REPLACE FUNCTION sync_post_hashtags()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_tag      TEXT;
    v_hash_id  UUID;
BEGIN
    -- Remove old associations when post_text changes or post is deleted.
    IF TG_OP = 'UPDATE' THEN
        DELETE FROM post_hashtags WHERE post_id = NEW.id;
        -- Decrement usage counts for tags that belonged to this post.
        UPDATE hashtags h
        SET usage_count = GREATEST(usage_count - 1, 0),
            updated_at  = NOW()
        WHERE h.tag IN (
            SELECT lower(m[1])
            FROM regexp_matches(OLD.post_text, '#([A-Za-z0-9_]+)', 'g') AS m
        );
    END IF;

    -- Skip if deleted or no text.
    IF NEW.is_deleted = true OR NEW.post_text IS NULL THEN
        RETURN NEW;
    END IF;

    -- Insert/update each hashtag found in the new text.
    FOR v_tag IN
        SELECT DISTINCT lower(m[1])
        FROM regexp_matches(NEW.post_text, '#([A-Za-z0-9_]+)', 'g') AS m
    LOOP
        INSERT INTO hashtags (id, tag, usage_count, created_at, updated_at)
        VALUES (gen_random_uuid(), v_tag, 1, NOW(), NOW())
        ON CONFLICT (tag) DO UPDATE
            SET usage_count = hashtags.usage_count + 1,
                updated_at  = NOW()
        RETURNING id INTO v_hash_id;

        -- If ON CONFLICT branch ran, id wasn't returned — fetch it.
        IF v_hash_id IS NULL THEN
            SELECT id INTO v_hash_id FROM hashtags WHERE tag = v_tag;
        END IF;

        INSERT INTO post_hashtags (id, post_id, hashtag_id, created_at)
        VALUES (gen_random_uuid(), NEW.id, v_hash_id, NOW())
        ON CONFLICT DO NOTHING;
    END LOOP;

    RETURN NEW;
END;
$$;

-- 6. Attach trigger to posts table.
DROP TRIGGER IF EXISTS trg_sync_post_hashtags ON posts;
CREATE TRIGGER trg_sync_post_hashtags
    AFTER INSERT OR UPDATE OF post_text, is_deleted
    ON posts
    FOR EACH ROW EXECUTE FUNCTION sync_post_hashtags();
