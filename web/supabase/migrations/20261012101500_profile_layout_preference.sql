-- Persist a user's preferred presentation for their profile post list.
-- The default preserves the existing chronological feed for current accounts.

ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS profile_layout text;

UPDATE public.users
SET profile_layout = 'feed'
WHERE profile_layout IS NULL;

ALTER TABLE public.users
  ALTER COLUMN profile_layout SET DEFAULT 'feed',
  ALTER COLUMN profile_layout SET NOT NULL;

ALTER TABLE public.users
  DROP CONSTRAINT IF EXISTS users_profile_layout_check;

ALTER TABLE public.users
  ADD CONSTRAINT users_profile_layout_check
  CHECK (profile_layout IN ('feed', 'grid', 'highlights'));
