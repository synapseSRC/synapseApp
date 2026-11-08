-- Fix: profile picture uploads fail with
--   ERROR: 42501: new row violates row-level security policy for table "objects"
--
-- Why it happens
-- --------------
-- Supabase Storage writes every uploaded object with an INSERT ... RETURNING, and
-- PostgreSQL evaluates the table's SELECT policy for the RETURNING clause. A bucket that
-- has an INSERT policy but no SELECT policy therefore rejects every upload, even when the
-- row satisfies the INSERT policy.
--
-- storage.objects had exactly that gap for the avatars bucket. 20261026204022
-- (fix_avatar_upload_rls_owner_id) created profile_avatar_upload_owner without the
-- matching read policy. Every other bucket that accepts uploads - posts, covers,
-- story-media, story-thumbnails, reels, chat-attachments, group-avatars - already has a
-- SELECT policy, which is why profile pictures were the only uploads that failed.
--
-- The bug is not client specific: Android (SupabaseStorageService), the shared
-- SupabaseUploadService used by iOS, and the web uploader all POST to the same endpoint
-- and all failed the same way.
--
-- The fix
-- -------
-- Give the avatars bucket the read policy its own write policy assumes, scoped to the
-- uploader and mirroring profile_avatar_upload_owner. The bucket is public
-- (storage.buckets.public = true), so avatar images stay readable by anyone through the
-- public object route; this only unblocks the Storage API write path.
--
-- Rollback: drop policy if exists "profile_avatar_read_owner" on storage.objects;

drop policy if exists "profile_avatar_read_owner" on storage.objects;

create policy "profile_avatar_read_owner"
  on storage.objects
  for select
  to authenticated
  using (
    bucket_id = 'avatars'
    and split_part(name, '/', 1) = (auth.uid())::text
  );
