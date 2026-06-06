-- Test with empty user id to see all posts returned
SELECT id, post_id, item_type, user_id, parent_post_id, parent_author_username
FROM get_ranked_post_ids('', 20, 0);
