BEGIN;

-- Generate a dedicated, internal-only credential; the value remains in Vault.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM vault.decrypted_secrets
    WHERE name = 'notification_push_webhook_secret'
  ) THEN
    PERFORM vault.create_secret(
      gen_random_uuid()::text || gen_random_uuid()::text,
      'notification_push_webhook_secret',
      'Authenticates internal database trigger calls to send-push-notification'
    );
  END IF;
END;
$$;

CREATE OR REPLACE FUNCTION private.enqueue_notification_push_event(p_payload jsonb)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'vault', 'net', 'pg_temp'
AS $function$
DECLARE
  v_secret text;
BEGIN
  SELECT decrypted_secret
    INTO v_secret
  FROM vault.decrypted_secrets
  WHERE name = 'notification_push_webhook_secret'
  LIMIT 1;

  IF v_secret IS NULL OR v_secret = '' THEN
    RAISE WARNING 'Notification push secret is missing from Vault; push event was not queued';
    RETURN;
  END IF;

  PERFORM net.http_post(
    url := 'https://apqvyyphlrtmuyjnzmuq.supabase.co/functions/v1/send-push-notification',
    body := p_payload,
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'x-notification-webhook-secret', v_secret
    ),
    timeout_milliseconds := 5000
  );
EXCEPTION
  WHEN OTHERS THEN
    RAISE WARNING 'Could not queue notification push event: %', SQLERRM;
END;
$function$;

CREATE OR REPLACE FUNCTION private.handle_message_notification()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
DECLARE
  v_is_group boolean;
BEGIN
  SELECT is_group
    INTO v_is_group
  FROM public.chats
  WHERE id = NEW.chat_id;

  IF NOT FOUND OR COALESCE(v_is_group, true) THEN
    RETURN NEW;
  END IF;

  PERFORM private.enqueue_notification_push_event(
    jsonb_build_object('message_id', NEW.id::text)
  );

  RETURN NEW;
END;
$function$;

CREATE OR REPLACE FUNCTION private.dispatch_notification_push()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
BEGIN
  PERFORM private.enqueue_notification_push_event(
    jsonb_build_object('notification_id', NEW.id::text)
  );
  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_notification_push_dispatch ON public.notifications;
CREATE TRIGGER trg_notification_push_dispatch
AFTER INSERT ON public.notifications
FOR EACH ROW
EXECUTE FUNCTION private.dispatch_notification_push();

-- Prevent clients from fabricating notification rows that would now cause remote pushes.
DROP POLICY IF EXISTS "System can insert notifications" ON public.notifications;
REVOKE INSERT ON TABLE public.notifications FROM anon, authenticated;

REVOKE ALL ON FUNCTION private.enqueue_notification_push_event(jsonb) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION private.handle_message_notification() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION private.dispatch_notification_push() FROM PUBLIC, anon, authenticated;

COMMENT ON FUNCTION private.enqueue_notification_push_event(jsonb)
IS 'Queues an authenticated asynchronous push event to the send-push-notification Edge Function via pg_net.';
COMMENT ON FUNCTION private.handle_message_notification()
IS 'Queues direct-message push delivery after a message insert; push body never includes message content.';
COMMENT ON FUNCTION private.dispatch_notification_push()
IS 'Dispatches a push for trusted database-generated rows in public.notifications.';

COMMIT;
