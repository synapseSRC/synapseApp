-- Add missing profile columns used by EditProfileViewModel
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS discord_tag TEXT,
    ADD COLUMN IF NOT EXISTS github_profile TEXT,
    ADD COLUMN IF NOT EXISTS occupation TEXT,
    ADD COLUMN IF NOT EXISTS workplace TEXT,
    ADD COLUMN IF NOT EXISTS personal_website TEXT,
    ADD COLUMN IF NOT EXISTS public_email TEXT,
    ADD COLUMN IF NOT EXISTS region TEXT;
