ALTER TABLE public.stories
  DROP CONSTRAINT stories_media_type_check,
  ADD CONSTRAINT stories_media_type_check
    CHECK (media_type = ANY (ARRAY['image'::text, 'video'::text, 'text'::text, 'photo'::text]));
