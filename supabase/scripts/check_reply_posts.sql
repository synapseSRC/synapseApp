SELECT COUNT(*) AS total_posts,
       COUNT(*) FILTER (WHERE in_reply_to_post_id IS NOT NULL) AS reply_posts,
       COUNT(*) FILTER (WHERE in_reply_to_post_id IS NULL) AS top_level_posts
FROM posts WHERE is_deleted IS NOT TRUE;
