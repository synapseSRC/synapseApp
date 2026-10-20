BEGIN;

-- users.id is the internal UUID; users.uid is the authenticated app-user identifier.
-- The helper resolves auth.uid() through users.id, so updates stay restricted to the
-- matching users.uid row and cannot transfer ownership to another account.
DROP POLICY IF EXISTS "Users can update own profile" ON public.users;
CREATE POLICY "Users can update own profile"
    ON public.users
    FOR UPDATE
    TO authenticated
    USING (uid = public.get_current_user_uid())
    WITH CHECK (uid = public.get_current_user_uid());

COMMIT;
