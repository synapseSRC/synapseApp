-- Feed function: returns ranked post IDs for the home timeline
-- Returns posts + reshares from followed users, ordered by recency
-- item_type: 'post' | 'reshare' | 'comment'

CREATE OR REPLACE FUNCTION public.get_ranked_post_ids(
    requesting_user_id TEXT,
    limit_val INT DEFAULT 20,
    offset_val INT DEFAULT 0
)
RETURNS TABLE (
    id TEXT,
    post_id TEXT,
    item_type TEXT,
    user_id TEXT,
    "timestamp" BIGINT,
    created_at TIMESTAMPTZ,
    -- comment fields
    content TEXT,
    parent_post_id TEXT,
    parent_comment_id TEXT,
    parent_author_username TEXT,
    reply_to_usernames JSONB,
    likes_count INT,
    comments_count INT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    -- Posts from followed users + own posts
    SELECT
        p.id AS id,
        p.id AS post_id,
        'post' AS item_type,
        p.author_uid AS user_id,
        p.timestamp AS timestamp,
        p.created_at AS created_at,
        NULL::TEXT AS content,
        NULL::TEXT AS parent_post_id,
        NULL::TEXT AS parent_comment_id,
        NULL::TEXT AS parent_author_username,
        NULL::JSONB AS reply_to_usernames,
        0 AS likes_count,
        0 AS comments_count
    FROM public.posts p
    WHERE
        p.is_deleted IS NOT TRUE
        AND p.in_reply_to_post_id IS NULL
        AND (
            p.author_uid = requesting_user_id
            OR p.author_uid IN (
                SELECT following_id FROM public.follows WHERE follower_id = requesting_user_id
            )
            OR requesting_user_id = ''
        )

    UNION ALL

    -- Reshares from followed users
    SELECT
        r.id::TEXT AS id,
        r.post_id AS post_id,
        'reshare' AS item_type,
        r.user_id AS user_id,
        EXTRACT(EPOCH FROM r.created_at)::BIGINT * 1000 AS timestamp,
        r.created_at AS created_at,
        NULL::TEXT AS content,
        NULL::TEXT AS parent_post_id,
        NULL::TEXT AS parent_comment_id,
        NULL::TEXT AS parent_author_username,
        NULL::JSONB AS reply_to_usernames,
        0 AS likes_count,
        0 AS comments_count
    FROM public.reshares r
    JOIN public.posts p ON p.id = r.post_id AND p.is_deleted IS NOT TRUE
    WHERE
        r.user_id IN (
            SELECT following_id FROM public.follows WHERE follower_id = requesting_user_id
        )

    ORDER BY created_at DESC
    LIMIT limit_val
    OFFSET offset_val;
$$;
