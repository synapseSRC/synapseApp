import { backend as supabase } from '@/lib/backend';
import type { Story } from './types';

export const getStories = async (): Promise<Story[]> => {
  const { data, error } = await supabase
    .from('stories')
    .select('id,user_id,content,media_url,thumbnail_url,media_type,created_at,expires_at,is_active')
    .eq('is_active', true)
    .order('created_at', { ascending: false })
    .limit(50);
  if (error) throw error;
  return (data || []) as Story[];
};

export const markStoryAsSeen = async (storyId: string, viewerId: string) => {
  await supabase.from('story_views').insert({ story_id: storyId, viewer_id: viewerId });
};

export const deleteStory = async (storyId: string) => {
  await supabase.from('stories').update({ is_active: false }).eq('id', storyId);
};

export const createStory = async (params: { mediaUrl: string; mediaType: string; content?: string | null }) => {
  const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
  const { data, error } = await supabase
    .from('stories')
    .insert({ media_url: params.mediaUrl, media_type: params.mediaType, content: params.content ?? null, expires_at: expiresAt, is_active: true })
    .select('id')
    .single();
  if (error) throw error;
  return data;
};
