-- The SELECT policy on chat_participants references chat_participants itself,
-- causing infinite recursion. Fix by using a SECURITY DEFINER helper that
-- reads chat_participants without RLS, breaking the cycle.

CREATE OR REPLACE FUNCTION public.is_chat_participant(p_chat_id uuid, p_user_id text)
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path TO 'public'
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.chat_participants
        WHERE chat_id = p_chat_id AND user_id = p_user_id
    );
$$;

DROP POLICY IF EXISTS "Participants can view participants" ON public.chat_participants;

CREATE POLICY "Participants can view participants"
ON public.chat_participants FOR SELECT
USING (
    user_id = get_current_user_uid()
    OR is_chat_participant(chat_id, get_current_user_uid())
);
