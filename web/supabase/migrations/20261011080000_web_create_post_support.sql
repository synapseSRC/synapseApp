-- Web create-post support: enforce true private visibility and publish scheduled posts server-side.
-- The source is kept with the web app; the same statements were applied to the active Supabase project.

CREATE OR REPLACE FUNCTION private.can_view_post(p_post_id text, p_viewer_uid text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
  WITH RECURSIVE refs(post_id, visited) AS (
    SELECT p_post_id, ARRAY[p_post_id]::text[] WHERE p_post_id IS NOT NULL
    UNION ALL
    SELECT link.parent_id, refs.visited || link.parent_id
    FROM refs
    JOIN public.posts p ON p.id = refs.post_id
    CROSS JOIN LATERAL (VALUES (p.in_reply_to_post_id), (p.quoted_post_id), (p.root_post_id)) AS link(parent_id)
    WHERE link.parent_id IS NOT NULL AND NOT link.parent_id = ANY(refs.visited)
  )
  SELECT EXISTS (SELECT 1 FROM refs)
     AND NOT EXISTS (
       SELECT 1
       FROM refs r
       JOIN public.posts p ON p.id = r.post_id
       WHERE NOT private.can_view_profile(p.author_uid, p_viewer_uid)
          OR NOT (
            p.post_visibility IS NULL
            OR p.post_visibility = 'public'
            OR (p_viewer_uid IS NOT NULL AND p.author_uid = p_viewer_uid)
            OR (
              p.post_visibility = 'followers'
              AND p_viewer_uid IS NOT NULL
              AND EXISTS (
                SELECT 1 FROM public.follows f
                WHERE f.follower_id = p_viewer_uid AND f.following_id = p.author_uid
              )
            )
          )
     );
$function$;

CREATE TABLE IF NOT EXISTS public.scheduled_posts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_uid text NOT NULL,
  scheduled_at timestamptz NOT NULL,
  payload jsonb NOT NULL,
  status text NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'published', 'failed', 'cancelled')),
  published_post_id text REFERENCES public.posts(id) ON DELETE SET NULL,
  last_error text,
  created_at timestamptz NOT NULL DEFAULT now(),
  published_at timestamptz,
  CONSTRAINT scheduled_posts_future_time CHECK (scheduled_at > created_at)
);

ALTER TABLE public.scheduled_posts ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS scheduled_posts_owner_manage ON public.scheduled_posts;
DROP POLICY IF EXISTS scheduled_posts_owner_read ON public.scheduled_posts;
DROP POLICY IF EXISTS scheduled_posts_owner_insert ON public.scheduled_posts;
DROP POLICY IF EXISTS scheduled_posts_owner_cancel ON public.scheduled_posts;
CREATE POLICY scheduled_posts_owner_read
  ON public.scheduled_posts FOR SELECT TO authenticated
  USING (owner_uid = auth.uid()::text);
CREATE POLICY scheduled_posts_owner_insert
  ON public.scheduled_posts FOR INSERT TO authenticated
  WITH CHECK (owner_uid = auth.uid()::text);
CREATE POLICY scheduled_posts_owner_cancel
  ON public.scheduled_posts FOR DELETE TO authenticated
  USING (owner_uid = auth.uid()::text AND status = 'pending');
REVOKE ALL ON public.scheduled_posts FROM PUBLIC, anon, authenticated;
GRANT SELECT, INSERT, DELETE ON public.scheduled_posts TO authenticated;

CREATE OR REPLACE FUNCTION public.publish_due_scheduled_posts()
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
DECLARE
  scheduled_row public.scheduled_posts%ROWTYPE;
  post_payload jsonb;
  root_post_id text;
  previous_post_id text;
  thread_body text;
  processed_count integer := 0;
  publish_time timestamptz;
BEGIN
  FOR scheduled_row IN
    SELECT *
    FROM public.scheduled_posts
    WHERE status = 'pending' AND scheduled_at <= clock_timestamp()
    ORDER BY scheduled_at, created_at
    LIMIT 50
    FOR UPDATE SKIP LOCKED
  LOOP
    BEGIN
      post_payload := scheduled_row.payload;
      publish_time := clock_timestamp();

      IF jsonb_typeof(post_payload) <> 'object' THEN
        RAISE EXCEPTION 'Scheduled post payload must be an object';
      END IF;
      IF COALESCE(post_payload->>'post_visibility', 'public') NOT IN ('public', 'followers', 'private') THEN
        RAISE EXCEPTION 'Unsupported post visibility';
      END IF;

      INSERT INTO public.posts (
        author_uid, post_text, post_image, media_items, post_type, timestamp, publish_date, is_deleted,
        post_visibility, has_location, location_name, location_address, location_latitude, location_longitude,
        location_place_id, has_poll, poll_question, poll_options, poll_end_time, poll_allow_multiple,
        post_hide_views_count, post_hide_like_count, post_hide_comments_count, post_disable_comments,
        youtube_url, metadata, reply_to_usernames
      ) VALUES (
        scheduled_row.owner_uid,
        NULLIF(post_payload->>'post_text', ''),
        NULLIF(post_payload->>'post_image', ''),
        COALESCE(post_payload->'media_items', '[]'::jsonb),
        COALESCE(NULLIF(post_payload->>'post_type', ''), 'TEXT'),
        floor(extract(epoch FROM publish_time) * 1000)::bigint,
        publish_time::text,
        false,
        COALESCE(NULLIF(post_payload->>'post_visibility', ''), 'public'),
        COALESCE(NULLIF(post_payload->>'has_location', '')::boolean, false),
        NULLIF(post_payload->>'location_name', ''),
        NULLIF(post_payload->>'location_address', ''),
        NULLIF(post_payload->>'location_latitude', '')::double precision,
        NULLIF(post_payload->>'location_longitude', '')::double precision,
        NULLIF(post_payload->>'location_place_id', ''),
        COALESCE(NULLIF(post_payload->>'has_poll', '')::boolean, false),
        NULLIF(post_payload->>'poll_question', ''),
        COALESCE(post_payload->'poll_options', '[]'::jsonb),
        NULLIF(post_payload->>'poll_end_time', ''),
        COALESCE(NULLIF(post_payload->>'poll_allow_multiple', '')::boolean, false),
        COALESCE(NULLIF(post_payload->>'post_hide_views_count', ''), 'false'),
        COALESCE(NULLIF(post_payload->>'post_hide_like_count', ''), 'false'),
        COALESCE(NULLIF(post_payload->>'post_hide_comments_count', ''), 'false'),
        COALESCE(NULLIF(post_payload->>'post_disable_comments', ''), 'false'),
        NULLIF(post_payload->>'youtube_url', ''),
        COALESCE(post_payload->'metadata', '{}'::jsonb),
        '[]'::jsonb
      ) RETURNING id INTO root_post_id;

      previous_post_id := root_post_id;
      FOR thread_body IN
        SELECT jsonb_array_elements_text(
          CASE WHEN jsonb_typeof(post_payload->'thread_posts') = 'array'
            THEN post_payload->'thread_posts' ELSE '[]'::jsonb END
        )
      LOOP
        IF NULLIF(btrim(thread_body), '') IS NOT NULL THEN
          publish_time := clock_timestamp();
          INSERT INTO public.posts (
            author_uid, post_text, post_image, media_items, post_type, timestamp, publish_date, is_deleted,
            post_visibility, has_location, has_poll, poll_options, poll_allow_multiple,
            post_hide_views_count, post_hide_like_count, post_hide_comments_count, post_disable_comments,
            in_reply_to_post_id, root_post_id, reply_to_usernames
          ) VALUES (
            scheduled_row.owner_uid, btrim(thread_body), NULL, '[]'::jsonb, 'TEXT',
            floor(extract(epoch FROM publish_time) * 1000)::bigint, publish_time::text, false,
            COALESCE(NULLIF(post_payload->>'post_visibility', ''), 'public'), false, false, '[]'::jsonb, false,
            COALESCE(NULLIF(post_payload->>'post_hide_views_count', ''), 'false'),
            COALESCE(NULLIF(post_payload->>'post_hide_like_count', ''), 'false'),
            COALESCE(NULLIF(post_payload->>'post_hide_comments_count', ''), 'false'),
            COALESCE(NULLIF(post_payload->>'post_disable_comments', ''), 'false'),
            previous_post_id, root_post_id, '[]'::jsonb
          ) RETURNING id INTO previous_post_id;
        END IF;
      END LOOP;

      UPDATE public.scheduled_posts
      SET status = 'published', published_post_id = root_post_id, published_at = publish_time, last_error = NULL
      WHERE id = scheduled_row.id;
      processed_count := processed_count + 1;
    EXCEPTION WHEN OTHERS THEN
      UPDATE public.scheduled_posts
      SET status = 'failed', last_error = left(SQLERRM, 1000)
      WHERE id = scheduled_row.id;
    END;
  END LOOP;
  RETURN processed_count;
END;
$function$;

REVOKE ALL ON FUNCTION public.publish_due_scheduled_posts() FROM PUBLIC, anon, authenticated;

DO $block$
DECLARE
  existing_job_id bigint;
BEGIN
  SELECT jobid INTO existing_job_id FROM cron.job WHERE jobname = 'synapse-publish-scheduled-posts';
  IF existing_job_id IS NOT NULL THEN
    PERFORM cron.unschedule(existing_job_id);
  END IF;
  PERFORM cron.schedule(
    'synapse-publish-scheduled-posts',
    '* * * * *',
    'SELECT public.publish_due_scheduled_posts();'
  );
END;
$block$;
