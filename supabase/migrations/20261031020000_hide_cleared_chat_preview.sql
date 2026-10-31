-- Clear chat hides the last message and unread badge for the user who cleared it,
-- without affecting other participants' view of the conversation.
CREATE OR REPLACE FUNCTION public.get_chat_inbox_summaries(p_archived_only boolean)
 RETURNS TABLE(chat_id text, participant_id text, participant_name text, participant_avatar text, participant_status text, participant_last_seen text, is_group boolean, is_pinned boolean, is_muted boolean, last_message jsonb, unread_count bigint)
 LANGUAGE plpgsql
AS $function$
DECLARE
    v_user_id text := public.get_current_user_uid();
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'authentication required' USING ERRCODE = '28000';
    END IF;

    RETURN QUERY
    WITH my_chats AS (
        SELECT cp.chat_id, cp.is_archived, cp.is_pinned, cp.is_muted, cp.last_read_at, cp.created_at,
               c.is_group, c.name AS chat_name, c.avatar_url AS chat_avatar
        FROM public.chat_participants cp
        JOIN public.chats c ON c.id = cp.chat_id
        WHERE cp.user_id = v_user_id
          AND cp.is_archived = p_archived_only
    ),
    rep_participant AS (
        SELECT DISTINCT ON (cp.chat_id) cp.chat_id, cp.user_id AS participant_id
        FROM public.chat_participants cp
        WHERE cp.user_id <> v_user_id
        ORDER BY cp.chat_id, cp.created_at
    ),
    visible_msgs AS (
        SELECT m.*
        FROM public.messages m
        WHERE m.is_deleted IS NOT TRUE
          AND NOT EXISTS (
              SELECT 1 FROM public.user_deleted_messages d
              WHERE d.user_id = v_user_id AND d.message_id = m.id
          )
    ),
    last_msgs AS (
        SELECT DISTINCT ON (m.chat_id) m.chat_id, m.id, m.sender_id, m.content, m.message_type,
               m.media_url, m.delivery_status, m.message_state, m.content_state, m.is_deleted,
               m.is_edited, m.reply_to_id, m.read_by, m.created_at, m.sent_at, m.delivered_at,
               m.read_at, m.edited_at, m.deleted_at, m.updated_at, m.attachments, m.expires_at,
               m.failure_reason, m.metadata
        FROM visible_msgs m
        ORDER BY m.chat_id, m.created_at DESC
    ),
    unread_counts AS (
        SELECT cp.chat_id, count(*)::bigint AS unread_count
        FROM public.chat_participants cp
        JOIN visible_msgs m ON m.chat_id = cp.chat_id
        WHERE cp.user_id = v_user_id
          AND cp.is_archived = p_archived_only
          AND (cp.last_read_at IS NULL OR m.created_at > cp.last_read_at)
          AND m.sender_id <> v_user_id
        GROUP BY cp.chat_id
    )
    SELECT 
        mc.chat_id::text AS chat_id,
        CASE 
            WHEN mc.is_group THEN mc.chat_id::text
            ELSE COALESCE(rp.participant_id, mc.chat_id::text)
        END AS participant_id,
        CASE 
            WHEN mc.is_group THEN mc.chat_name
            ELSE COALESCE(u.display_name, u.username, mc.chat_name)
        END AS participant_name,
        CASE 
            WHEN mc.is_group THEN mc.chat_avatar
            ELSE u.avatar
        END AS participant_avatar,
        CASE 
            WHEN mc.is_group THEN NULL
            ELSE u.status
        END AS participant_status,
        CASE 
            WHEN mc.is_group THEN NULL
            ELSE u.last_seen::text
        END AS participant_last_seen,
        mc.is_group AS is_group,
        mc.is_pinned AS is_pinned,
        mc.is_muted AS is_muted,
        COALESCE(
            jsonb_build_object(
                'id', lm.id::text,
                'chat_id', lm.chat_id::text,
                'sender_id', lm.sender_id,
                'content', lm.content,
                'message_type', lm.message_type,
                'media_url', lm.media_url,
                'delivery_status', lm.delivery_status,
                'message_state', lm.message_state,
                'content_state', lm.content_state,
                'is_deleted', lm.is_deleted,
                'is_edited', lm.is_edited,
                'reply_to_id', lm.reply_to_id::text,
                'sent_at', lm.sent_at::text,
                'delivered_at', lm.delivered_at::text,
                'read_at', lm.read_at::text,
                'edited_at', lm.edited_at::text,
                'deleted_at', lm.deleted_at::text,
                'created_at', lm.created_at::text,
                'updated_at', lm.updated_at::text,
                'read_by', lm.read_by,
                'expires_at', lm.expires_at::text,
                'failure_reason', lm.failure_reason
            ),
            NULL::jsonb
        ) AS last_message,
        COALESCE(uc.unread_count, 0::bigint) AS unread_count
    FROM my_chats mc
    LEFT JOIN rep_participant rp ON rp.chat_id = mc.chat_id
    LEFT JOIN public.users u ON u.uid = rp.participant_id
    LEFT JOIN last_msgs lm ON lm.chat_id = mc.chat_id
    LEFT JOIN unread_counts uc ON uc.chat_id = mc.chat_id
    ORDER BY mc.is_pinned DESC, COALESCE(lm.created_at, mc.created_at) DESC;
END;
$function$;
