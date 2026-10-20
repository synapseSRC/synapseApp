-- Group creation permissions, revocable invite codes, and approval requests.
ALTER TABLE public.chats
    ADD COLUMN IF NOT EXISTS allow_member_settings_edits boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS require_join_approval boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS allow_member_invites boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS invite_code text NOT NULL DEFAULT replace(gen_random_uuid()::text, '-', '');

CREATE UNIQUE INDEX IF NOT EXISTS chats_invite_code_unique
    ON public.chats (invite_code);

CREATE TABLE IF NOT EXISTS public.group_join_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id uuid NOT NULL REFERENCES public.chats(id) ON DELETE CASCADE,
    user_id text NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    status text NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'declined')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (chat_id, user_id)
);

ALTER TABLE public.group_join_requests ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Group members can view own requests" ON public.group_join_requests;
CREATE POLICY "Group members can view own requests"
    ON public.group_join_requests FOR SELECT TO authenticated
    USING (user_id = get_current_user_uid());
DROP POLICY IF EXISTS "Group admins can view requests" ON public.group_join_requests;
CREATE POLICY "Group admins can view requests"
    ON public.group_join_requests FOR SELECT TO authenticated
    USING (private.is_chat_admin(chat_id, get_current_user_uid()));

CREATE OR REPLACE FUNCTION public.rotate_group_invite_code(p_chat_id uuid)
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, private, pg_temp
AS $$
DECLARE next_code text;
DECLARE current_uid text := get_current_user_uid();
BEGIN
    IF current_uid IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    UPDATE public.chats AS c
       SET invite_code = replace(gen_random_uuid()::text, '-', ''), updated_at = now()
     WHERE c.id = p_chat_id
       AND c.is_group = true
       AND (private.is_chat_admin(c.id, current_uid) OR c.allow_member_invites = true)
    RETURNING c.invite_code INTO next_code;
    IF next_code IS NULL THEN RAISE EXCEPTION 'Group not found or invite permission denied'; END IF;
    RETURN next_code;
END;
$$;

CREATE OR REPLACE FUNCTION public.resolve_group_invite(p_invite_code text)
RETURNS TABLE (
    chat_id uuid,
    name text,
    description text,
    avatar_url text,
    require_join_approval boolean,
    member_count bigint
)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT c.id, c.name, c.description, c.avatar_url, c.require_join_approval,
           count(cp.user_id)::bigint
      FROM public.chats c
      LEFT JOIN public.chat_participants cp ON cp.chat_id = c.id
     WHERE c.is_group = true AND c.invite_code = p_invite_code
     GROUP BY c.id, c.name, c.description, c.avatar_url, c.require_join_approval
     LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION public.request_group_join_by_invite(p_invite_code text)
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, private, pg_temp
AS $$
DECLARE target_chat public.chats%ROWTYPE;
DECLARE current_uid text := get_current_user_uid();
BEGIN
    IF current_uid IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    SELECT * INTO target_chat FROM public.chats
     WHERE is_group = true AND invite_code = p_invite_code LIMIT 1;
    IF NOT FOUND THEN RAISE EXCEPTION 'Invite link is invalid or has been revoked'; END IF;
    IF EXISTS (SELECT 1 FROM public.chat_participants WHERE chat_id = target_chat.id AND user_id = current_uid) THEN
        RETURN 'already_member';
    END IF;
    IF target_chat.require_join_approval THEN
        INSERT INTO public.group_join_requests (chat_id, user_id, status, updated_at)
        VALUES (target_chat.id, current_uid, 'pending', now())
        ON CONFLICT (chat_id, user_id) DO UPDATE
            SET status = 'pending', updated_at = now()
            WHERE public.group_join_requests.status <> 'approved';
        RETURN 'pending';
    END IF;
    INSERT INTO public.chat_participants (chat_id, user_id, is_admin)
    VALUES (target_chat.id, current_uid, false)
    ON CONFLICT DO NOTHING;
    RETURN 'joined';
END;
$$;

CREATE OR REPLACE FUNCTION public.approve_group_join_request(p_request_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, private, pg_temp
AS $$
DECLARE request_row public.group_join_requests%ROWTYPE;
DECLARE current_uid text := get_current_user_uid();
BEGIN
    IF current_uid IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    SELECT * INTO request_row FROM public.group_join_requests WHERE id = p_request_id FOR UPDATE;
    IF NOT FOUND OR request_row.status <> 'pending' THEN RAISE EXCEPTION 'Pending request not found'; END IF;
    IF NOT private.is_chat_admin(request_row.chat_id, current_uid) THEN RAISE EXCEPTION 'Admin permission required'; END IF;
    INSERT INTO public.chat_participants (chat_id, user_id, is_admin)
    VALUES (request_row.chat_id, request_row.user_id, false)
    ON CONFLICT DO NOTHING;
    UPDATE public.group_join_requests SET status = 'approved', updated_at = now() WHERE id = p_request_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.decline_group_join_request(p_request_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, private, pg_temp
AS $$
DECLARE request_row public.group_join_requests%ROWTYPE;
DECLARE current_uid text := get_current_user_uid();
BEGIN
    IF current_uid IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    SELECT * INTO request_row FROM public.group_join_requests WHERE id = p_request_id FOR UPDATE;
    IF NOT FOUND OR request_row.status <> 'pending' THEN RAISE EXCEPTION 'Pending request not found'; END IF;
    IF NOT private.is_chat_admin(request_row.chat_id, current_uid) THEN RAISE EXCEPTION 'Admin permission required'; END IF;
    UPDATE public.group_join_requests SET status = 'declined', updated_at = now() WHERE id = p_request_id;
END;
$$;

REVOKE ALL ON FUNCTION public.rotate_group_invite_code(uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.resolve_group_invite(text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.request_group_join_by_invite(text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.approve_group_join_request(uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.decline_group_join_request(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.rotate_group_invite_code(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.resolve_group_invite(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.request_group_join_by_invite(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.approve_group_join_request(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.decline_group_join_request(uuid) TO authenticated;
GRANT SELECT ON public.group_join_requests TO authenticated;
REVOKE INSERT, UPDATE, DELETE ON public.group_join_requests FROM authenticated;


CREATE OR REPLACE FUNCTION public.update_group_message_permission(p_chat_id uuid, p_only_admins_can_message boolean)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, private, pg_temp
AS $$
DECLARE current_uid text := get_current_user_uid();
BEGIN
    IF current_uid IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    IF NOT private.is_chat_admin(p_chat_id, current_uid) AND NOT EXISTS (
        SELECT 1 FROM public.chats c
        JOIN public.chat_participants cp ON cp.chat_id = c.id
        WHERE c.id = p_chat_id AND c.allow_member_settings_edits = true AND cp.user_id = current_uid
    ) THEN RAISE EXCEPTION 'Group settings permission required'; END IF;
    UPDATE public.chats SET only_admins_can_message = p_only_admins_can_message, updated_at = now()
     WHERE id = p_chat_id AND is_group = true;
    IF NOT FOUND THEN RAISE EXCEPTION 'Group not found'; END IF;
END;
$$;

CREATE OR REPLACE FUNCTION public.update_group_disappearing_mode(p_chat_id uuid, p_disappearing_mode text)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, private, pg_temp
AS $$
DECLARE current_uid text := get_current_user_uid();
BEGIN
    IF current_uid IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    IF p_disappearing_mode NOT IN ('OFF', 'TWENTY_FOUR_HOURS', 'SEVEN_DAYS', 'NINETY_DAYS') THEN
        RAISE EXCEPTION 'Unsupported disappearing message mode';
    END IF;
    IF NOT private.is_chat_admin(p_chat_id, current_uid) AND NOT EXISTS (
        SELECT 1 FROM public.chats c
        JOIN public.chat_participants cp ON cp.chat_id = c.id
        WHERE c.id = p_chat_id AND c.is_group = true AND c.allow_member_settings_edits = true AND cp.user_id = current_uid
    ) THEN RAISE EXCEPTION 'Group settings permission required'; END IF;
    UPDATE public.chats SET disappearing_mode = p_disappearing_mode, updated_at = now()
     WHERE id = p_chat_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Group not found'; END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.update_group_message_permission(uuid, boolean) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.update_group_disappearing_mode(uuid, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.update_group_message_permission(uuid, boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION public.update_group_disappearing_mode(uuid, text) TO authenticated;
