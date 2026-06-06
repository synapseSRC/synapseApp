-- All triggers on posts table
SELECT trigger_name, event_manipulation, action_timing, action_statement
FROM information_schema.triggers
WHERE event_object_table = 'posts' AND event_object_schema = 'public'
ORDER BY trigger_name;

-- Check if in_reply_to_post_id and root_post_id are in PostInsertDto (they aren't)
-- Confirm the DTO fields vs actual columns that are NOT NULL with no default
SELECT column_name, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'posts'
  AND is_nullable = 'NO' AND column_default IS NULL
ORDER BY ordinal_position;
