import type { UserProfile } from '@/types';

export type Audience = 'public' | 'followers' | 'private';
export type ToolKey = 'media' | 'poll' | 'tagged' | 'feeling' | 'location' | 'youtube' | 'schedule' | 'settings' | 'thread';
export type PostToolState = Record<ToolKey, boolean>;
export type MediaAttachment = { id: string; file: File; previewUrl: string; kind: 'image' | 'video' };
export type TaggedPerson = { uid: string; username: string | null; display_name: string | null; avatar: string | null };
export type LocationChoice = { name: string; address: string; latitude: number | null; longitude: number | null; placeId: string | null };
export type PlaceResult = { place_id?: number | string; name?: string; display_name?: string; lat?: string; lon?: string; address?: Record<string, string> };

export interface CreatePostScreenProps {
  user: UserProfile;
  onClose: () => void;
  onPostCreated?: () => void;
}

export const MAX_MEDIA_FILES = 8;
export const MAX_MEDIA_BYTES = 10 * 1024 * 1024;
export const ALLOWED_MEDIA = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'video/mp4', 'video/quicktime']);
export const POLL_DURATIONS = [
  { value: 1, label: '1 hour' },
  { value: 6, label: '6 hours' },
  { value: 24, label: '1 day' },
  { value: 72, label: '3 days' },
  { value: 168, label: '7 days' },
] as const;
export const TEXT_BACKGROUNDS = [
  { value: 'none', label: 'Plain', color: '#ffffff', text: '#18181b' },
  { value: '#f4f4f5', label: 'Mist', color: '#f4f4f5', text: '#18181b' },
  { value: '#e4e4e7', label: 'Stone', color: '#e4e4e7', text: '#18181b' },
  { value: '#18181b', label: 'Ink', color: '#18181b', text: '#ffffff' },
] as const;

export const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0] || '').join('').toUpperCase() || 'S';
export const getDraftKey = (userId: string) => `synapse:create-post:draft:${userId}`;
export const loadDraftText = (key: string) => {
  try { return window.localStorage.getItem(key) || ''; } catch { return ''; }
};
export const localDateTime = (date: Date) => {
  const adjusted = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return adjusted.toISOString().slice(0, 16);
};
export const safeFileName = (name: string) => name.replace(/[^a-zA-Z0-9._-]+/g, '-').slice(-80) || 'upload';
export const formatBytes = (bytes: number) => bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
export const isYouTubeUrl = (value: string) => {
  try {
    const url = new URL(value);
    return ['youtube.com', 'www.youtube.com', 'm.youtube.com', 'youtu.be', 'www.youtu.be'].includes(url.hostname.toLowerCase());
  } catch { return false; }
};
export const profileLabel = (person: TaggedPerson) => person.display_name || person.username || 'Synapse member';
