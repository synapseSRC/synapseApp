-- Check ALL policies including their permissive/restrictive nature
SELECT policyname, cmd, permissive, qual, with_check
FROM pg_policies
WHERE schemaname = 'public' AND tablename = 'posts'
ORDER BY cmd, policyname;
