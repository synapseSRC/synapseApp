-- Efficient inbox summaries and composite ordering for per-chat timelines.
-- The partial index covers the active-message predicate used by inbox previews,
-- unread counts, and composite cursor pagination.
CREATE INDEX IF NOT EXISTS idx_messages_chat_created_id_active
    ON public.messages (chat_id, created_at DESC, id DESC)
    WHERE is_deleted = false;

CREATE OR REPLACE FUNCTION public.get_chat_inbox_summaries(p_archived_only boolean DEFAULT false)
RETURNS TABLE (
    chat_id uuid,
    participant_id text,
    participant_name text,
    participant_avatar text,
    participant_status text,
    participant_last_seen timestamptz,
    is_group boolean,
    is_pinned boolean,
    is_muted boolean,
    last_message jsonb,
    unread_count bigint
)
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = public, pg_temp
AS $function$
    SELECT
        cp.chat_id,
        COALESCE(other_participant.user_id, cp.user_id) AS participant_id,
        CASE
            WHEN COALESCE(c.is_group, false) THEN COALESCE(c.name, 'Group Chat')
            ELSE COALESCE(u.display_name, u.username, 'Unknown')
        END AS participant_name,
        CASE WHEN COALESCE(c.is_group, false) THEN c.avatar_url ELSE u.avatar END AS participant_avatar,
        u.status AS participant_status,
        u.last_seen AS participant_last_seen,
        COALESCE(c.is_group, false) AS is_group,
        COALESCE(cp.is_pinned, false) AS is_pinned,
        COALESCE(cp.is_muted, false) AS is_muted,
        latest.message_json AS last_message,
        COALESCE(unread.message_count, 0)::bigint AS unread_count
    FROM public.chat_participants cp
    JOIN public.chats c ON c.id = cp.chat_id
    LEFT JOIN LATERAL (
        SELECT cp_other.user_id
        FROM public.chat_participants cp_other
        WHERE cp_other.chat_id = cp.chat_id
          AND cp_other.user_id <> auth.uid()::text
        ORDER BY cp_other.user_id
        LIMIT 1
    ) other_participant ON true
    LEFT JOIN public.users u ON u.uid = other_participant.user_id
    LEFT JOIN LATERAL (
        SELECT
            m.created_at,
            jsonb_build_object(
                'id', m.id,
                'chat_id', m.chat_id,
                'sender_id', m.sender_id,
                'content', COALESCE(m.content, ''),
                'message_type', COALESCE(m.message_type, 'text'),
                'media_url', m.media_url,
                'created_at', m.created_at,
                'metadata', m.metadata
            ) AS message_json
        FROM public.messages m
        WHERE m.chat_id = cp.chat_id
          AND m.is_deleted = false
          AND (m.expires_at IS NULL OR m.expires_at > now())
        ORDER BY m.created_at DESC, m.id DESC
        LIMIT 1
    ) latest ON true
    LEFT JOIN LATERAL (
        SELECT count(*)::bigint AS message_count
        FROM public.messages m
        WHERE m.chat_id = cp.chat_id
          AND m.is_deleted = false
          AND (m.expires_at IS NULL OR m.expires_at > now())
          AND cp.last_read_at IS NOT NULL
          AND m.created_at > cp.last_read_at
    ) unread ON true
    WHERE cp.user_id = auth.uid()::text
      AND COALESCE(cp.is_archived, false) = p_archived_only
    ORDER BY latest.created_at DESC NULLS LAST, cp.chat_id;
$function$;

REVOKE ALL ON FUNCTION public.get_chat_inbox_summaries(boolean) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_chat_inbox_summaries(boolean) TO authenticated;


-- Receipt state is monotonic; skip message rows that already contain the read receipt.
-- The participant checkpoint remains updated so timestamp-based unread summaries stay correct.
CREATE OR REPLACE FUNCTION public.mark_messages_as_read(p_chat_id uuid, p_user_id text)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'pg_temp'
AS $function$
DECLARE
    v_user_id TEXT := auth.uid()::text;
    v_now TIMESTAMPTZ := NOW();
BEGIN
    IF v_user_id IS NULL OR p_user_id IS DISTINCT FROM v_user_id THEN
        RAISE EXCEPTION 'invalid user';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.chat_participants cp
        WHERE cp.chat_id = p_chat_id AND cp.user_id = v_user_id
    ) THEN
        RAISE EXCEPTION 'not a chat participant';
    END IF;

    UPDATE public.chat_participants
    SET last_read_at = v_now
    WHERE chat_id = p_chat_id AND user_id = v_user_id;

    UPDATE public.messages m
    SET read_by = CASE
            WHEN COALESCE(m.read_by, '[]'::jsonb) ? v_user_id
                THEN COALESCE(m.read_by, '[]'::jsonb)
            ELSE COALESCE(m.read_by, '[]'::jsonb) || jsonb_build_array(v_user_id)
        END,
        delivery_status = 'read',
        message_state = 'read',
        read_at = COALESCE(m.read_at, v_now),
        delivered_at = COALESCE(m.delivered_at, v_now),
        updated_at = v_now
    WHERE m.chat_id = p_chat_id
      AND m.sender_id <> v_user_id
      AND (
          NOT (COALESCE(m.read_by, '[]'::jsonb) ? v_user_id)
          OR m.delivery_status IS DISTINCT FROM 'read'
          OR m.message_state IS DISTINCT FROM 'read'
          OR m.read_at IS NULL
          OR m.delivered_at IS NULL
      );
END;
$function$;
