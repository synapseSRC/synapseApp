SELECT pg_get_constraintdef(oid) FROM pg_constraint
WHERE conname = 'stories_media_type_check';
