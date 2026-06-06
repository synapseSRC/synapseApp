-- ============================================================
-- SYNAPSE SOCIAL — FULL SCHEMA RESTORE
-- ============================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA extensions;
CREATE EXTENSION IF NOT EXISTS "pg_net";
CREATE EXTENSION IF NOT EXISTS "pg_cron";

-- ============================================================
-- CORE USER TABLES
-- ============================================================

CREATE TABLE public.users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid         TEXT UNIQUE NOT NULL,
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
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.usernames (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid         TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    username    TEXT UNIQUE NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.user_settings (
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

CREATE TABLE public.user_presence (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED,
    status          TEXT DEFAULT 'offline',
    last_seen       TIMESTAMPTZ DEFAULT NOW(),
    current_chat_id TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.user_preferences (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    preferences JSONB DEFAULT '{}',
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.user_passkeys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    credential_id   TEXT UNIQUE NOT NULL,
    device_name     TEXT,
    date_added      BIGINT,
    last_used       BIGINT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.user_api_keys (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    key_hash    TEXT NOT NULL,
    name        TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.user_public_keys (
    user_id     TEXT PRIMARY KEY REFERENCES public.users(uid) ON DELETE CASCADE,
    public_key  TEXT NOT NULL,
    key_version INT DEFAULT 1,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.push_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    token       TEXT NOT NULL,
    platform    TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.profile_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    avatar      TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.cover_image_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    cover_image_url TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- SOCIAL GRAPH
-- ============================================================

CREATE TABLE public.follows (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    following_id    TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(follower_id, following_id)
);

CREATE TABLE public.blocks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id  TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    blocked_id  TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id)
);

CREATE TABLE public.profile_likes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    target_id   TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, target_id)
);

-- ============================================================
-- POSTS
-- ============================================================

CREATE TABLE public.posts (
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
    media_items             JSONB,
    has_poll                BOOLEAN,
    poll_question           TEXT,
    poll_options            JSONB,
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
    is_deleted              BOOLEAN DEFAULT false,
    is_edited               BOOLEAN DEFAULT false,
    edited_at               TIMESTAMPTZ,
    deleted_at              TIMESTAMPTZ,
    reply_to_usernames      JSONB DEFAULT '[]',
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.post_likes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE TABLE public.likes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    target_id   TEXT,
    target_type TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.reactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    reaction_type   TEXT NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE TABLE public.reshares (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    comment     TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE TABLE public.favorites (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    post_id             TEXT REFERENCES public.posts(id) ON DELETE CASCADE,
    collection_id       UUID,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, post_id)
);

CREATE TABLE public.bookmark_collections (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.hidden_posts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, post_id)
);

CREATE TABLE public.poll_votes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    option_index    INT NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE TABLE public.mentions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- HASHTAGS
-- ============================================================

CREATE TABLE public.hashtags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag         TEXT UNIQUE NOT NULL,
    usage_count INT DEFAULT 0,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.post_hashtags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    hashtag_id  UUID NOT NULL REFERENCES public.hashtags(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(post_id, hashtag_id)
);

-- ============================================================
-- REPORTS
-- ============================================================

CREATE TABLE public.reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    target_id   TEXT NOT NULL,
    target_type TEXT NOT NULL,
    reason      TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.post_reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    post_id     TEXT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    reason      TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(reporter_id, post_id)
);

CREATE TABLE public.user_reports (
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

CREATE TABLE public.media_files (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    url         TEXT NOT NULL,
    media_type  TEXT,
    size_bytes  BIGINT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.media_interactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    media_id    UUID REFERENCES public.media_files(id) ON DELETE CASCADE,
    type        TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.media_likes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    media_id    UUID REFERENCES public.media_files(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, media_id)
);

-- ============================================================
-- STORIES
-- ============================================================

CREATE TABLE public.stories (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    media_url               TEXT,
    media_type              TEXT CHECK (media_type IN ('image','video','text')),
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

CREATE TABLE public.story_views (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    viewer_id   TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    viewed_at   TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(story_id, viewer_id)
);

CREATE TABLE public.story_reactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    reaction    TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(story_id, user_id)
);

CREATE TABLE public.story_replies (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.story_mentions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.story_archive (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    archived_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.story_highlights (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    title       TEXT NOT NULL,
    cover_url   TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.story_highlight_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    highlight_id    UUID NOT NULL REFERENCES public.story_highlights(id) ON DELETE CASCADE,
    story_id        UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.story_interactive_elements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    type        TEXT NOT NULL,
    data        JSONB DEFAULT '{}',
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.story_interactive_responses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    element_id      UUID NOT NULL REFERENCES public.story_interactive_elements(id) ON DELETE CASCADE,
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    response        JSONB,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.story_custom_privacy (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.story_hidden_from (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    story_id    UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.close_friends (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    friend_id   TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, friend_id)
);

CREATE TABLE public.tag_requests (
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

CREATE TABLE public.reels (
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

CREATE TABLE public.reel_interactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reel_id     UUID NOT NULL REFERENCES public.reels(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    type        TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.reel_opposers_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reel_id     UUID NOT NULL REFERENCES public.reels(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(reel_id, user_id)
);

CREATE TABLE public.reel_comments (
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

CREATE TABLE public.chats (
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

CREATE TABLE public.chat_participants (
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

CREATE TABLE public.messages (
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

CREATE TABLE public.message_reactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    user_id         TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    reaction_type   TEXT NOT NULL,
    chat_id         UUID REFERENCES public.chats(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(message_id, user_id)
);

CREATE TABLE public.message_edit_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id  UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    edited_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.message_forwards (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_message_id UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    forwarded_message_id UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.user_deleted_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id  UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    user_id     TEXT NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(message_id, user_id)
);

CREATE TABLE public.typing_status (
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

CREATE TABLE public.notifications (
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

CREATE TABLE public.notification_preferences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    enabled         BOOLEAN DEFAULT true,
    settings        JSONB DEFAULT '{}',
    quiet_hours     JSONB DEFAULT '{}',
    do_not_disturb  BOOLEAN DEFAULT false,
    dnd_until       TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.notification_analytics (
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
-- BUSINESS & MISC
-- ============================================================

CREATE TABLE public.business_accounts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 TEXT UNIQUE NOT NULL REFERENCES public.users(uid) ON DELETE CASCADE,
    account_type            TEXT NOT NULL,
    monetization_enabled    BOOLEAN DEFAULT false,
    verification_status     TEXT DEFAULT 'pending',
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.news_articles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_name     TEXT,
    headline        TEXT NOT NULL,
    image_url       TEXT,
    url             TEXT,
    published_at    TIMESTAMPTZ DEFAULT NOW(),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.changelogs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version     TEXT NOT NULL,
    content     TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.syra_trigger_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event       TEXT,
    data        JSONB,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.family_connections (
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

CREATE INDEX idx_posts_author_uid ON public.posts(author_uid);
CREATE INDEX idx_posts_created_at ON public.posts(created_at DESC);
CREATE INDEX idx_posts_root_post_id ON public.posts(root_post_id);
CREATE INDEX idx_follows_follower ON public.follows(follower_id);
CREATE INDEX idx_follows_following ON public.follows(following_id);
CREATE INDEX idx_messages_chat_id ON public.messages(chat_id);
CREATE INDEX idx_messages_sender_id ON public.messages(sender_id);
CREATE INDEX idx_messages_created_at ON public.messages(created_at DESC);
CREATE INDEX idx_notifications_recipient ON public.notifications(recipient_id);
CREATE INDEX idx_notifications_created_at ON public.notifications(created_at DESC);
CREATE INDEX idx_stories_user_id ON public.stories(user_id);
CREATE INDEX idx_stories_expires_at ON public.stories(expires_at);
CREATE INDEX idx_hashtags_tag ON public.hashtags(tag);
CREATE INDEX idx_post_hashtags_post_id ON public.post_hashtags(post_id);
CREATE INDEX idx_post_hashtags_hashtag_id ON public.post_hashtags(hashtag_id);
CREATE INDEX idx_reactions_post_id ON public.reactions(post_id);
CREATE INDEX idx_user_presence_current_chat_id ON public.user_presence(current_chat_id);
CREATE INDEX idx_chat_participants_user_id ON public.chat_participants(user_id);
CREATE INDEX idx_chat_participants_chat_id ON public.chat_participants(chat_id);

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
-- HELPER FUNCTION: get current user uid
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_current_user_uid()
RETURNS TEXT
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT COALESCE(
    (SELECT uid FROM public.users WHERE id = auth.uid() LIMIT 1),
    auth.uid()::TEXT
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

-- user_settings
CREATE POLICY "Users manage own settings" ON public.user_settings FOR ALL USING (public.get_current_user_uid() = user_id);

-- user_presence
CREATE POLICY "Presence is publicly readable" ON public.user_presence FOR SELECT USING (true);
CREATE POLICY "Users manage own presence" ON public.user_presence FOR ALL USING (public.get_current_user_uid() = user_id);

-- user_preferences
CREATE POLICY "Users manage own preferences" ON public.user_preferences FOR ALL USING (public.get_current_user_uid() = user_id);

-- user_passkeys / user_api_keys
CREATE POLICY "Users manage own passkeys" ON public.user_passkeys FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Users manage own api keys" ON public.user_api_keys FOR ALL USING (public.get_current_user_uid() = user_id);

-- user_public_keys
CREATE POLICY "Public keys are readable" ON public.user_public_keys FOR SELECT USING (true);
CREATE POLICY "Users manage own public key" ON public.user_public_keys FOR ALL USING (public.get_current_user_uid() = user_id);

-- push_tokens
CREATE POLICY "Users manage own push tokens" ON public.push_tokens FOR ALL USING (public.get_current_user_uid() = user_id);

-- profile/cover history
CREATE POLICY "Profile history readable" ON public.profile_history FOR SELECT USING (true);
CREATE POLICY "Users manage own profile history" ON public.profile_history FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Cover history readable" ON public.cover_image_history FOR SELECT USING (true);
CREATE POLICY "Users manage own cover history" ON public.cover_image_history FOR ALL USING (public.get_current_user_uid() = user_id);

-- follows
CREATE POLICY "Follows are publicly readable" ON public.follows FOR SELECT USING (true);
CREATE POLICY "Users manage own follows" ON public.follows FOR ALL USING (public.get_current_user_uid() = follower_id);

-- blocks
CREATE POLICY "Users manage own blocks" ON public.blocks FOR ALL USING (public.get_current_user_uid() = blocker_id);
CREATE POLICY "Users can see if blocked" ON public.blocks FOR SELECT USING (public.get_current_user_uid() = blocked_id);

-- profile_likes
CREATE POLICY "Profile likes readable" ON public.profile_likes FOR SELECT USING (true);
CREATE POLICY "Users manage own profile likes" ON public.profile_likes FOR ALL USING (public.get_current_user_uid() = user_id);

-- posts
CREATE POLICY "Posts are publicly readable" ON public.posts FOR SELECT USING (true);
CREATE POLICY "Users can insert own posts" ON public.posts FOR INSERT WITH CHECK (public.get_current_user_uid() = author_uid);
CREATE POLICY "Users can update own posts" ON public.posts FOR UPDATE USING (public.get_current_user_uid() = author_uid);
CREATE POLICY "Users can delete own posts" ON public.posts FOR DELETE USING (public.get_current_user_uid() = author_uid);

-- post_likes / likes / reactions / reshares / favorites
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

-- hashtags / post_hashtags
CREATE POLICY "Hashtags are public" ON public.hashtags FOR SELECT USING (true);
CREATE POLICY "Authenticated can insert hashtags" ON public.hashtags FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Post hashtags are public" ON public.post_hashtags FOR SELECT USING (true);
CREATE POLICY "Authenticated can insert post hashtags" ON public.post_hashtags FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Users can delete own post hashtags" ON public.post_hashtags FOR DELETE USING (
    EXISTS (SELECT 1 FROM public.posts WHERE id = post_hashtags.post_id AND author_uid = public.get_current_user_uid())
);

-- mentions
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

-- chats
CREATE POLICY "Participants can see their chats" ON public.chats FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.chat_participants WHERE chat_id = chats.id AND user_id = public.get_current_user_uid())
);
CREATE POLICY "Authenticated can create chats" ON public.chats FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Participants can update chats" ON public.chats FOR UPDATE USING (
    EXISTS (SELECT 1 FROM public.chat_participants WHERE chat_id = chats.id AND user_id = public.get_current_user_uid() AND is_admin = true)
);

-- chat_participants
CREATE POLICY "Participants can view participants" ON public.chat_participants FOR SELECT USING (
    user_id = public.get_current_user_uid() OR
    EXISTS (SELECT 1 FROM public.chat_participants cp2 WHERE cp2.chat_id = chat_participants.chat_id AND cp2.user_id = public.get_current_user_uid())
);
CREATE POLICY "Authenticated can insert participants" ON public.chat_participants FOR INSERT WITH CHECK (auth.role() = 'authenticated');
CREATE POLICY "Users manage own participation" ON public.chat_participants FOR UPDATE USING (user_id = public.get_current_user_uid());
CREATE POLICY "Users can leave chats" ON public.chat_participants FOR DELETE USING (user_id = public.get_current_user_uid());

-- messages
CREATE POLICY "Participants can read messages" ON public.messages FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.chat_participants WHERE chat_id = messages.chat_id AND user_id = public.get_current_user_uid())
);
CREATE POLICY "Participants can send messages" ON public.messages FOR INSERT WITH CHECK (
    public.get_current_user_uid() = sender_id AND
    EXISTS (SELECT 1 FROM public.chat_participants WHERE chat_id = messages.chat_id AND user_id = public.get_current_user_uid())
);
CREATE POLICY "Sender can update own messages" ON public.messages FOR UPDATE USING (public.get_current_user_uid() = sender_id);
CREATE POLICY "Sender can delete own messages" ON public.messages FOR DELETE USING (public.get_current_user_uid() = sender_id);

-- message sub-tables
CREATE POLICY "Message reactions readable by participants" ON public.message_reactions FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.messages m JOIN public.chat_participants cp ON cp.chat_id = m.chat_id WHERE m.id = message_reactions.message_id AND cp.user_id = public.get_current_user_uid())
);
CREATE POLICY "Users manage own message reactions" ON public.message_reactions FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Message edit history readable" ON public.message_edit_history FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.messages m JOIN public.chat_participants cp ON cp.chat_id = m.chat_id WHERE m.id = message_edit_history.message_id AND cp.user_id = public.get_current_user_uid())
);
CREATE POLICY "Users manage own deleted messages" ON public.user_deleted_messages FOR ALL USING (public.get_current_user_uid() = user_id);
CREATE POLICY "Typing status readable by participants" ON public.typing_status FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.chat_participants WHERE chat_id = typing_status.chat_id AND user_id = public.get_current_user_uid())
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
-- TRIGGER: auto-create user_settings + user_presence on signup
-- ============================================================

CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.user_settings (user_id)
  VALUES (NEW.id::TEXT)
  ON CONFLICT (user_id) DO NOTHING;

  INSERT INTO public.user_presence (user_id)
  VALUES (NEW.id::TEXT)
  ON CONFLICT (user_id) DO NOTHING;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_auth_user();

-- ============================================================
-- TRIGGER: cascade user deletion cleanup
-- ============================================================

CREATE OR REPLACE FUNCTION public.handle_user_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  DELETE FROM public.users WHERE uid = OLD.id::TEXT;
  RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_deleted ON auth.users;
CREATE TRIGGER on_auth_user_deleted
  AFTER DELETE ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_user_delete();

-- ============================================================
-- FUNCTION: increment hashtag usage
-- ============================================================

CREATE OR REPLACE FUNCTION public.increment_hashtag_usage(hashtag_tag TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.hashtags (tag, usage_count)
  VALUES (hashtag_tag, 1)
  ON CONFLICT (tag) DO UPDATE SET usage_count = hashtags.usage_count + 1, updated_at = NOW();
END;
$$;

-- ============================================================
-- FUNCTION: get trending hashtags
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_trending_hashtags(limit_count INT DEFAULT 10)
RETURNS TABLE(id UUID, tag TEXT, trending_usage_count INT)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  RETURN QUERY
  SELECT h.id, h.tag, h.usage_count AS trending_usage_count
  FROM public.hashtags h
  ORDER BY h.usage_count DESC, h.updated_at DESC
  LIMIT limit_count;
END;
$$;

-- ============================================================
-- FUNCTION: increment post views
-- ============================================================

CREATE OR REPLACE FUNCTION public.increment_post_views(post_id TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  UPDATE public.posts SET views_count = views_count + 1 WHERE id = post_id;
END;
$$;

-- ============================================================
-- FUNCTION: get posts reactions summary
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_posts_reactions_summary(post_ids TEXT[])
RETURNS TABLE(post_id TEXT, reaction_type TEXT, count BIGINT)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  RETURN QUERY
  SELECT r.post_id, r.reaction_type, COUNT(*) as count
  FROM public.reactions r
  WHERE r.post_id = ANY(post_ids)
  GROUP BY r.post_id, r.reaction_type;
END;
$$;

-- ============================================================
-- STORAGE BUCKETS
-- ============================================================

INSERT INTO storage.buckets (id, name, public) VALUES
  ('avatars', 'avatars', true),
  ('covers', 'covers', true),
  ('post-images', 'post-images', true),
  ('post-media', 'post-media', true),
  ('chat-attachments', 'chat-attachments', false),
  ('story-media', 'story-media', true),
  ('story-thumbnails', 'story-thumbnails', true),
  ('media', 'media', true),
  ('public_storage', 'public_storage', true)
ON CONFLICT (id) DO NOTHING;
