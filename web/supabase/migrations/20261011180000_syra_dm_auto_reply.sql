CREATE OR REPLACE FUNCTION public.invoke_syra_dm_reply()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $function$
DECLARE
  syra_auth_id text;
  syra_uid text;
  caller_id text;
  request_headers_raw text;
  request_headers jsonb;
  request_http_headers jsonb;
  auth_header text;
  api_key text;
  participant_count integer;
  message_content text;
  parsed_content jsonb;
BEGIN
  IF lower(coalesce(NEW.message_type, '')) <> 'text'
     OR coalesce(NEW.is_deleted, false)
     OR coalesce(NEW.content_state, 'active') IN ('deleted', 'expired')
     OR btrim(coalesce(NEW.content, '')) = '' THEN
    RETURN NEW;
  END IF;

  caller_id := auth.uid()::text;
  IF caller_id IS NULL THEN
    RETURN NEW;
  END IF;

  SELECT u.id::text, u.uid
    INTO syra_auth_id, syra_uid
  FROM public.users AS u
  WHERE lower(coalesce(u.username, '')) = 'syra'
  LIMIT 1;

  IF syra_auth_id IS NULL
     OR syra_uid IS NULL
     OR caller_id = syra_auth_id
     OR NEW.sender_id = syra_uid THEN
    RETURN NEW;
  END IF;

  SELECT count(*)::integer
    INTO participant_count
  FROM public.chat_participants AS cp
  WHERE cp.chat_id = NEW.chat_id;

  IF participant_count <> 2
     OR NOT EXISTS (
       SELECT 1
       FROM public.chat_participants AS cp
       WHERE cp.chat_id = NEW.chat_id
         AND cp.user_id = syra_auth_id
     )
     OR NOT EXISTS (
       SELECT 1
       FROM public.chat_participants AS cp
       WHERE cp.chat_id = NEW.chat_id
         AND cp.user_id = caller_id
     ) THEN
    RETURN NEW;
  END IF;

  message_content := NEW.content;
  BEGIN
    parsed_content := NEW.content::jsonb;
  EXCEPTION WHEN others THEN
    parsed_content := NULL;
  END;

  -- Android E2EE payloads are unreadable to this server-side responder; never send ciphertext to the model.
  IF jsonb_typeof(parsed_content) = 'object' THEN
    IF (
      jsonb_typeof(parsed_content -> 'type') = 'number'
      AND (parsed_content ->> 'type') ~ '^-?[0-9]+$'
      AND jsonb_typeof(parsed_content -> 'body') = 'string'
    ) OR EXISTS (
      SELECT 1
      FROM jsonb_each(parsed_content) AS item(key, value)
      WHERE jsonb_typeof(item.value) = 'object'
        AND jsonb_typeof(item.value -> 'type') = 'number'
        AND (item.value ->> 'type') ~ '^-?[0-9]+$'
        AND jsonb_typeof(item.value -> 'body') = 'string'
    ) THEN
      RETURN NEW;
    END IF;

    IF jsonb_typeof(parsed_content -> 'content') = 'string' THEN
      message_content := parsed_content ->> 'content';
    END IF;
  ELSIF jsonb_typeof(parsed_content) = 'array' AND EXISTS (
    SELECT 1
    FROM jsonb_array_elements(parsed_content) AS item(value)
    WHERE jsonb_typeof(item.value) = 'object'
      AND jsonb_typeof(item.value -> 'type') = 'number'
      AND (item.value ->> 'type') ~ '^-?[0-9]+$'
      AND jsonb_typeof(item.value -> 'body') = 'string'
  ) THEN
    RETURN NEW;
  END IF;

  IF btrim(coalesce(message_content, '')) = '' THEN
    RETURN NEW;
  END IF;

  request_headers_raw := current_setting('request.headers', true);
  IF request_headers_raw IS NULL OR btrim(request_headers_raw) = '' THEN
    RETURN NEW;
  END IF;

  BEGIN
    request_headers := request_headers_raw::jsonb;
  EXCEPTION WHEN others THEN
    RETURN NEW;
  END;

  auth_header := request_headers ->> 'authorization';
  IF auth_header IS NULL OR auth_header !~* '^Bearer[[:space:]]+[^[:space:]]+$' THEN
    RETURN NEW;
  END IF;

  api_key := request_headers ->> 'apikey';
  request_http_headers := jsonb_build_object(
    'Content-Type', 'application/json',
    'Authorization', auth_header
  );
  IF api_key IS NOT NULL AND btrim(api_key) <> '' THEN
    request_http_headers := request_http_headers || jsonb_build_object('apikey', api_key);
  END IF;

  PERFORM net.http_post(
    url := 'https://apqvyyphlrtmuyjnzmuq.supabase.co/functions/v1/syra-reply-dm',
    headers := request_http_headers,
    body := jsonb_build_object(
      'chat_id', NEW.chat_id,
      'request_id', 'syra-dm-' || NEW.id::text,
      'source_message_id', NEW.id,
      'content', message_content,
      'context', '[]'::jsonb
    ),
    timeout_milliseconds := 30000
  );

  RETURN NEW;
END;
$function$;

REVOKE ALL ON FUNCTION public.invoke_syra_dm_reply() FROM PUBLIC;

DROP TRIGGER IF EXISTS trg_syra_dm_reply ON public.messages;
CREATE TRIGGER trg_syra_dm_reply
AFTER INSERT ON public.messages
FOR EACH ROW
EXECUTE FUNCTION public.invoke_syra_dm_reply();
