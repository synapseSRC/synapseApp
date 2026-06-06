-- ============================================================
-- SYNAPSE SOCIAL — UNIVERSAL SETUP SCRIPT
-- Run this on a fresh Supabase project to restore full schema.
-- supabase db reset  (local)  OR  paste into SQL editor (remote)
-- ============================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA extensions;
CREATE EXTENSION IF NOT EXISTS "pg_net";
CREATE EXTENSION IF NOT EXISTS "pg_cron";

-- ============================================================
-- CORE USER TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS public.users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid         TEXT UNIQUE,   -- auto-filled from id by trigger if null
    email       TEXT,
    username    TEXT UNIQUE,
    nickname    TEXT,
    display_name TEXT,
    bio         TEXT,
    avatar      TEXT,
    avatar_history_type TEXT DEFAULT 'local',
    profile_cover_image TEXT,
    account_premium BOOLEAN DEFAULT false,
    user_level_xp INT DEFAULT 500,
    verify      BOOLEAN DEFAULT false,
    is_private  BOOLEAN DEFAULT false,
    account_type TEXT DEFAULT 'user',
    gender      TEXT DEFAULT 'hidden',
    banned      BOOLEAN DEFAULT false,
    status      TEXT DEFAULT 'offline',
    join_date   TEXT,
    one_signal_player_id TEXT,
    last_seen   TIMESTAMPTZ,
    chatting_with TEXT,
    followers_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    posts_count INT DEFAULT 0,
    location    TEXT,
    relationship_status TEXT,
    birthday    TEXT,
    work        TEXT,
    education   TEXT,
    current_city TEXT,
    hometown    TEXT,
    website     TEXT,
    pronouns    TEXT,
    linked_accounts JSONB DEFAULT '[]',
    privacy_settings JSONB DEFAULT '{}',
    is_admin    BOOLEAN NOT NULL DEFAULT false,
    -- extra profile fields
    discord_tag TEXT,
    github_profile TEXT,
    occupation TEXT,
    workplace TEXT,
    personal_website TEXT,
    public_email TEXT,
    region TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.usernames (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid         TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    username    TEXT UNIQUE NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.user_settings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED,
    theme       TEXT DEFAULT 'system',
    language    TEXT DEFAULT 'en',
    font_size   TEXT DEFAULT 'medium',
    notifications_enabled BOOLEAN DEFAULT true,
    increase_contrast BOOLEAN DEFAULT false,
    high_contrast_text BOOLEAN DEFAULT false,
    reduce_animations BOOLEAN DEFAULT false,
    autoplay_animations BOOLEAN DEFAULT true,
    data        JSONB DEFAULT '{}',
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.user_presence (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED,
    is_online       BOOLEAN DEFAULT false,
    last_seen       TIMESTAMPTZ DEFAULT NOW(),
    activity_status TEXT DEFAULT 'offline',
    current_chat_id TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.user_preferences (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    preferences JSONB DEFAULT '{}',
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.user_passkeys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    credential_id   TEXT UNIQUE NOT NULL,
    device_name     TEXT,
    date_added      BIGINT,
    last_used       BIGINT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.user_api_keys (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    key_hash    TEXT NOT NULL,
    name        TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.user_public_keys (
    user_id     TEXT PRIMARY KEY REFERENCES public.users(uid) ON DELETE CASCADE,
    public_key  TEXT NOT NULL,
    key_version INT DEFAULT 1,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.push_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    token       TEXT NOT NULL,
    platform    TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.profile_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    avatar      TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.cover_image_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    cover_image_url TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- SOCIAL GRAPH
-- ============================================================

CREATE TABLE IF NOT EXISTS public.follows (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    following_id    TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(follower_id, following_id)
);

CREATE TABLE IF NOT EXISTS public.blocks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id  TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    blocked_id  TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id)
);

CREATE TABLE IF NOT EXISTS public.profile_likes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    target_id   TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, target_id)
);

-- ============================================================
-- POSTS
-- ============================================================

CREATE TABLE IF NOT EXISTS public.posts (
    id                      TEXT PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    key                     TEXT,
    author_uid              TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    post_text               TEXT,
    post_image              TEXT,
    post_type               TEXT,
    post_visibility         TEXT DEFAULT 'public',
    post_hide_views_count   TEXT,
    post_hide_like_count    TEXT,
    post_hide_comments_count TEXT,
    post_disable_comments   TEXT,
    publish_date            TEXT,
    timestamp               BIGINT NOT NULL DEFAULT 0,
    likes_count             INT DEFAULT 0,
    comments_count          INT DEFAULT 0,
    views_count             INT DEFAULT 0,
    reshares_count          INT DEFAULT 0,
    media_items             JSONB NOT NULL DEFAULT '[]',
    has_poll                BOOLEAN,
    poll_question           TEXT,
    poll_options            JSONB NOT NULL DEFAULT '[]',
    poll_end_time           TEXT,
    poll_allow_multiple     BOOLEAN,
    has_location            BOOLEAN,
    location_name           TEXT,
    location_address        TEXT,
    location_latitude       DOUBLE PRECISION,
    location_longitude      DOUBLE PRECISION,
    location_place_id       TEXT,
    youtube_url             TEXT,
    link_previews           JSONB,
    metadata                JSONB,
    quoted_post_id          TEXT REFERENCES public.posts(id) ON DELETE SET NULL,
    is_quote                BOOLEAN DEFAULT false,
    in_reply_to_post_id     TEXT REFERENCES public.posts(id) ON DELETE SET NULL,
    root_post_id            TEXT REFERENCES public.posts(id) ON DELETE SET NULL,
    reply_to_usernames      JSONB DEFAULT '[]',
    is_deleted              BOOLEAN DEFAULT false,
    is_edited               BOOLEAN DEFAULT false,
    edited_at               TIMESTAMPTZ,
    deleted_at              TIMESTAMPTZ,
    is_encrypted            BOOLEAN DEFAULT false,
    nonce                   TEXT,
    encryption_key_id       TEXT,
    encrypted_content       JSONB,
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.post_likes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.likes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    target_id   TEXT,
    target_type TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.reactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    reaction_type   TEXT NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.reshares (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    comment     TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.favorites (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    post_id             TEXT REFERENCES public.posts(id) ON DELETE CASCADE,
    collection_id       UUID,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, post_id)
);

CREATE TABLE IF NOT EXISTS public.bookmark_collections (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.hidden_posts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, post_id)
);

CREATE TABLE IF NOT EXISTS public.poll_votes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    option_index    INT NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.mentions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- HASHTAGS
-- ============================================================

CREATE TABLE IF NOT EXISTS public.hashtags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag         TEXT UNIQUE NOT NULL,
    usage_count INT DEFAULT 0,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.post_hashtags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    hashtag_id  UUID NOT NULL REFERENCES public.hashtags(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(post_id, hashtag_id)
);

-- ============================================================
-- REPORTS
-- ============================================================

CREATE TABLE IF NOT EXISTS public.reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    target_id   TEXT NOT NULL,
    target_type TEXT NOT NULL,
    reason      TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.post_reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    reason      TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(reporter_id, post_id)
);

CREATE TABLE IF NOT EXISTS public.user_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    reported_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    reason          TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(reporter_id, reported_id)
);

-- ============================================================
-- MEDIA
-- ============================================================

CREATE TABLE IF NOT EXISTS public.media_files (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    url         TEXT NOT NULL,
    media_type  TEXT,
    size_bytes  BIGINT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.media_interactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    media_id    UUID REFERENCES public.media_files(id) ON DELETE CASCADE,
    type        TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.media_likes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    media_id    UUID REFERENCES public.media_files(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, media_id)
);

-- ============================================================
-- STORIES
-- ============================================================

CREATE TABLE IF NOT EXISTS public.stories (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    media_url               TEXT,
    media_type              TEXT CHECK (media_type IN ('image','video','text','photo')),
    content                 TEXT,
    duration                INT,
    duration_hours          INT DEFAULT 24,
    privacy_setting         TEXT DEFAULT 'public',
    views_count             INT DEFAULT 0,
    reactions_count         INT DEFAULT 0,
    replies_count           INT DEFAULT 0,
    is_active               BOOLEAN DEFAULT true,
    thumbnail_url           TEXT,
    media_width             INT,
    media_height            INT,
    media_duration_seconds  INT,
    file_size_bytes         BIGINT,
    is_reported             BOOLEAN DEFAULT false,
    moderation_status       TEXT DEFAULT 'approved',
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    expires_at              TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '24 hours')
);

CREATE TABLE IF NOT EXISTS public.story_views (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    viewer_id   TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    viewed_at   TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(story_id, viewer_id)
);

CREATE TABLE IF NOT EXISTS public.story_reactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    reaction    TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(story_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.story_replies (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.story_mentions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.story_archive (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    archived_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.story_highlights (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    title       TEXT NOT NULL,
    cover_url   TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.story_highlight_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    highlight_id    UUID NOT NULL REFERENCES public.story_highlights(id) ON DELETE CASCADE,
    story_id        UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.story_interactive_elements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    type        TEXT NOT NULL,
    data        JSONB DEFAULT '{}',
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.story_interactive_responses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    element_id      UUID NOT NULL REFERENCES public.story_interactive_elements(id) ON DELETE CASCADE,
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    response        JSONB,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.story_custom_privacy (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.story_hidden_from (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.close_friends (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    friend_id   TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS public.tag_requests (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    requester_id TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    target_id   TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    status      TEXT DEFAULT 'pending',
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- REELS
-- ============================================================

CREATE TABLE IF NOT EXISTS public.reels (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id          TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    video_url           TEXT NOT NULL,
    thumbnail_url       TEXT,
    caption             TEXT,
    music_track         TEXT,
    likes_count         INT DEFAULT 0,
    comment_count       INT DEFAULT 0,
    share_count         INT DEFAULT 0,
    oppose_count        INT DEFAULT 0,
    location_name       TEXT,
    location_address    TEXT,
    location_latitude   DOUBLE PRECISION,
    location_longitude  DOUBLE PRECISION,
    metadata            JSONB,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.reel_interactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reel_id     UUID NOT NULL REFERENCES public.reels(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    type        TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.reel_opposers_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reel_id     UUID NOT NULL REFERENCES public.reels(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(reel_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.reel_comments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reel_id     UUID NOT NULL REFERENCES public.reels(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- CHAT
-- ============================================================

CREATE TABLE IF NOT EXISTS public.chats (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    TEXT,
    description             TEXT,
    avatar_url              TEXT,
    is_group                BOOLEAN DEFAULT false,
    created_by              TEXT REFERENCES public.users(uid) ON DELETE SET NULL,
    only_admins_can_message BOOLEAN DEFAULT false,
    disappearing_mode       TEXT,
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.chat_participants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id         UUID NOT NULL REFERENCES public.chats(id) ON DELETE CASCADE,
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    is_admin        BOOLEAN DEFAULT false,
    is_archived     BOOLEAN DEFAULT false,
    is_pinned       BOOLEAN DEFAULT false,
    is_muted        BOOLEAN DEFAULT false,
    last_read_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id         UUID NOT NULL REFERENCES public.chats(id) ON DELETE CASCADE,
    sender_id       TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    content         TEXT,
    message_type    TEXT DEFAULT 'text',
    media_url       TEXT,
    delivery_status TEXT DEFAULT 'sent',
    message_state   TEXT DEFAULT 'sent',
    is_deleted      BOOLEAN DEFAULT false,
    is_edited       BOOLEAN DEFAULT false,
    reply_to_id     UUID REFERENCES public.messages(id) ON DELETE SET NULL,
    read_by         JSONB DEFAULT '[]',
    expires_at      TIMESTAMPTZ,
    edited_at       TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    read_at         TIMESTAMPTZ,
    attachments     JSONB,
    deleted_for_users JSONB DEFAULT '[]',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.message_reactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    reaction_type   TEXT NOT NULL,
    chat_id         UUID REFERENCES public.chats(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(message_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.message_edit_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id  UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    edited_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.message_forwards (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_message_id UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    forwarded_message_id UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.user_deleted_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id  UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(message_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.typing_status (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id     UUID NOT NULL REFERENCES public.chats(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    is_typing   BOOLEAN DEFAULT false,
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(chat_id, user_id)
);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS public.notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id    TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    sender_id       TEXT REFERENCES public.users(uid) ON DELETE SET NULL,
    type            TEXT NOT NULL,
    title           JSONB NOT NULL,
    body            JSONB,
    data            JSONB,
    priority        INT DEFAULT 2,
    delivery_status TEXT DEFAULT 'pending',
    is_read         BOOLEAN DEFAULT false,
    read_at         TIMESTAMPTZ,
    interacted_at   TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.notification_preferences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    enabled         BOOLEAN DEFAULT true,
    settings        JSONB DEFAULT '{}',
    quiet_hours     JSONB DEFAULT '{}',
    do_not_disturb  BOOLEAN DEFAULT false,
    dnd_until       TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.notification_analytics (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id     UUID NOT NULL REFERENCES public.notifications(id) ON DELETE CASCADE,
    user_id             TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    delivered_at        TIMESTAMPTZ,
    opened_at           TIMESTAMPTZ,
    interaction_type    TEXT NOT NULL,
    platform            TEXT NOT NULL,
    app_version         TEXT
);

-- ============================================================
-- MISC
-- ============================================================

CREATE TABLE IF NOT EXISTS public.business_accounts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    account_type            TEXT NOT NULL,
    monetization_enabled    BOOLEAN DEFAULT false,
    verification_status     TEXT DEFAULT 'pending',
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.news_articles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_name     TEXT,
    headline        TEXT NOT NULL,
    image_url       TEXT,
    url             TEXT,
    published_at    TIMESTAMPTZ DEFAULT NOW(),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.changelogs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version     TEXT NOT NULL,
    content     TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.syra_trigger_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event       TEXT,
    data        JSONB,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.family_connections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    connected_id    TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    relation        TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, connected_id)
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_posts_author_uid ON public.posts(author_uid);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON public.posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_root_post_id ON public.posts(root_post_id);
CREATE INDEX IF NOT EXISTS idx_follows_follower ON public.follows(follower_id);
CREATE INDEX IF NOT EXISTS idx_follows_following ON public.follows(following_id);
CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON public.messages(chat_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON public.messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON public.messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON public.notifications(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON public.notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stories_user_id ON public.stories(user_id);
CREATE INDEX IF NOT EXISTS idx_stories_expires_at ON public.stories(expires_at);
CREATE INDEX IF NOT EXISTS idx_hashtags_tag ON public.hashtags(tag);
CREATE INDEX IF NOT EXISTS idx_post_hashtags_post_id ON public.post_hashtags(post_id);
CREATE INDEX IF NOT EXISTS idx_post_hashtags_hashtag_id ON public.post_hashtags(hashtag_id);
CREATE INDEX IF NOT EXISTS idx_reactions_post_id ON public.reactions(post_id);
CREATE INDEX IF NOT EXISTS idx_user_presence_current_chat_id ON public.user_presence(current_chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_user_id ON public.chat_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_chat_id ON public.chat_participants(chat_id);

-- ============================================================
-- RLS — Enable on all tables
-- ============================================================

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.usernames ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_presence ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_passkeys ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_api_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_public_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.push_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profile_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cover_image_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profile_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.post_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reshares ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.favorites ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookmark_collections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.hidden_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.poll_votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mentions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.hashtags ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.post_hashtags ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.post_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.media_files ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.media_interactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.media_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_views ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_replies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_mentions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_archive ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_highlights ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_highlight_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_interactive_elements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_interactive_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_custom_privacy ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_hidden_from ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.close_friends ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tag_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reels ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reel_interactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reel_opposers_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reel_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_edit_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_forwards ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_deleted_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.typing_status ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_analytics ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.news_articles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.changelogs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.family_connections ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- HELPER FUNCTIONS
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_current_user_uid()
RETURNS TEXT LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT COALESCE(
    (SELECT uid FROM public.users WHERE id = auth.uid() LIMIT 1),
    auth.uid()::TEXT
  );
$$;

-- Breaks the RLS self-reference loop on chat_participants
CREATE OR REPLACE FUNCTION public.is_chat_participant(p_chat_id uuid, p_user_id text)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.chat_participants
    WHERE chat_id = p_chat_id AND user_id = p_user_id
  );
$$;

-- ============================================================
-- RLS POLICIES
-- ============================================================

-- users
CREATE POLICY "Users are publicly readable" ON public.users FOR SELECT USING (true);
CREATE POLICY "Users can insert own profile" ON public.users FOR INSERT WITH CHECK (auth.uid()::TEXT = uid OR auth.uid()::TEXT = id::TEXT);
CREATE POLICY "Users can update own profile" ON public.users FOR UPDATE USING (public.get_current_user_uid() = uid);

-- usernames
CREATE POLICY "Usernames are publicly readable" ON public.usernames FOR SELECT USING (true);
CREATE POLICY "Users manage own username" ON public.usernames FOR ALL USING (public.get_current_user_uid() = uid);

-- user_settings / user_presence / user_preferences
CREATE POLICY "Users manage own settings" ON public.user_settings FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Presence is publicly readable" ON public.user_presence FOR SELECT USING (true);
CREATE POLICY "Users manage own presence" ON public.user_presence FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Users manage own preferences" ON public.user_preferences FOR ALL USING (public.get_current_user_uid() = user_id);

-- user_passkeys / api_keys / public_keys / push_tokens
CREATE POLICY "Users manage own passkeys" ON public.user_passkeys FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Users manage own api keys" ON public.user_api_keys FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Public keys are readable" ON public.user_public_keys FOR SELECT USING (true);
CREATE POLICY "Users manage own public key" ON public.user_public_keys FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Users manage own push tokens" ON public.push_tokens FOR ALL USING (public.get_current_user_uid() = user_id);

-- profile/cover history
CREATE POLICY "Profile history readable" ON public.profile_history FOR SELECT USING (true);
CREATE POLICY "Users manage own profile history" ON public.profile_history FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Cover history readable" ON public.cover_image_history FOR SELECT USING (true);
CREATE POLICY "Users manage own cover history" ON public.cover_image_history FOR ALL USING (public.get_current_user_uid() = user_id);

-- follows / blocks / profile_likes
CREATE POLICY "Follows are publicly readable" ON public.follows FOR SELECT USING (true);
CREATE POLICY "Users manage own follows" ON public.follows FOR ALL USING (public.get_current_user_uid() = follower_id);
CREATE POLICY "Users manage own blocks" ON public.blocks FOR ALL USING (public.get_current_user_uid() = blocker_id);
CREATE POLICY "Users can see if blocked" ON public.blocks FOR SELECT USING (public.get_current_user_uid() = blocked_id);
CREATE POLICY "Profile likes readable" ON public.profile_likes FOR SELECT USING (true);
CREATE POLICY "Users manage own profile likes" ON public.profile_likes FOR ALL USING (public.get_current_user_uid() = user_id);

-- posts
CREATE POLICY "Posts are publicly readable" ON public.posts FOR SELECT USING (true);
CREATE POLICY "Users can insert own posts" ON public.posts FOR INSERT WITH CHECK (auth.uid()::text = author_uid);
CREATE POLICY "Users can update own posts" ON public.posts FOR UPDATE USING (public.get_current_user_uid() = author_uid);
CREATE POLICY "Users can delete own posts" ON public.posts FOR DELETE USING (public.get_current_user_uid() = author_uid);

-- post engagement
CREATE POLICY "Post likes readable" ON public.post_likes FOR SELECT USING (true);
CREATE POLICY "Users manage own post likes" ON public.post_likes FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Likes readable" ON public.likes FOR SELECT USING (true);
CREATE POLICY "Users manage own likes" ON public.likes FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Reactions readable" ON public.reactions FOR SELECT USING (true);
CREATE POLICY "Users manage own reactions" ON public.reactions FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Reshares readable" ON public.reshares FOR SELECT USING (true);
CREATE POLICY "Users manage own reshares" ON public.reshares FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Users manage own favorites" ON public.favorites FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Users manage own bookmarks" ON public.bookmark_collections FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Users manage own hidden posts" ON public.hidden_posts FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Poll votes readable" ON public.poll_votes FOR SELECT USING (true);
CREATE POLICY "Users manage own poll votes" ON public.poll_votes FOR ALL USING (public.get_current_user_uid() = user_id);

-- hashtags / post_hashtags / mentions
CREATE POLICY "Hashtags are public" ON public.hashtags FOR SELECT USING (true);
CREATE POLICY "Authenticated can insert hashtags" ON public.hashtags FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Post hashtags are public" ON public.post_hashtags FOR SELECT USING (true);
CREATE POLICY "Authenticated can insert post hashtags" ON public.post_hashtags FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Users can delete own post hashtags" ON public.post_hashtags FOR DELETE USING (
    EXISTS (SELECT 1 FROM public.posts WHERE id = post_hashtags.post_id AND author_uid = public.get_current_user_uid())
);
CREATE POLICY "Mentions readable" ON public.mentions FOR SELECT USING (true);
CREATE POLICY "Authenticated can insert mentions" ON public.mentions FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- reports
CREATE POLICY "Users can create reports" ON public.reports FOR INSERT WITH CHECK (public.get_current_user_uid() = reporter_id);
CREATE POLICY "Users can create post reports" ON public.post_reports FOR INSERT WITH CHECK (public.get_current_user_uid() = reporter_id);
CREATE POLICY "Users can create user reports" ON public.user_reports FOR INSERT WITH CHECK (public.get_current_user_uid() = reporter_id);

-- media
CREATE POLICY "Media files readable" ON public.media_files FOR SELECT USING (true);
CREATE POLICY "Users manage own media" ON public.media_files FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Media interactions readable" ON public.media_interactions FOR SELECT USING (true);
CREATE POLICY "Users manage own media interactions" ON public.media_interactions FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Media likes readable" ON public.media_likes FOR SELECT USING (true);
CREATE POLICY "Users manage own media likes" ON public.media_likes FOR ALL USING (public.get_current_user_uid() = user_id);

-- stories
CREATE POLICY "Stories readable" ON public.stories FOR SELECT USING (true);
CREATE POLICY "Users manage own stories" ON public.stories FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Story views readable" ON public.story_views FOR SELECT USING (true);
CREATE POLICY "Users can insert story views" ON public.story_views FOR INSERT WITH CHECK (public.get_current_user_uid() = viewer_id);
CREATE POLICY "Story reactions readable" ON public.story_reactions FOR SELECT USING (true);
CREATE POLICY "Users manage own story reactions" ON public.story_reactions FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Story replies readable" ON public.story_replies FOR SELECT USING (true);
CREATE POLICY "Users manage own story replies" ON public.story_replies FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Story highlights readable" ON public.story_highlights FOR SELECT USING (true);
CREATE POLICY "Users manage own highlights" ON public.story_highlights FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Story highlight items readable" ON public.story_highlight_items FOR SELECT USING (true);
CREATE POLICY "Close friends readable" ON public.close_friends FOR SELECT USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Users manage own close friends" ON public.close_friends FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Story interactive elements readable" ON public.story_interactive_elements FOR SELECT USING (true);
CREATE POLICY "Story interactive responses readable" ON public.story_interactive_responses FOR SELECT USING (true);
CREATE POLICY "Users insert story responses" ON public.story_interactive_responses FOR INSERT WITH CHECK (public.get_current_user_uid() = user_id);
CREATE POLICY "Story mentions readable" ON public.story_mentions FOR SELECT USING (true);
CREATE POLICY "Story archive readable by owner" ON public.story_archive FOR SELECT USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Story custom privacy by owner" ON public.story_custom_privacy FOR ALL USING (
    EXISTS (SELECT 1 FROM public.stories WHERE id = story_custom_privacy.story_id AND user_id = public.get_current_user_uid())
);
CREATE POLICY "Story hidden from by owner" ON public.story_hidden_from FOR ALL USING (
    EXISTS (SELECT 1 FROM public.stories WHERE id = story_hidden_from.story_id AND user_id = public.get_current_user_uid())
);
CREATE POLICY "Tag requests readable by involved" ON public.tag_requests FOR SELECT USING (
    public.get_current_user_uid() = requester_id OR public.get_current_user_uid() = target_id
);

-- reels
CREATE POLICY "Reels are public" ON public.reels FOR SELECT USING (true);
CREATE POLICY "Users manage own reels" ON public.reels FOR ALL USING (public.get_current_user_uid() = creator_id);
CREATE POLICY "Reel interactions readable" ON public.reel_interactions FOR SELECT USING (true);
CREATE POLICY "Users manage own reel interactions" ON public.reel_interactions FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Reel opposers readable" ON public.reel_opposers_log FOR SELECT USING (true);
CREATE POLICY "Users manage own reel opposes" ON public.reel_opposers_log FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Reel comments readable" ON public.reel_comments FOR SELECT USING (true);
CREATE POLICY "Users manage own reel comments" ON public.reel_comments FOR ALL USING (public.get_current_user_uid() = user_id);

-- chats (use is_chat_participant to avoid RLS recursion)
CREATE POLICY "Participants can see their chats" ON public.chats FOR SELECT USING (
    public.is_chat_participant(chats.id, public.get_current_user_uid())
);
CREATE POLICY "Authenticated can create chats" ON public.chats FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Admins can update chats" ON public.chats FOR UPDATE USING (
    public.is_chat_participant(chats.id, public.get_current_user_uid())
);

-- chat_participants (SELECT uses helper to avoid recursion)
CREATE POLICY "Participants can view participants" ON public.chat_participants FOR SELECT USING (
    user_id = public.get_current_user_uid()
    OR public.is_chat_participant(chat_id, public.get_current_user_uid())
);
CREATE POLICY "Authenticated can insert participants" ON public.chat_participants FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Users manage own participation" ON public.chat_participants FOR UPDATE USING (user_id = public.get_current_user_uid());
CREATE POLICY "Users can leave chats" ON public.chat_participants FOR DELETE USING (user_id = public.get_current_user_uid());

-- messages
CREATE POLICY "Participants can read messages" ON public.messages FOR SELECT USING (
    public.is_chat_participant(messages.chat_id, public.get_current_user_uid())
);
CREATE POLICY "Participants can send messages" ON public.messages FOR INSERT WITH CHECK (
    public.get_current_user_uid() = sender_id AND
    public.is_chat_participant(messages.chat_id, public.get_current_user_uid())
);
CREATE POLICY "Sender can update own messages" ON public.messages FOR UPDATE USING (public.get_current_user_uid() = sender_id);
CREATE POLICY "Sender can delete own messages" ON public.messages FOR DELETE USING (public.get_current_user_uid() = sender_id);

-- message sub-tables
CREATE POLICY "Message reactions readable by participants" ON public.message_reactions FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.messages m WHERE m.id = message_reactions.message_id AND public.is_chat_participant(m.chat_id, public.get_current_user_uid()))
);
CREATE POLICY "Users manage own message reactions" ON public.message_reactions FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Message edit history readable" ON public.message_edit_history FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.messages m WHERE m.id = message_edit_history.message_id AND public.is_chat_participant(m.chat_id, public.get_current_user_uid()))
);
CREATE POLICY "Users manage own deleted messages" ON public.user_deleted_messages FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Typing status readable by participants" ON public.typing_status FOR SELECT USING (
    public.is_chat_participant(typing_status.chat_id, public.get_current_user_uid())
);
CREATE POLICY "Users manage own typing status" ON public.typing_status FOR ALL USING (public.get_current_user_uid() = user_id);

-- notifications
CREATE POLICY "Users see own notifications" ON public.notifications FOR SELECT USING (public.get_current_user_uid() = recipient_id);
CREATE POLICY "System can insert notifications" ON public.notifications FOR INSERT WITH CHECK (true);
CREATE POLICY "Users update own notifications" ON public.notifications FOR UPDATE USING (public.get_current_user_uid() = recipient_id);
CREATE POLICY "Users delete own notifications" ON public.notifications FOR DELETE USING (public.get_current_user_uid() = recipient_id);
CREATE POLICY "Users manage own notification prefs" ON public.notification_preferences FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Users see own notification analytics" ON public.notification_analytics FOR SELECT USING (public.get_current_user_uid() = user_id);

-- misc
CREATE POLICY "News articles are public" ON public.news_articles FOR SELECT USING (true);
CREATE POLICY "Changelogs are public" ON public.changelogs FOR SELECT USING (true);
CREATE POLICY "Family connections readable by owner" ON public.family_connections FOR ALL USING (public.get_current_user_uid() = user_id);

-- ============================================================
-- GRANTS
-- ============================================================

GRANT USAGE ON SCHEMA public TO authenticated, anon;
GRANT SELECT, INSERT, UPDATE ON public.users TO authenticated;
GRANT SELECT ON public.users TO anon;
GRANT SELECT, INSERT, UPDATE ON public.user_settings TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.user_presence TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.usernames TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.follows TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.blocks TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.posts TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reshares TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.favorites TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.post_likes TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.poll_votes TO authenticated;
GRANT SELECT, INSERT ON public.hashtags TO authenticated;
GRANT SELECT, INSERT, DELETE ON public.post_hashtags TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.chats TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.chat_participants TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.messages TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.message_reactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.typing_status TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_deleted_messages TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.notifications TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.notification_preferences TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.stories TO authenticated;
GRANT SELECT, INSERT ON public.story_views TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_reactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_replies TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reels TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reel_comments TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reel_interactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reel_opposers_log TO authenticated;
GRANT SELECT, INSERT ON public.reports TO authenticated;
GRANT SELECT, INSERT ON public.post_reports TO authenticated;
GRANT SELECT, INSERT ON public.user_reports TO authenticated;
GRANT SELECT ON public.news_articles TO authenticated, anon;
GRANT SELECT ON public.changelogs TO authenticated, anon;
GRANT SELECT, INSERT, UPDATE ON public.user_public_keys TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.push_tokens TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_preferences TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_passkeys TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.profile_history TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.cover_image_history TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.profile_likes TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.bookmark_collections TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.hidden_posts TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.mentions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.media_files TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.media_interactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.media_likes TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.close_friends TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.family_connections TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.message_edit_history TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.notification_analytics TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_highlights TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_highlight_items TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_archive TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_mentions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_interactive_elements TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_interactive_responses TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_custom_privacy TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_hidden_from TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.tag_requests TO authenticated;

-- ============================================================
-- TRIGGERS
-- ============================================================

-- Auto-fill uid from id if not provided
CREATE OR REPLACE FUNCTION public.set_user_uid()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.uid IS NULL OR NEW.uid = '' THEN
    NEW.uid := NEW.id::TEXT;
  END IF;
  RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS set_uid_on_insert ON public.users;
CREATE TRIGGER set_uid_on_insert
  BEFORE INSERT ON public.users
  FOR EACH ROW EXECUTE FUNCTION public.set_user_uid();

-- Auto-create user_settings + user_presence when a user row is inserted
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO public.user_settings (user_id) VALUES (NEW.uid) ON CONFLICT (user_id) DO NOTHING;
  INSERT INTO public.user_presence (user_id) VALUES (NEW.uid) ON CONFLICT (user_id) DO NOTHING;
  RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS on_public_user_created ON public.users;
CREATE TRIGGER on_public_user_created
  AFTER INSERT ON public.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Cascade delete public.users when auth.users is deleted
CREATE OR REPLACE FUNCTION public.handle_user_delete()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  DELETE FROM public.users WHERE uid = OLD.id::TEXT;
  RETURN OLD;
END;
$$;
DROP TRIGGER IF EXISTS on_auth_user_deleted ON auth.users;
CREATE TRIGGER on_auth_user_deleted
  AFTER DELETE ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_user_delete();

-- Sync post hashtags on insert/update
CREATE OR REPLACE FUNCTION public.sync_post_hashtags()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  tag_match TEXT;
  tag_id UUID;
BEGIN
  DELETE FROM public.post_hashtags WHERE post_id = NEW.id;
  IF NEW.post_text IS NOT NULL THEN
    FOR tag_match IN SELECT DISTINCT lower(m[1])
      FROM regexp_matches(NEW.post_text, '#([A-Za-z0-9_]+)', 'g') AS m
    LOOP
      INSERT INTO public.hashtags (tag, usage_count)
      VALUES (tag_match, 1)
      ON CONFLICT (tag) DO UPDATE SET usage_count = hashtags.usage_count + 1, updated_at = NOW()
      RETURNING id INTO tag_id;
      INSERT INTO public.post_hashtags (post_id, hashtag_id) VALUES (NEW.id, tag_id)
      ON CONFLICT (post_id, hashtag_id) DO NOTHING;
    END LOOP;
  END IF;
  RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS trg_sync_post_hashtags ON public.posts;
CREATE TRIGGER trg_sync_post_hashtags
  AFTER INSERT OR UPDATE OF post_text ON public.posts
  FOR EACH ROW EXECUTE FUNCTION public.sync_post_hashtags();

-- ============================================================
-- FUNCTIONS
-- ============================================================

CREATE OR REPLACE FUNCTION public.increment_hashtag_usage(hashtag_tag TEXT)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO public.hashtags (tag, usage_count) VALUES (hashtag_tag, 1)
  ON CONFLICT (tag) DO UPDATE SET usage_count = hashtags.usage_count + 1, updated_at = NOW();
END;
$$;

CREATE OR REPLACE FUNCTION public.get_trending_hashtags(limit_count INT DEFAULT 10)
RETURNS TABLE(id UUID, tag TEXT, trending_usage_count INT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  RETURN QUERY
  SELECT h.id, h.tag, h.usage_count AS trending_usage_count
  FROM public.hashtags h ORDER BY h.usage_count DESC, h.updated_at DESC LIMIT limit_count;
END;
$$;

CREATE OR REPLACE FUNCTION public.increment_post_views(post_id TEXT)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  UPDATE public.posts SET views_count = views_count + 1 WHERE id = post_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.get_posts_reactions_summary(post_ids TEXT[])
RETURNS TABLE(post_id TEXT, reaction_type TEXT, count BIGINT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  RETURN QUERY
  SELECT r.post_id, r.reaction_type, COUNT(*) as count
  FROM public.reactions r WHERE r.post_id = ANY(post_ids)
  GROUP BY r.post_id, r.reaction_type;
END;
$$;

CREATE OR REPLACE FUNCTION public.get_ranked_post_ids(
    requesting_user_id text,
    limit_val integer DEFAULT 20,
    offset_val integer DEFAULT 0
)
RETURNS TABLE(
    id text, post_id text, item_type text, user_id text,
    "timestamp" bigint, created_at timestamptz,
    content text, parent_post_id text, parent_comment_id text,
    parent_author_username text, reply_to_usernames jsonb,
    likes_count integer, comments_count integer
)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = 'public' AS $$
    SELECT id, post_id, item_type, user_id, timestamp, created_at,
           content, parent_post_id, parent_comment_id, parent_author_username,
           reply_to_usernames, likes_count, comments_count
    FROM (
        -- All public top-level posts
        SELECT p.id, p.id AS post_id, 'post'::text AS item_type,
            p.author_uid AS user_id, p.timestamp, p.created_at,
            NULL::TEXT AS content, NULL::TEXT AS parent_post_id,
            NULL::TEXT AS parent_comment_id, NULL::TEXT AS parent_author_username,
            NULL::JSONB AS reply_to_usernames, 0 AS likes_count, 0 AS comments_count,
            CASE WHEN p.author_uid = requesting_user_id OR p.author_uid IN (
                SELECT following_id FROM public.follows WHERE follower_id = requesting_user_id
            ) THEN 1 ELSE 0 END AS rank_score
        FROM public.posts p
        WHERE p.is_deleted IS NOT TRUE
          AND p.in_reply_to_post_id IS NULL
          AND (p.post_visibility IS NULL OR p.post_visibility = 'public')

        UNION ALL

        -- Reply posts from followed users (X-style "replied to" in feed)
        SELECT p.id, p.id AS post_id, 'post'::text AS item_type,
            p.author_uid AS user_id, p.timestamp, p.created_at,
            p.post_text AS content,
            p.in_reply_to_post_id AS parent_post_id,
            NULL::TEXT AS parent_comment_id,
            parent_u.username AS parent_author_username,
            p.reply_to_usernames, p.likes_count, p.comments_count, 1 AS rank_score
        FROM public.posts p
        LEFT JOIN public.posts parent_p ON parent_p.id = p.in_reply_to_post_id
        LEFT JOIN public.users parent_u ON parent_u.uid = parent_p.author_uid
        WHERE p.is_deleted IS NOT TRUE
          AND p.in_reply_to_post_id IS NOT NULL
          AND (p.post_visibility IS NULL OR p.post_visibility = 'public')
          AND (p.author_uid = requesting_user_id OR p.author_uid IN (
              SELECT following_id FROM public.follows WHERE follower_id = requesting_user_id
          ))

        UNION ALL

        -- Reshares from followed users
        SELECT r.id::TEXT, r.post_id, 'reshare'::text AS item_type,
            r.user_id, EXTRACT(EPOCH FROM r.created_at)::BIGINT * 1000, r.created_at,
            NULL::TEXT, NULL::TEXT, NULL::TEXT, NULL::TEXT, NULL::JSONB, 0, 0, 1 AS rank_score
        FROM public.reshares r
        JOIN public.posts p ON p.id = r.post_id AND p.is_deleted IS NOT TRUE
        WHERE r.user_id IN (
            SELECT following_id FROM public.follows WHERE follower_id = requesting_user_id
        )
    ) feed
    ORDER BY rank_score DESC, created_at DESC
    LIMIT limit_val OFFSET offset_val;
$$;

-- ============================================================
-- STORAGE BUCKETS
-- ============================================================

INSERT INTO storage.buckets (id, name, public) VALUES
  ('avatars',           'avatars',           true),
  ('covers',            'covers',            true),
  ('post-images',       'post-images',       true),
  ('post-media',        'post-media',        true),
  ('chat-attachments',  'chat-attachments',  false),
  ('story-media',       'story-media',       true),
  ('story-thumbnails',  'story-thumbnails',  true),
  ('media',             'media',             true),
  ('public_storage',    'public_storage',    true)
ON CONFLICT (id) DO NOTHING;
