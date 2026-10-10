# Supabase deployment note

The private-profile migration was executed on the connected Supabase project through MCP at the user’s direction. Read-only verification confirmed the follow_requests table (0 rows), public_profiles view, privacy/follow RPCs, RLS helpers, and the posts, covers, story-media, story-thumbnails, and reels buckets set to private. No user content or follow rows were changed by the migration.

The migration and SQL RLS test files were removed from the current PR at the user’s request after execution. Their content remains in earlier commits on the feature branch, but it will not be included in the merge diff. This PR therefore does not carry a versioned database migration or test harness into main. Preserve/recreate the migration in the repository if the schema change must be reproducible from main.

## Accepted media scope

Third-party media URLs are intentionally not protected by profile RLS. The read-only audit found 7 external image-hosted references across 3 account owners, including follower-only stories; one affected profile is already private. Supabase RLS now hides the corresponding database rows from unauthorized viewers, but a person who already has an external URL can still fetch that media directly. The user accepted this limitation; the migration did not rehost or revoke those files.

First-party Supabase Storage buckets are private and use short-lived signed URLs after RLS authorization. Avatars and chat attachments remain outside profile locking. The SQL RLS harness was not run against production.
