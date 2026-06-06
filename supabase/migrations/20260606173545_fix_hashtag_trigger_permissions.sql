-- The sync_post_hashtags trigger fires on INSERT/UPDATE of posts.
-- It runs as the calling user (authenticated) who has no WRITE access
-- to hashtags or post_hashtags → "permission denied for table hashtags".
-- Fix: recreate the function with SECURITY DEFINER so it runs as the
-- owner (postgres) and grant SELECT/INSERT on both tables to authenticated.

CREATE OR REPLACE FUNCTION sync_post_hashtags()
RETURNS TRIGGER LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_tag      TEXT;
    v_hash_id  UUID;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        DELETE FROM post_hashtags WHERE post_id = NEW.id;
        UPDATE hashtags h
        SET usage_count = GREATEST(usage_count - 1, 0),
            updated_at  = NOW()
        WHERE h.tag IN (
            SELECT lower(m[1])
            FROM regexp_matches(OLD.post_text, '#([A-Za-z0-9_]+)', 'g') AS m
        );
    END IF;

    IF NEW.is_deleted = true OR NEW.post_text IS NULL THEN
        RETURN NEW;
    END IF;

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
