-- Confirm the current INSERT policy
SELECT policyname, cmd, with_check
FROM pg_policies
WHERE schemaname = 'public' AND tablename = 'posts' AND cmd = 'INSERT';
