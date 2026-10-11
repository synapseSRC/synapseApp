-- Extend the existing profile editor with interests and featured-profile selections.
-- Keep avatar uploads scoped to the signed-in user's own folder in the public avatars bucket.

ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS interests jsonb NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS featured_post_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS featured_media_urls jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE public.users
  DROP CONSTRAINT IF EXISTS users_interests_array_check,
  DROP CONSTRAINT IF EXISTS users_featured_post_ids_array_check,
  DROP CONSTRAINT IF EXISTS users_featured_media_urls_array_check;

ALTER TABLE public.users
  ADD CONSTRAINT users_interests_array_check CHECK (jsonb_typeof(interests) = 'array'),
  ADD CONSTRAINT users_featured_post_ids_array_check CHECK (jsonb_typeof(featured_post_ids) = 'array'),
  ADD CONSTRAINT users_featured_media_urls_array_check CHECK (jsonb_typeof(featured_media_urls) = 'array');

DROP POLICY IF EXISTS profile_avatar_upload_owner ON storage.objects;
CREATE POLICY profile_avatar_upload_owner
  ON storage.objects FOR INSERT TO authenticated
  WITH CHECK (
    bucket_id = 'avatars'
    AND owner_id = auth.uid()::text
    AND split_part(name, '/', 1) = auth.uid()::text
  );
