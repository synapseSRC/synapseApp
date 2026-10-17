BEGIN;

-- A public quote post notifies the original author through the existing SHARE preference.
-- The quoted author is excluded from the quote author's generic NEW_POST fan-out below
-- so they receive one contextual notification instead of two.

CREATE OR REPLACE FUNCTION private.handle_new_post_follower_notifications()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
DECLARE
  v_name text;
  v_follower record;
  v_quoted_author text;
BEGIN
  IF NEW.in_reply_to_post_id IS NOT NULL THEN
    RETURN NEW;
  END IF;

  IF COALESCE(NEW.post_visibility, 'public') <> 'public' THEN
    RETURN NEW;
  END IF;

  IF NEW.quoted_post_id IS NOT NULL THEN
    SELECT p.author_uid
      INTO v_quoted_author
    FROM public.posts p
    WHERE p.id = NEW.quoted_post_id;
  END IF;

  SELECT COALESCE(u.display_name, u.username, 'Someone')
    INTO v_name
  FROM public.users u
  WHERE u.uid = NEW.author_uid;

  FOR v_follower IN
    SELECT f.follower_id
    FROM public.follows f
    WHERE f.following_id = NEW.author_uid
      AND f.follower_id <> NEW.author_uid
      AND (v_quoted_author IS NULL OR f.follower_id <> v_quoted_author)
  LOOP
    PERFORM private.create_user_notification(
      v_follower.follower_id,
      NEW.author_uid,
      'NEW_POST',
      'New post',
      COALESCE(v_name, 'Someone') || ' published a new post.',
      jsonb_build_object(
        'postId', NEW.id,
        'targetType', 'POST',
        'target_id', NEW.id
      )
    );
  END LOOP;

  RETURN NEW;
END;
$function$;

CREATE OR REPLACE FUNCTION private.handle_reshare_notification()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
DECLARE
  v_recipient text;
  v_name text;
BEGIN
  SELECT p.author_uid
    INTO v_recipient
  FROM public.posts p
  WHERE p.id = NEW.post_id;

  -- Ignore missing targets and self-reshares.
  IF v_recipient IS NULL OR v_recipient = NEW.user_id THEN
    RETURN NEW;
  END IF;

  SELECT COALESCE(u.display_name, u.username, 'Someone')
    INTO v_name
  FROM public.users u
  WHERE u.uid = NEW.user_id;

  PERFORM private.create_user_notification(
    v_recipient,
    NEW.user_id,
    'SHARE',
    'New repost',
    COALESCE(v_name, 'Someone') || ' reposted your post.',
    jsonb_build_object(
      'postId', NEW.post_id,
      'targetType', 'POST',
      'target_id', NEW.post_id,
      'reshareId', NEW.id::text,
      'shareType', 'REPOST'
    )
  );

  RETURN NEW;
END;
$function$;

CREATE OR REPLACE FUNCTION private.handle_quoted_post_notification()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
DECLARE
  v_recipient text;
  v_name text;
BEGIN
  IF NEW.quoted_post_id IS NULL OR NEW.in_reply_to_post_id IS NOT NULL THEN
    RETURN NEW;
  END IF;

  SELECT p.author_uid
    INTO v_recipient
  FROM public.posts p
  WHERE p.id = NEW.quoted_post_id;

  IF v_recipient IS NULL OR v_recipient = NEW.author_uid THEN
    RETURN NEW;
  END IF;

  SELECT COALESCE(u.display_name, u.username, 'Someone')
    INTO v_name
  FROM public.users u
  WHERE u.uid = NEW.author_uid;

  PERFORM private.create_user_notification(
    v_recipient,
    NEW.author_uid,
    'SHARE',
    'New quote post',
    COALESCE(v_name, 'Someone') || ' quoted your post.',
    jsonb_build_object(
      'postId', NEW.quoted_post_id,
      'targetType', 'POST',
      'target_id', NEW.quoted_post_id,
      'quotePostId', NEW.id,
      'shareType', 'QUOTE'
    )
  );

  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_notifications_reshare ON public.reshares;
CREATE TRIGGER trg_notifications_reshare
AFTER INSERT ON public.reshares
FOR EACH ROW
EXECUTE FUNCTION private.handle_reshare_notification();

DROP TRIGGER IF EXISTS trg_notifications_quoted_post ON public.posts;
CREATE TRIGGER trg_notifications_quoted_post
AFTER INSERT ON public.posts
FOR EACH ROW
EXECUTE FUNCTION private.handle_quoted_post_notification();

REVOKE ALL ON FUNCTION private.handle_new_post_follower_notifications() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION private.handle_reshare_notification() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION private.handle_quoted_post_notification() FROM PUBLIC, anon, authenticated;

COMMENT ON FUNCTION private.handle_reshare_notification()
IS 'Creates a SHARE notification for the original post author when another user reshares a post; push is delivered through the shared notification dispatcher.';
COMMENT ON FUNCTION private.handle_quoted_post_notification()
IS 'Creates a SHARE notification for the original post author when another user publishes a quote post; push is delivered through the shared notification dispatcher.';

COMMIT;
