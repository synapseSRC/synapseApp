SELECT trigger_name, event_manipulation, action_timing
FROM information_schema.triggers
WHERE event_object_table = 'posts' AND event_object_schema = 'public'
ORDER BY trigger_name;
