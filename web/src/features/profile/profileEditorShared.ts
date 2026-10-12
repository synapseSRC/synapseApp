import type { LucideIcon } from 'lucide-react';
import { BadgeCheck, Briefcase, CalendarDays, Camera, Globe, Heart, Image, MapPin, Star, UserRound } from 'lucide-react';
import type { UserProfile } from '@/types';

export type ProfileLayout = 'feed' | 'grid' | 'highlights';

export const EDITABLE_PROFILE_SELECT = 'uid,username,nickname,display_name,avatar,profile_cover_image,bio,website,region,gender,current_city,hometown,occupation,workplace,education,pronouns,birthday,relationship_status,discord_tag,github_profile,personal_website,public_email,account_type,interests,featured_post_ids,featured_media_urls,profile_layout,created_at,followers_count,following_count,posts_count,verify';

export interface EditableProfile {
  uid: string;
  username: string | null;
  nickname: string | null;
  display_name: string | null;
  avatar: string | null;
  profile_cover_image: string | null;
  bio: string | null;
  website: string | null;
  region: string | null;
  gender: string | null;
  current_city: string | null;
  hometown: string | null;
  occupation: string | null;
  workplace: string | null;
  education: string | null;
  pronouns: string | null;
  birthday: string | null;
  relationship_status: string | null;
  discord_tag: string | null;
  github_profile: string | null;
  personal_website: string | null;
  public_email: string | null;
  account_type: string | null;
  interests: unknown;
  featured_post_ids: unknown;
  featured_media_urls: unknown;
  profile_layout: ProfileLayout | null;
  created_at: string | null;
  followers_count: number | null;
  following_count: number | null;
  posts_count: number | null;
  verify: boolean | null;
}

export interface ProfileEditorProps {
  user: UserProfile;
  profile: EditableProfile;
  onCancel: () => void;
  onSaved: (profile: EditableProfile, warning?: string) => void;
}

export type SectionId = 'basic' | 'media' | 'personal' | 'location' | 'work' | 'social' | 'interests' | 'professional' | 'appearance' | 'featured' | 'layout';
export type SectionMeta = { id: SectionId; title: string; subtitle: string; icon: LucideIcon; group: 'Profile' | 'Professional' | 'Customize' };

export const SECTIONS: SectionMeta[] = [
  { id: 'basic', title: 'Basic information', subtitle: 'Display name, username and bio', icon: UserRound, group: 'Profile' },
  { id: 'media', title: 'Profile media', subtitle: 'Profile photo, cover and photo history', icon: Camera, group: 'Profile' },
  { id: 'personal', title: 'Personal information', subtitle: 'Gender, pronouns, birthday and relationship', icon: CalendarDays, group: 'Profile' },
  { id: 'location', title: 'Location', subtitle: 'Region, current city and hometown', icon: MapPin, group: 'Profile' },
  { id: 'work', title: 'Work & education', subtitle: 'Occupation, workplace and education', icon: Briefcase, group: 'Profile' },
  { id: 'social', title: 'Social & links', subtitle: 'Discord, GitHub, website and public email', icon: Globe, group: 'Profile' },
  { id: 'interests', title: 'Interests & skills', subtitle: 'Add and arrange profile interests', icon: Heart, group: 'Profile' },
  { id: 'professional', title: 'Professional profile', subtitle: 'Professional details and business account', icon: BadgeCheck, group: 'Professional' },
  { id: 'appearance', title: 'Profile appearance', subtitle: 'Preview and update your profile cover', icon: Image, group: 'Customize' },
  { id: 'featured', title: 'Featured content', subtitle: 'Pin posts and media to your profile', icon: Star, group: 'Customize' },
  { id: 'layout', title: 'Profile layout', subtitle: 'Choose how posts and highlights appear', icon: Image, group: 'Customize' },
];

export interface ProfileDraft {
  username: string;
  display_name: string;
  bio: string;
  avatar: string;
  profile_cover_image: string;
  gender: string;
  pronouns: string;
  birthday: string;
  relationship_status: string;
  public_email: string;
  region: string;
  current_city: string;
  hometown: string;
  occupation: string;
  workplace: string;
  education: string;
  discord_tag: string;
  github_profile: string;
  personal_website: string;
  website: string;
  interests: string[];
  featured_post_ids: string[];
  featured_media_urls: string[];
  profile_layout: ProfileLayout;
}

export interface HistoryItem { id: string; url: string; created_at: string | null; }
export interface FeaturedPost { id: string; post_text: string | null; post_image: string | null; media_items: unknown; timestamp: number | null; }
export interface FeaturedMedia { url: string; postId: string; caption: string | null; }
export interface BusinessAccount { account_type: string; monetization_enabled: boolean | null; verification_status: string | null; }

export const inputClass = 'mt-2 h-11 w-full rounded-2xl border border-border bg-background px-4 text-sm text-foreground outline-none placeholder:text-muted-foreground focus:ring-2 focus:ring-ring/30';
export const textareaClass = 'mt-2 min-h-28 w-full resize-y rounded-2xl border border-border bg-background px-4 py-3 text-sm leading-6 text-foreground outline-none placeholder:text-muted-foreground focus:ring-2 focus:ring-ring/30';
export const labelClass = 'block text-sm font-medium text-foreground';

export const toStringArray = (value: unknown): string[] => Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : [];
export const textValue = (value: string | null | undefined) => value || '';
export const toDraft = (profile: EditableProfile, user: UserProfile): ProfileDraft => ({
  username: profile.username || user.username || '',
  display_name: profile.display_name || profile.nickname || user.fullName || '',
  bio: textValue(profile.bio || user.bio),
  avatar: textValue(profile.avatar || user.avatarUrl),
  profile_cover_image: textValue(profile.profile_cover_image),
  gender: (profile.gender || 'hidden').toLowerCase(),
  pronouns: textValue(profile.pronouns),
  birthday: textValue(profile.birthday),
  relationship_status: textValue(profile.relationship_status),
  public_email: textValue(profile.public_email),
  region: textValue(profile.region),
  current_city: textValue(profile.current_city),
  hometown: textValue(profile.hometown),
  occupation: textValue(profile.occupation),
  workplace: textValue(profile.workplace),
  education: textValue(profile.education),
  discord_tag: textValue(profile.discord_tag),
  github_profile: textValue(profile.github_profile),
  personal_website: textValue(profile.personal_website || profile.website),
  website: textValue(profile.website || profile.personal_website),
  interests: toStringArray(profile.interests),
  featured_post_ids: toStringArray(profile.featured_post_ids),
  featured_media_urls: toStringArray(profile.featured_media_urls),
  profile_layout: profile.profile_layout === 'grid' || profile.profile_layout === 'highlights' ? profile.profile_layout : 'feed',
});

export const normalizeUrl = (value: string, label: string): string | null => {
  const clean = value.trim();
  if (!clean) return null;
  const candidate = /^https?:\/\//i.test(clean) ? clean : `https://${clean}`;
  let url: URL;
  try { url = new URL(candidate); } catch { throw new Error(`${label} must be a valid URL.`); }
  if (url.protocol !== 'https:' || url.username || url.password) throw new Error(`${label} must use a secure HTTPS address.`);
  return url.toString();
};

export const normalizeImageUrl = (value: string): string | null => {
  const clean = value.trim();
  if (!clean) return null;
  const candidate = clean.startsWith('/') ? clean : /^https?:\/\//i.test(clean) ? clean : `https://${clean}`;
  let url: URL;
  try { url = new URL(candidate, window.location.origin); } catch { throw new Error('Enter a valid HTTPS image URL.'); }
  if (url.protocol !== 'https:' && url.origin !== window.location.origin) throw new Error('Profile images must use HTTPS or a same-site URL.');
  if (url.username || url.password) throw new Error('Image URLs cannot include a username or password.');
  return url.origin === window.location.origin ? `${url.pathname}${url.search}${url.hash}` : url.toString();
};

export const githubUrl = (value: string): string | null => {
  const clean = value.trim();
  if (!clean) return null;
  const candidate = /^https?:\/\//i.test(clean) ? clean : `https://github.com/${clean.replace(/^github\.com\//i, '')}`;
  let url: URL;
  try { url = new URL(candidate); } catch { throw new Error('Enter a valid GitHub username or link.'); }
  if (url.protocol !== 'https:' || !/(^|\.)github\.com$/i.test(url.hostname)) throw new Error('Use a GitHub profile link.');
  return url.toString();
};

export const mediaUrls = (value: unknown): string[] => {
  if (!Array.isArray(value)) return [];
  return value.flatMap(item => {
    if (typeof item === 'string') return [item];
    if (!item || typeof item !== 'object') return [];
    const row = item as Record<string, unknown>;
    const url = row.url || row.media_url || row.uri || row.src;
    return typeof url === 'string' ? [url] : [];
  });
};

export const sectionKeys: Record<SectionId, (keyof ProfileDraft)[]> = {
  basic: ['username', 'display_name', 'bio'],
  media: ['avatar', 'profile_cover_image'],
  personal: ['gender', 'pronouns', 'birthday', 'relationship_status'],
  location: ['region', 'current_city', 'hometown'],
  work: ['occupation', 'workplace', 'education'],
  social: ['discord_tag', 'github_profile', 'personal_website', 'website', 'public_email'],
  interests: ['interests'],
  professional: ['occupation', 'workplace'],
  appearance: ['profile_cover_image'],
  featured: ['featured_post_ids', 'featured_media_urls'],
  layout: ['profile_layout'],
};

export const moveItem = (items: string[], index: number, direction: -1 | 1) => {
  const next = [...items];
  const destination = index + direction;
  if (destination < 0 || destination >= next.length) return next;
  [next[index], next[destination]] = [next[destination], next[index]];
  return next;
};

