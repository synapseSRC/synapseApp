-- Test with both user IDs
SELECT 'user1' AS u, id, post_id, item_type, parent_post_id FROM get_ranked_post_ids('590c32a0-7847-4f39-9beb-3b649d989bf7', 20, 0)
UNION ALL
SELECT 'user2' AS u, id, post_id, item_type, parent_post_id FROM get_ranked_post_ids('36c38a2a-d86f-4945-959f-35349eb634f3', 20, 0)
ORDER BY u, item_type;
