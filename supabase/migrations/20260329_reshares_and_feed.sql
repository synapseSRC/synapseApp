CREATE TABLE IF NOT EXISTS public.reshares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    reshare_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(post_id, user_id)
);
CREATE INDEX IF NOT EXISTS reshares_user_id_idx ON public.reshares(user_id);
CREATE INDEX IF NOT EXISTS reshares_post_id_idx ON public.reshares(post_id);
ALTER TABLE public.reshares ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can manage their own reshares" ON public.reshares
    FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Reshares are publicly readable" ON public.reshares
    FOR SELECT USING (true);

CREATE OR REPLACE FUNCTION public.increment_post_reshares(post_id UUID)
RETURNS void LANGUAGE sql SECURITY DEFINER AS $$
    UPDATE public.posts SET reshares_count = COALESCE(reshares_count, 0) + 1 WHERE id = post_id;
$$;

CREATE OR REPLACE FUNCTION public.decrement_post_reshares(post_id UUID)
RETURNS void LANGUAGE sql SECURITY DEFINER AS $$
    UPDATE public.posts SET reshares_count = GREATEST(COALESCE(reshares_count, 0) - 1, 0) WHERE id = post_id;
$$;

CREATE OR REPLACE VIEW public.feed_timeline AS
SELECT
    p.id,
    'post' AS item_type,
    p.id AS post_id,
    p.author_uid AS user_id,
    EXTRACT(EPOCH FROM p.created_at) * 1000 AS timestamp,
    p.created_at,
    NULL::UUID AS parent_post_id,
    NULL::UUID AS parent_comment_id,
    NULL::TEXT AS parent_author_username,
    p.post_text AS content,
    p.likes_count,
    p.comments_count
FROM public.posts p
WHERE p.author_uid IN (SELECT following_id FROM public.follows WHERE follower_id = auth.uid()) OR p.author_uid = auth.uid()

UNION ALL

SELECT
    r.id,
    'reshare' AS item_type,
    r.post_id AS post_id,
    r.user_id AS user_id,
    EXTRACT(EPOCH FROM r.created_at) * 1000 AS timestamp,
    r.created_at,
    NULL::UUID AS parent_post_id,
    NULL::UUID AS parent_comment_id,
    NULL::TEXT AS parent_author_username,
    r.reshare_text AS content,
    0 AS likes_count,
    0 AS comments_count
FROM public.reshares r
WHERE r.user_id IN (SELECT following_id FROM public.follows WHERE follower_id = auth.uid()) OR r.user_id = auth.uid()

UNION ALL

SELECT
    c.id,
    'comment' AS item_type,
    c.post_id AS post_id,
    c.user_id AS user_id,
    EXTRACT(EPOCH FROM c.created_at) * 1000 AS timestamp,
    c.created_at,
    c.post_id AS parent_post_id,
    c.parent_comment_id AS parent_comment_id,
    NULL::TEXT AS parent_author_username,
    c.content,
    c.likes_count,
    c.replies_count AS comments_count
FROM public.comments c
WHERE c.user_id IN (SELECT following_id FROM public.follows WHERE follower_id = auth.uid()) OR c.user_id = auth.uid();
