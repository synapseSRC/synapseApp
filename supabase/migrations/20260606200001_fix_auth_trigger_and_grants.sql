-- Fix 1: Move user_settings/user_presence creation to public.users trigger (not auth trigger)
-- This avoids FK violation since public.users row doesn't exist at auth signup time

ALTER TABLE public.user_settings DROP CONSTRAINT IF EXISTS user_settings_user_id_fkey;
ALTER TABLE public.user_presence DROP CONSTRAINT IF EXISTS user_presence_user_id_fkey;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.user_settings (user_id)
  VALUES (NEW.uid)
  ON CONFLICT (user_id) DO NOTHING;

  INSERT INTO public.user_presence (user_id)
  VALUES (NEW.uid)
  ON CONFLICT (user_id) DO NOTHING;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_public_user_created ON public.users;
CREATE TRIGGER on_public_user_created
  AFTER INSERT ON public.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Fix 2: Grant table permissions to authenticated/anon roles

GRANT USAGE ON SCHEMA public TO authenticated, anon;
GRANT SELECT, INSERT, UPDATE ON public.users TO authenticated;
GRANT SELECT ON public.users TO anon;
GRANT SELECT, INSERT, UPDATE ON public.user_settings TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.user_presence TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.usernames TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.follows TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.blocks TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.posts TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reshares TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.favorites TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.post_likes TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.poll_votes TO authenticated;
GRANT SELECT, INSERT ON public.hashtags TO authenticated;
GRANT SELECT, INSERT, DELETE ON public.post_hashtags TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.chats TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.chat_participants TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.messages TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.message_reactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.typing_status TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_deleted_messages TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.notifications TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.notification_preferences TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.stories TO authenticated;
GRANT SELECT, INSERT ON public.story_views TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_reactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_replies TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reels TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reel_comments TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reel_interactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.reel_opposers_log TO authenticated;
GRANT SELECT, INSERT ON public.reports TO authenticated;
GRANT SELECT, INSERT ON public.post_reports TO authenticated;
GRANT SELECT, INSERT ON public.user_reports TO authenticated;
GRANT SELECT ON public.news_articles TO authenticated, anon;
GRANT SELECT ON public.changelogs TO authenticated, anon;
GRANT SELECT, INSERT, UPDATE ON public.user_public_keys TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.push_tokens TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_preferences TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_passkeys TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.profile_history TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.cover_image_history TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.profile_likes TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.bookmark_collections TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.hidden_posts TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.mentions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.media_files TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.media_interactions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.media_likes TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.close_friends TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.family_connections TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.message_edit_history TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.notification_analytics TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_highlights TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_highlight_items TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_archive TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_mentions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_interactive_elements TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_interactive_responses TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_custom_privacy TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.story_hidden_from TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.tag_requests TO authenticated;
