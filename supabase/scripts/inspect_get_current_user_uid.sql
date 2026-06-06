SELECT pg_get_functiondef(oid)
FROM pg_proc
WHERE proname = 'get_current_user_uid';
