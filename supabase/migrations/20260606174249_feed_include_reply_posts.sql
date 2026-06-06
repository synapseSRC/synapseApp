-- Comments (reply posts) from followed users were excluded by
-- "AND p.in_reply_to_post_id IS NULL". Add them back as a separate
-- union branch so the feed shows "UserA replied to UserB" items,
-- matching X/Twitter behaviour.

CREATE OR REPLACE FUNCTION public.get_ranked_post_ids(
    requesting_user_id text,
    limit_val integer DEFAULT 20,
    offset_val integer DEFAULT 0
)
RETURNS TABLE(
    id text, post_id text, item_type text, user_id text,
    "timestamp" bigint, created_at timestamp with time zone,
    content text, parent_post_id text, parent_comment_id text,
    parent_author_username text, reply_to_usernames jsonb,
    likes_count integer, comments_count integer
)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path TO 'public'
AS $$
    SELECT id, post_id, item_type, user_id, timestamp, created_at,
           content, parent_post_id, parent_comment_id, parent_author_username,
           reply_to_usernames, likes_count, comments_count
    FROM (
        -- All public top-level posts
        SELECT
            p.id, p.id AS post_id, 'post'::text AS item_type,
            p.author_uid AS user_id, p.timestamp, p.created_at,
            NULL::TEXT AS content, NULL::TEXT AS parent_post_id,
            NULL::TEXT AS parent_comment_id, NULL::TEXT AS parent_author_username,
            NULL::JSONB AS reply_to_usernames,
            0 AS likes_count, 0 AS comments_count,
            CASE WHEN p.author_uid = requesting_user_id OR p.author_uid IN (
                SELECT following_id FROM public.follows WHERE follower_id = requesting_user_id
            ) THEN 1 ELSE 0 END AS rank_score
        FROM public.posts p
        WHERE p.is_deleted IS NOT TRUE
          AND p.in_reply_to_post_id IS NULL
          AND (p.post_visibility IS NULL OR p.post_visibility = 'public')

        UNION ALL

        -- Reply posts (comments) from followed users + own — shown like X "replied to"
        SELECT
            p.id, p.id AS post_id, 'post'::text AS item_type,
            p.author_uid AS user_id, p.timestamp, p.created_at,
            p.post_text AS content,
            p.in_reply_to_post_id AS parent_post_id,
            NULL::TEXT AS parent_comment_id,
            parent_u.username AS parent_author_username,
            p.reply_to_usernames,
            p.likes_count, p.comments_count,
            1 AS rank_score
        FROM public.posts p
        LEFT JOIN public.posts parent_p ON parent_p.id = p.in_reply_to_post_id
        LEFT JOIN public.users parent_u ON parent_u.uid = parent_p.author_uid
        WHERE p.is_deleted IS NOT TRUE
          AND p.in_reply_to_post_id IS NOT NULL
          AND (p.post_visibility IS NULL OR p.post_visibility = 'public')
          AND (
              p.author_uid = requesting_user_id
              OR p.author_uid IN (
                  SELECT following_id FROM public.follows WHERE follower_id = requesting_user_id
              )
          )

        UNION ALL

        -- Reshares from followed users
        SELECT
            r.id::TEXT, r.post_id, 'reshare'::text AS item_type,
            r.user_id, EXTRACT(EPOCH FROM r.created_at)::BIGINT * 1000, r.created_at,
            NULL::TEXT, NULL::TEXT, NULL::TEXT, NULL::TEXT, NULL::JSONB,
            0, 0,
            1 AS rank_score
        FROM public.reshares r
        JOIN public.posts p ON p.id = r.post_id AND p.is_deleted IS NOT TRUE
        WHERE r.user_id IN (
            SELECT following_id FROM public.follows WHERE follower_id = requesting_user_id
        )
    ) feed
    ORDER BY rank_score DESC, created_at DESC
    LIMIT limit_val
    OFFSET offset_val;
$$;
