BEGIN;

-- Persist valid text mentions through the existing mentions notification trigger.
CREATE OR REPLACE FUNCTION private.sync_post_mentions()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
DECLARE
  v_mentioned_user_id text;
BEGIN
  IF TG_OP = 'UPDATE' THEN
    DELETE FROM public.mentions m
    WHERE m.post_id = NEW.id
      AND NOT EXISTS (
        SELECT 1 FROM regexp_matches(COALESCE(NEW.post_text, ''), '(^|[^[:alnum:]_])@([[:alnum:]_]+)', 'g') AS token(parts)
        JOIN public.users mentioned ON lower(mentioned.username) = lower(token.parts[2])
        WHERE mentioned.uid = m.user_id AND mentioned.uid <> NEW.author_uid
      );
  END IF;
  FOR v_mentioned_user_id IN
    SELECT DISTINCT mentioned.uid
    FROM regexp_matches(COALESCE(NEW.post_text, ''), '(^|[^[:alnum:]_])@([[:alnum:]_]+)', 'g') AS token(parts)
    JOIN public.users mentioned ON lower(mentioned.username) = lower(token.parts[2])
    WHERE mentioned.uid <> NEW.author_uid
  LOOP
    INSERT INTO public.mentions (post_id, user_id)
    SELECT NEW.id, v_mentioned_user_id WHERE NOT EXISTS (
      SELECT 1 FROM public.mentions existing WHERE existing.post_id = NEW.id AND existing.user_id = v_mentioned_user_id
    );
  END LOOP;
  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_sync_post_mentions ON public.posts;
CREATE TRIGGER trg_sync_post_mentions
AFTER INSERT OR UPDATE OF post_text, author_uid ON public.posts
FOR EACH ROW EXECUTE FUNCTION private.sync_post_mentions();

CREATE OR REPLACE FUNCTION private.handle_post_reply_notification()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
DECLARE
  v_parent_author text; v_root_author text; v_name text; v_type text; v_body text; v_data jsonb;
  v_syra_uid text; v_is_syra_response boolean;
BEGIN
  IF NEW.in_reply_to_post_id IS NULL THEN RETURN NEW; END IF;
  SELECT p.author_uid INTO v_parent_author FROM public.posts p WHERE p.id = NEW.in_reply_to_post_id;
  SELECT COALESCE(u.display_name, u.username, 'Someone') INTO v_name FROM public.users u WHERE u.uid = NEW.author_uid;
  SELECT u.uid INTO v_syra_uid FROM public.users u WHERE lower(COALESCE(u.username, '')) = 'syra' LIMIT 1;
  v_is_syra_response := v_syra_uid IS NOT NULL AND NEW.author_uid = v_syra_uid AND EXISTS (
    SELECT 1 FROM public.posts parent WHERE parent.id = NEW.in_reply_to_post_id
      AND COALESCE(parent.post_text, '') ~* '(^|[^[:alnum:]_])@syra([^[:alnum:]_]|$)'
  );
  v_type := CASE WHEN v_is_syra_response THEN 'NEW_REPLY' WHEN EXISTS (
    SELECT 1 FROM public.posts parent WHERE parent.id = NEW.in_reply_to_post_id AND parent.in_reply_to_post_id IS NULL
  ) THEN 'NEW_COMMENT' ELSE 'NEW_REPLY' END;
  v_body := CASE WHEN v_is_syra_response THEN COALESCE(v_name, 'Someone') || ' replied to your post.'
    WHEN v_type = 'NEW_COMMENT' THEN COALESCE(v_name, 'Someone') || ' commented on your post.'
    ELSE COALESCE(v_name, 'Someone') || ' replied to your comment.' END;
  v_data := jsonb_build_object('postId', NEW.root_post_id, 'commentId', NEW.id::text, 'targetType', 'POST_COMMENT', 'target_id', NEW.id::text);
  PERFORM private.create_user_notification(v_parent_author, NEW.author_uid, v_type, CASE WHEN v_type = 'NEW_COMMENT' THEN 'New comment' ELSE 'New reply' END, v_body, v_data);
  IF NEW.root_post_id IS NOT NULL THEN
    SELECT p.author_uid INTO v_root_author FROM public.posts p WHERE p.id = NEW.root_post_id;
    IF v_root_author IS NOT NULL AND v_root_author <> COALESCE(v_parent_author, '') AND v_root_author <> NEW.author_uid THEN
      PERFORM private.create_user_notification(v_root_author, NEW.author_uid, 'NEW_REPLY', 'New reply', COALESCE(v_name, 'Someone') || ' replied in a thread you follow.', v_data);
    END IF;
  END IF;
  RETURN NEW;
END;
$function$;

COMMIT;
