-- The INSERT RLS policy on posts uses get_current_user_uid() which does a
-- secondary lookup in public.users. Under SECURITY DEFINER this can return
-- NULL if auth.uid() is not available in that execution context.
--
-- Fix: replace the with_check expression with a direct auth.uid()::text
-- comparison, which is always evaluated in the caller's auth context.

DROP POLICY IF EXISTS "Users can insert own posts" ON posts;

CREATE POLICY "Users can insert own posts"
  ON posts
  FOR INSERT
  WITH CHECK (auth.uid()::text = author_uid);
