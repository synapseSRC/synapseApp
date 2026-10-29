// Column names were verified against the live Data API on 2026-10-28; Postgres types
// follow the supabase-kt DTOs in synapseApp/shared, so treat widths as advisory.

export interface ProfileRow {
    uid: string;
    username: string | null;
    nickname: string | null;
    display_name: string | null;
    bio: string | null;
    avatar: string | null;
    verify: boolean | null;
    is_private: boolean | null;
    followers_count: number | null;
    following_count: number | null;
    posts_count: number | null;
    profile_cover_image: string | null;
    join_date: string | null;
    created_at: string | null;
}

export interface OwnProfileRow extends ProfileRow {
    website: string | null;
    location: string | null;
    pronouns: string | null;
    work: string | null;
    education: string | null;
    relationship_status: string | null;
}

export interface UserRow extends ProfileRow {
    id: number | null;
    email: string | null;
    avatar_history_type: string | null;
    account_premium: boolean | null;
    user_level_xp: number | null;
    account_type: string | null;
    gender: string | null;
    banned: boolean | null;
    status: string | null;
    one_signal_player_id: string | null;
    last_seen: string | null;
    chatting_with: string | null;
    location: string | null;
    relationship_status: string | null;
    birthday: string | null;
    work: string | null;
    education: string | null;
    current_city: string | null;
    hometown: string | null;
    website: string | null;
    pronouns: string | null;
    linked_accounts: unknown;
    privacy_settings: unknown;
    is_admin: boolean | null;
    updated_at: string | null;
    discord_tag: string | null;
    github_profile: string | null;
    occupation: string | null;
    workplace: string | null;
    personal_website: string | null;
    public_email: string | null;
    region: string | null;
    interests: unknown;
    featured_post_ids: unknown;
    featured_media_urls: unknown;
    profile_layout: string | null;
}

export interface MediaItem {
    url: string;
    type: string;
    width?: number | null;
    height?: number | null;
    duration?: number | null;
}

export interface PostRow {
    id: string;
    author_uid: string;
    post_text: string | null;
    post_image: string | null;
    post_type: string | null;
    post_visibility: string | null;
    timestamp: number;
    created_at: string | null;
    updated_at: string | null;
    publish_date: string | null;
    likes_count: number | null;
    comments_count: number | null;
    views_count: number | null;
    reshares_count: number | null;
    in_reply_to_post_id: string | null;
    root_post_id: string | null;
    reply_to_usernames: unknown;
    quoted_post_id: string | null;
    is_quote: boolean | null;
    media_items: MediaItem[] | null;
    is_deleted: boolean | null;
    is_edited: boolean | null;
    edited_at: string | null;
    deleted_at: string | null;
    has_poll: boolean | null;
    poll_question: string | null;
    poll_options: unknown;
    poll_end_time: string | null;
    poll_allow_multiple: boolean | null;
    has_location: boolean | null;
    location_name: string | null;
    location_address: string | null;
    location_latitude: number | null;
    location_longitude: number | null;
    youtube_url: string | null;
    link_previews: unknown;
    metadata: unknown;
    post_hide_views_count: string | null;
    post_hide_like_count: string | null;
    post_hide_comments_count: string | null;
    post_disable_comments: string | null;
}

export interface PostAuthor {
    uid: string;
    username: string | null;
    display_name: string | null;
    avatar: string | null;
    verify: boolean | null;
}

export interface ChatRow {
    id: string;
    name: string | null;
    description: string | null;
    avatar_url: string | null;
    is_group: boolean | null;
    created_by: string | null;
    only_admins_can_message: boolean | null;
    disappearing_mode: string | null;
    allow_member_settings_edits: boolean | null;
    require_join_approval: boolean | null;
    allow_member_invites: boolean | null;
    invite_code: string | null;
    created_at: string | null;
    updated_at: string | null;
}

export interface ChatParticipantRow {
    id: number | null;
    chat_id: string;
    user_id: string;
    is_admin: boolean | null;
    is_archived: boolean | null;
    is_pinned: boolean | null;
    is_muted: boolean | null;
    last_read_at: string | null;
}

export interface MessageRow {
    id: string;
    chat_id: string;
    sender_id: string;
    content: string;
    message_type: string | null;
    media_url: string | null;
    delivery_status: string | null;
    message_state: string | null;
    content_state: string | null;
    is_deleted: boolean | null;
    is_edited: boolean | null;
    reply_to_id: string | null;
    sent_at: string | null;
    delivered_at: string | null;
    read_at: string | null;
    edited_at: string | null;
    deleted_at: string | null;
    created_at: string | null;
    updated_at: string | null;
    read_by: string[] | null;
    expires_at: string | null;
    failure_reason: string | null;
    client_id: string | null;
    device_id: string | null;
    attachments: MediaItem[] | null;
    metadata: unknown;
}

export interface NotificationRow {
    id: string;
    recipient_id: string;
    sender_id: string | null;
    type: string;
    title: Record<string, unknown> | null;
    body: Record<string, unknown> | null;
    data: Record<string, unknown> | null;
    priority: number | null;
    delivery_status: string | null;
    is_read: boolean | null;
    read_at: string | null;
    interacted_at: string | null;
    created_at: string;
    expires_at: string | null;
}
