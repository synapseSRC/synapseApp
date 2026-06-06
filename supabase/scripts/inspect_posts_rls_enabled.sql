-- Check RLS status on posts
SELECT relname, relrowsecurity, relforcerowsecurity
FROM pg_class
WHERE relname = 'posts' AND relnamespace = 'public'::regnamespace;
