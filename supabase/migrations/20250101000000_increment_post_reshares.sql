CREATE OR REPLACE FUNCTION increment_post_reshares(post_id UUID)
RETURNS void
LANGUAGE sql
AS $$
  UPDATE posts
  SET reshares_count = reshares_count + 1
  WHERE id = post_id;
$$;
