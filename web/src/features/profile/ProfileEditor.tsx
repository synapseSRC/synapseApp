import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowDown, ArrowLeft, ArrowUp, BadgeCheck, Briefcase, CalendarDays, Camera, Check,
  ChevronRight, Globe, Heart, Image, Loader2, MapPin,
  Plus, Save, Star, Trash2, Upload, UserRound, X,
  type LucideIcon,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { supabase } from '@/lib/supabase';
import { useAuthStore } from '@/features/auth/useAuthStore';
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

interface ProfileEditorProps {
  user: UserProfile;
  profile: EditableProfile;
  onCancel: () => void;
  onSaved: (profile: EditableProfile, warning?: string) => void;
}

type SectionId = 'basic' | 'media' | 'personal' | 'location' | 'work' | 'social' | 'interests' | 'professional' | 'appearance' | 'featured' | 'layout';
type SectionMeta = { id: SectionId; title: string; subtitle: string; icon: LucideIcon; group: 'Profile' | 'Professional' | 'Customize' };

const SECTIONS: SectionMeta[] = [
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

interface ProfileDraft {
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

interface HistoryItem { id: string; url: string; created_at: string | null; }
interface FeaturedPost { id: string; post_text: string | null; post_image: string | null; media_items: unknown; timestamp: number | null; }
interface FeaturedMedia { url: string; postId: string; caption: string | null; }
interface BusinessAccount { account_type: string; monetization_enabled: boolean | null; verification_status: string | null; }

const inputClass = 'mt-2 h-11 w-full rounded-2xl border border-border bg-background px-4 text-sm text-foreground outline-none placeholder:text-muted-foreground focus:ring-2 focus:ring-ring/30';
const textareaClass = 'mt-2 min-h-28 w-full resize-y rounded-2xl border border-border bg-background px-4 py-3 text-sm leading-6 text-foreground outline-none placeholder:text-muted-foreground focus:ring-2 focus:ring-ring/30';
const labelClass = 'block text-sm font-medium text-foreground';

const toStringArray = (value: unknown): string[] => Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : [];
const textValue = (value: string | null | undefined) => value || '';
const toDraft = (profile: EditableProfile, user: UserProfile): ProfileDraft => ({
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

const normalizeUrl = (value: string, label: string): string | null => {
  const clean = value.trim();
  if (!clean) return null;
  const candidate = /^https?:\/\//i.test(clean) ? clean : `https://${clean}`;
  let url: URL;
  try { url = new URL(candidate); } catch { throw new Error(`${label} must be a valid URL.`); }
  if (url.protocol !== 'https:' || url.username || url.password) throw new Error(`${label} must use a secure HTTPS address.`);
  return url.toString();
};

const normalizeImageUrl = (value: string): string | null => {
  const clean = value.trim();
  if (!clean) return null;
  const candidate = clean.startsWith('/') ? clean : /^https?:\/\//i.test(clean) ? clean : `https://${clean}`;
  let url: URL;
  try { url = new URL(candidate, window.location.origin); } catch { throw new Error('Enter a valid HTTPS image URL.'); }
  if (url.protocol !== 'https:' && url.origin !== window.location.origin) throw new Error('Profile images must use HTTPS or a same-site URL.');
  if (url.username || url.password) throw new Error('Image URLs cannot include a username or password.');
  return url.origin === window.location.origin ? `${url.pathname}${url.search}${url.hash}` : url.toString();
};

const githubUrl = (value: string): string | null => {
  const clean = value.trim();
  if (!clean) return null;
  const candidate = /^https?:\/\//i.test(clean) ? clean : `https://github.com/${clean.replace(/^github\.com\//i, '')}`;
  let url: URL;
  try { url = new URL(candidate); } catch { throw new Error('Enter a valid GitHub username or link.'); }
  if (url.protocol !== 'https:' || !/(^|\.)github\.com$/i.test(url.hostname)) throw new Error('Use a GitHub profile link.');
  return url.toString();
};

const mediaUrls = (value: unknown): string[] => {
  if (!Array.isArray(value)) return [];
  return value.flatMap(item => {
    if (typeof item === 'string') return [item];
    if (!item || typeof item !== 'object') return [];
    const row = item as Record<string, unknown>;
    const url = row.url || row.media_url || row.uri || row.src;
    return typeof url === 'string' ? [url] : [];
  });
};

const sectionKeys: Record<SectionId, (keyof ProfileDraft)[]> = {
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

const moveItem = (items: string[], index: number, direction: -1 | 1) => {
  const next = [...items];
  const destination = index + direction;
  if (destination < 0 || destination >= next.length) return next;
  [next[index], next[destination]] = [next[destination], next[index]];
  return next;
};

const Field: React.FC<{ id: string; label: string; value: string; onChange: (value: string) => void; placeholder?: string; maxLength?: number; type?: string; hint?: string; inputMode?: 'text' | 'email' | 'url' | 'numeric' }> = ({ id, label, value, onChange, placeholder, maxLength, type = 'text', hint, inputMode }) => (
  <label htmlFor={id} className={labelClass}>{label}<input id={id} type={type} inputMode={inputMode} value={value} onChange={event => onChange(event.target.value)} placeholder={placeholder} maxLength={maxLength} className={inputClass} />{hint && <span className="mt-1.5 block text-xs leading-5 text-muted-foreground">{hint}</span>}</label>
);

const TextArea: React.FC<{ id: string; label: string; value: string; onChange: (value: string) => void; placeholder?: string; maxLength?: number; rows?: number }> = ({ id, label, value, onChange, placeholder, maxLength, rows = 4 }) => (
  <label htmlFor={id} className={labelClass}>{label}<textarea id={id} value={value} onChange={event => onChange(event.target.value)} placeholder={placeholder} maxLength={maxLength} rows={rows} className={textareaClass} />{maxLength && <span className="mt-1 block text-right text-xs font-normal text-muted-foreground">{value.length}/{maxLength}</span>}</label>
);

const SelectField: React.FC<{ id: string; label: string; value: string; onChange: (value: string) => void; options: Array<{ value: string; label: string }> }> = ({ id, label, value, onChange, options }) => (
  <label htmlFor={id} className={labelClass}>{label}<select id={id} value={value} onChange={event => onChange(event.target.value)} className={inputClass}>{options.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
);

const SaveButton: React.FC<{ isSaving: boolean; onClick: () => void; dirty: boolean }> = ({ isSaving, onClick, dirty }) => (
  <Button type="button" disabled={isSaving || !dirty} onClick={onClick} className="rounded-full bg-foreground px-5 text-sm font-semibold text-background shadow-none hover:opacity-85 disabled:opacity-50">
    {isSaving ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Saving…</> : <><Save className="mr-2 h-4 w-4" />Save changes</>}
  </Button>
);

export const ProfileEditor: React.FC<ProfileEditorProps> = ({ user, profile, onCancel, onSaved }) => {
  const [section, setSection] = useState<SectionId | null>(null);
  const [draft, setDraft] = useState<ProfileDraft>(() => toDraft(profile, user));
  const [savedDraft, setSavedDraft] = useState<ProfileDraft>(() => toDraft(profile, user));
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [coverFile, setCoverFile] = useState<File | null>(null);
  const [avatarFilePreview, setAvatarFilePreview] = useState<string | null>(null);
  const [coverFilePreview, setCoverFilePreview] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [showDiscard, setShowDiscard] = useState(false);
  const [pendingInterest, setPendingInterest] = useState('');
  const [avatarHistory, setAvatarHistory] = useState<HistoryItem[]>([]);
  const [coverHistory, setCoverHistory] = useState<HistoryItem[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [historyReload, setHistoryReload] = useState(0);
  const [candidatePosts, setCandidatePosts] = useState<FeaturedPost[]>([]);
  const [featuredLoading, setFeaturedLoading] = useState(false);
  const [featuredError, setFeaturedError] = useState<string | null>(null);
  const [featuredTab, setFeaturedTab] = useState<'posts' | 'media' | 'collections'>('posts');
  const [businessAccount, setBusinessAccount] = useState<BusinessAccount | null>(null);
  const [businessLoading, setBusinessLoading] = useState(false);
  const [businessError, setBusinessError] = useState<string | null>(null);
  const [confirmBusiness, setConfirmBusiness] = useState(false);
  const [switchingBusiness, setSwitchingBusiness] = useState(false);

  useEffect(() => {
    if (!avatarFile) { setAvatarFilePreview(null); return; }
    const objectUrl = URL.createObjectURL(avatarFile);
    setAvatarFilePreview(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [avatarFile]);

  useEffect(() => {
    if (!coverFile) { setCoverFilePreview(null); return; }
    const objectUrl = URL.createObjectURL(coverFile);
    setCoverFilePreview(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [coverFile]);

  useEffect(() => {
    if (section !== 'media' && section !== 'appearance') return;
    let cancelled = false;
    const loadHistory = async () => {
      setHistoryLoading(true);
      setHistoryError(null);
      try {
        const [avatarResult, coverResult] = await Promise.all([
          supabase.from('profile_history').select('id,avatar,created_at').eq('user_id', user.id).order('created_at', { ascending: false }).limit(12),
          supabase.from('cover_image_history').select('id,cover_image_url,created_at').eq('user_id', user.id).order('created_at', { ascending: false }).limit(12),
        ]);
        if (avatarResult.error) throw avatarResult.error;
        if (coverResult.error) throw coverResult.error;
        if (!cancelled) {
          setAvatarHistory(((avatarResult.data || []) as Array<{ id: string; avatar: string | null; created_at: string | null }>).filter(row => row.avatar).map(row => ({ id: row.id, url: row.avatar as string, created_at: row.created_at })));
          setCoverHistory(((coverResult.data || []) as Array<{ id: string; cover_image_url: string | null; created_at: string | null }>).filter(row => row.cover_image_url).map(row => ({ id: row.id, url: row.cover_image_url as string, created_at: row.created_at })));
        }
      } catch (error) {
        if (!cancelled) setHistoryError(error instanceof Error ? error.message : 'Could not load profile photo history.');
      } finally {
        if (!cancelled) setHistoryLoading(false);
      }
    };
    void loadHistory();
    return () => { cancelled = true; };
  }, [section, user.id, historyReload]);

  useEffect(() => {
    if (section !== 'featured') return;
    let cancelled = false;
    const loadPosts = async () => {
      setFeaturedLoading(true);
      setFeaturedError(null);
      try {
        const { data, error } = await supabase.from('posts').select('id,post_text,post_image,media_items,timestamp').eq('author_uid', user.id).or('is_deleted.is.null,is_deleted.eq.false').order('timestamp', { ascending: false }).limit(60);
        if (error) throw error;
        if (!cancelled) setCandidatePosts((data || []) as FeaturedPost[]);
      } catch (error) {
        if (!cancelled) setFeaturedError(error instanceof Error ? error.message : 'Could not load your posts.');
      } finally {
        if (!cancelled) setFeaturedLoading(false);
      }
    };
    void loadPosts();
    return () => { cancelled = true; };
  }, [section, user.id]);

  useEffect(() => {
    if (section !== 'professional') return;
    let cancelled = false;
    const loadBusiness = async () => {
      setBusinessLoading(true);
      setBusinessError(null);
      try {
        const { data, error } = await supabase.from('business_accounts').select('account_type,monetization_enabled,verification_status').eq('user_id', user.id).maybeSingle();
        if (error) throw error;
        if (!cancelled) setBusinessAccount(data as BusinessAccount | null);
      } catch (error) {
        if (!cancelled) setBusinessError(error instanceof Error ? error.message : 'Could not load professional account details.');
      } finally {
        if (!cancelled) setBusinessLoading(false);
      }
    };
    void loadBusiness();
    return () => { cancelled = true; };
  }, [section, user.id]);

  const isDirty = JSON.stringify(draft) !== JSON.stringify(savedDraft) || Boolean(avatarFile) || Boolean(coverFile);
  const activeMeta = section ? SECTIONS.find(item => item.id === section) || null : null;
  const avatarPreview = avatarFilePreview || draft.avatar;
  const coverPreview = coverFilePreview || draft.profile_cover_image;
  const updateDraft = <K extends keyof ProfileDraft>(key: K, value: ProfileDraft[K]) => {
    setDraft(current => ({ ...current, [key]: value }));
    setNotice(null);
    setError(null);
  };

  const fileError = (file: File) => {
    const allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    if (!allowed.includes(file.type)) return 'Choose a JPG, PNG, GIF or WebP image.';
    if (file.size > 5 * 1024 * 1024) return 'Images must be 5 MB or smaller.';
    return null;
  };

  const uploadImage = async (file: File, kind: 'avatar' | 'cover') => {
    const invalid = fileError(file);
    if (invalid) throw new Error(invalid);
    const extension = file.type === 'image/jpeg' ? 'jpg' : file.type.split('/')[1] || 'img';
    const path = `${user.id}/profile/${kind}-${Date.now()}-${crypto.randomUUID()}.${extension}`;
    const { error } = await supabase.storage.from('avatars').upload(path, file, { cacheControl: '31536000', contentType: file.type, upsert: false });
    if (error) throw error;
    return supabase.storage.from('avatars').getPublicUrl(path).data.publicUrl;
  };

  const setSavedProfileState = (profileData: EditableProfile, savedSection: SectionId) => {
    const next = toDraft(profileData, user);
    const mergedSaved = { ...savedDraft };
    for (const key of sectionKeys[savedSection]) mergedSaved[key] = next[key] as never;
    setSavedDraft(mergedSaved);
    setDraft(current => {
      const updated = { ...current };
      for (const key of sectionKeys[savedSection]) updated[key] = next[key] as never;
      return updated;
    });
  };

  const syncUserIdentity = async (profileData: EditableProfile, savedSection: SectionId) => {
    if (savedSection !== 'basic' && savedSection !== 'media' && savedSection !== 'appearance') return [] as string[];
    const warnings: string[] = [];
    if (savedSection === 'basic' && profileData.username) {
      const { error: usernameError } = await supabase.from('usernames').upsert({ uid: user.id, username: profileData.username }, { onConflict: 'uid' });
      if (usernameError) warnings.push('Your profile was saved, but the username directory did not sync.');
    }
    const { data: authData, error: authError } = await supabase.auth.updateUser({
      data: {
        username: profileData.username,
        display_name: profileData.display_name,
        full_name: profileData.display_name,
        bio: profileData.bio,
        avatar_url: profileData.avatar,
      },
    });
    if (authError) warnings.push('Public profile saved, but sign-in metadata could not be refreshed.');
    const updatedUser: UserProfile = {
      ...user,
      username: profileData.username || user.username,
      fullName: profileData.display_name || user.fullName,
      avatarUrl: profileData.avatar || undefined,
      bio: profileData.bio || undefined,
      website: profileData.website || profileData.personal_website || undefined,
      createdAt: authData?.user?.created_at || user.createdAt,
    };
    useAuthStore.getState().setUser(updatedUser);
    return warnings;
  };

  const saveSection = async (target: SectionId) => {
    if (saving) return;
    setSaving(true);
    setError(null);
    setNotice(null);
    const oldAvatar = savedDraft.avatar;
    const oldCover = savedDraft.profile_cover_image;
    try {
      let avatar = draft.avatar.trim();
      let cover = draft.profile_cover_image.trim();
      if (target === 'basic') {
        const username = draft.username.trim().toLowerCase();
        if (!draft.display_name.trim()) throw new Error('Display name is required.');
        if (draft.display_name.trim().length > 80) throw new Error('Display name can be up to 80 characters.');
        if (username !== (savedDraft.username || '').toLowerCase() && !/^[a-z][a-z0-9_.]{2,24}$/.test(username)) throw new Error('Android username rules: 3–25 characters, starts with a letter, and uses letters, numbers, dots or underscores.');
        if (draft.bio.length > 160) throw new Error('Bio can be up to 160 characters.');
      }
      if (target === 'social') {
        if (draft.discord_tag.trim().length > 50) throw new Error('Discord tag can be up to 50 characters.');
        if (draft.github_profile.trim().length > 100) throw new Error('GitHub link can be up to 100 characters.');
        if (draft.public_email.trim() && !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(draft.public_email.trim())) throw new Error('Enter a valid public email address.');
      }
      if (target === 'personal' && draft.birthday) {
        if (!/^\d{4}-\d{2}-\d{2}$/.test(draft.birthday) || Date.parse(draft.birthday) > Date.now()) throw new Error('Enter a valid birthday that is not in the future.');
      }
      if (target === 'media' || target === 'appearance') {
        if (target === 'media' && avatarFile) avatar = await uploadImage(avatarFile, 'avatar');
        if (coverFile) cover = await uploadImage(coverFile, 'cover');
        if (!avatarFile) avatar = normalizeImageUrl(draft.avatar) || '';
        if (!coverFile) cover = normalizeImageUrl(draft.profile_cover_image) || '';
      }

      let updates: Record<string, unknown> = {};
      if (target === 'basic') updates = { username: draft.username.trim().toLowerCase(), display_name: draft.display_name.trim(), nickname: draft.display_name.trim(), bio: draft.bio.trim() || null };
      if (target === 'media') updates = { avatar: avatar || null, profile_cover_image: cover || null };
      if (target === 'personal') updates = { gender: draft.gender || 'hidden', pronouns: draft.pronouns.trim() || null, birthday: draft.birthday || null, relationship_status: draft.relationship_status || null };
      if (target === 'location') updates = { region: draft.region.trim() || null, current_city: draft.current_city.trim() || null, hometown: draft.hometown.trim() || null };
      if (target === 'work' || target === 'professional') updates = { occupation: draft.occupation.trim() || null, workplace: draft.workplace.trim() || null, education: draft.education.trim() || null };
      if (target === 'social') {
        const personalWebsite = normalizeUrl(draft.personal_website, 'Personal website');
        const githubProfile = githubUrl(draft.github_profile);
        updates = { discord_tag: draft.discord_tag.trim() || null, github_profile: githubProfile, personal_website: personalWebsite, website: personalWebsite, public_email: draft.public_email.trim() || null };
      }
      if (target === 'interests') updates = { interests: draft.interests.map(item => item.trim()).filter(Boolean) };
      if (target === 'appearance') updates = { profile_cover_image: cover || null };
      if (target === 'featured') updates = { featured_post_ids: draft.featured_post_ids, featured_media_urls: draft.featured_media_urls };
      if (target === 'layout') updates = { profile_layout: draft.profile_layout };

      const { data, error: updateError } = await supabase.from('users').update({ ...updates, updated_at: new Date().toISOString() }).eq('uid', user.id).select(EDITABLE_PROFILE_SELECT).maybeSingle();
      if (updateError) {
        const message = updateError.message.toLowerCase();
        if (updateError.code === '23505' || message.includes('users_username_lower_key') || message.includes('duplicate key')) throw new Error('That username is already in use. Try another one.');
        throw updateError;
      }
      if (!data) throw new Error('Your profile could not be found for this account. Please refresh and try again.');
      const saved = data as EditableProfile;
      const warnings = await syncUserIdentity(saved, target);
      if ((target === 'media' || target === 'appearance') && saved.avatar && saved.avatar !== oldAvatar) {
        const { error: historyError } = await supabase.from('profile_history').insert({ user_id: user.id, avatar: saved.avatar });
        if (historyError) warnings.push('Profile saved, but avatar history could not be updated.');
      }
      if ((target === 'media' || target === 'appearance') && saved.profile_cover_image && saved.profile_cover_image !== oldCover) {
        const { error: historyError } = await supabase.from('cover_image_history').insert({ user_id: user.id, cover_image_url: saved.profile_cover_image });
        if (historyError) warnings.push('Profile saved, but cover-photo history could not be updated.');
      }
      setSavedProfileState(saved, target);
      if (target === 'media' || target === 'appearance') { setAvatarFile(null); setCoverFile(null); setHistoryReload(value => value + 1); }
      setNotice(warnings.length ? warnings.join(' ') : 'Changes saved.');
      onSaved(saved, warnings.length ? warnings.join(' ') : undefined);
      setSection(null);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Could not save these profile changes.');
    } finally {
      setSaving(false);
    }
  };

  const addInterest = () => {
    const value = pendingInterest.trim();
    if (!value) return;
    if (draft.interests.some(item => item.toLowerCase() === value.toLowerCase())) { setError('That interest is already on your profile.'); return; }
    if (value.length > 32) { setError('Interests can be up to 32 characters.'); return; }
    updateDraft('interests', [...draft.interests, value]);
    setPendingInterest('');
  };

  const changeBusinessAccount = async () => {
    if (switchingBusiness || businessAccount?.account_type === 'BUSINESS') return;
    setSwitchingBusiness(true);
    setBusinessError(null);
    try {
      const { data, error } = await supabase.from('business_accounts').upsert({ user_id: user.id, account_type: 'BUSINESS', monetization_enabled: false }, { onConflict: 'user_id' }).select('account_type,monetization_enabled,verification_status').single();
      if (error) throw error;
      const { error: profileTypeError } = await supabase.from('users').update({ account_type: 'business' }).eq('uid', user.id);
      if (profileTypeError) setBusinessError('Business account was created, but the profile type did not sync.');
      setBusinessAccount(data as BusinessAccount);
      setConfirmBusiness(false);
    } catch (error) {
      setBusinessError(error instanceof Error ? error.message : 'Could not create the business account.');
    } finally {
      setSwitchingBusiness(false);
    }
  };

  const candidateMedia = useMemo<FeaturedMedia[]>(() => {
    const list: FeaturedMedia[] = [];
    for (const post of candidatePosts) {
      const urls = mediaUrls(post.media_items);
      if (post.post_image && !urls.includes(post.post_image)) urls.unshift(post.post_image);
      urls.forEach(url => list.push({ url, postId: post.id, caption: post.post_text }));
    }
    return list.filter((item, index, all) => all.findIndex(other => other.url === item.url) === index);
  }, [candidatePosts]);

  const isSectionDirty = (target: SectionId) => sectionKeys[target].some(key => JSON.stringify(draft[key]) !== JSON.stringify(savedDraft[key])) || (target === 'media' && Boolean(avatarFile || coverFile)) || (target === 'appearance' && Boolean(coverFile));
  const completenessFields = [draft.display_name, draft.username, draft.bio, draft.avatar, draft.profile_cover_image, draft.region, draft.occupation, draft.interests.length ? 'set' : ''];
  const completedCount = completenessFields.filter(value => String(value || '').trim()).length;

  const requestClose = () => {
    if (isDirty) setShowDiscard(true);
    else onCancel();
  };

  const selectFile = (kind: 'avatar' | 'cover', file?: File) => {
    if (!file) return;
    const error = fileError(file);
    if (error) { setError(error); return; }
    setError(null);
    if (kind === 'avatar') setAvatarFile(file); else setCoverFile(file);
  };

  const saveButton = section ? <SaveButton isSaving={saving} onClick={() => void saveSection(section)} dirty={isSectionDirty(section)} /> : null;
  const grouped = ['Profile', 'Professional', 'Customize'] as const;

  const renderHistory = (kind: 'avatar' | 'cover') => {
    const rows = kind === 'avatar' ? avatarHistory : coverHistory;
    const selectedUrl = kind === 'avatar' ? draft.avatar : draft.profile_cover_image;
    return <div className="mt-6 space-y-3 border-t border-border pt-5">
      <div><h4 className="text-sm font-semibold text-foreground">{kind === 'avatar' ? 'Photo history' : 'Cover history'}</h4><p className="mt-1 text-xs text-muted-foreground">Choose a previous image and save to restore it.</p></div>
      {historyLoading ? <div className="h-24 animate-pulse rounded-2xl bg-muted" /> : historyError ? <p role="alert" className="text-sm text-muted-foreground">{historyError}</p> : rows.length ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">{rows.map(row => <button type="button" key={row.id} onClick={() => { if (kind === 'avatar') { setAvatarFile(null); updateDraft('avatar', row.url); } else { setCoverFile(null); updateDraft('profile_cover_image', row.url); } }} className={`overflow-hidden rounded-2xl border text-left transition ${selectedUrl === row.url ? 'border-foreground ring-2 ring-foreground/20' : 'border-border hover:border-zinc-400'}`}><img src={row.url} alt={kind === 'avatar' ? 'Previous profile photo' : 'Previous profile cover'} className={kind === 'avatar' ? 'aspect-square w-full object-cover' : 'aspect-[16/8] w-full object-cover'} loading="lazy" /><span className="block px-3 py-2 text-[11px] text-muted-foreground">{row.created_at ? new Date(row.created_at).toLocaleDateString() : 'Saved image'}{selectedUrl === row.url ? ' · Selected' : ''}</span></button>)}</div> : <div className="rounded-2xl bg-muted/50 px-4 py-5 text-sm text-muted-foreground">No previous images found.</div>}
    </div>;
  };

  const renderProfileSection = () => {
    if (!section) return null;
    if (section === 'basic') return <div className="space-y-5"><div className="rounded-2xl border border-border bg-card p-4"><p className="text-sm font-semibold text-card-foreground">Identity preview</p><p className="mt-1 text-sm text-muted-foreground">Changes here update the name, handle and short bio shown on your profile.</p></div><Field id="edit-profile-display-name" label="Display name" value={draft.display_name} onChange={value => updateDraft('display_name', value)} maxLength={80} placeholder="Your display name" /><Field id="edit-profile-username" label="Username" value={draft.username} onChange={value => updateDraft('username', value.toLowerCase())} maxLength={25} placeholder="username" hint="Android rules: 3–25 characters, starts with a letter, uses letters, numbers, dots or underscores." /><TextArea id="edit-profile-bio" label="Bio" value={draft.bio} onChange={value => updateDraft('bio', value)} maxLength={160} placeholder="A little about you…" /></div>;

    if (section === 'media') return <div className="space-y-6">
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-4"><div className="mb-4 flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-muted"><Camera className="h-4 w-4" /></span><div><p className="text-sm font-semibold text-card-foreground">Profile photo</p><p className="text-xs text-muted-foreground">Upload or restore a previous photo.</p></div></div><div className="mb-4 grid h-32 place-items-center overflow-hidden rounded-2xl bg-muted">{avatarPreview ? <img src={avatarPreview} alt="Profile photo preview" className="h-full w-full object-cover" /> : <UserRound className="h-8 w-8 text-muted-foreground" />}</div><label className="block text-sm font-medium text-foreground">Photo URL<input value={draft.avatar} onChange={event => updateDraft('avatar', event.target.value)} placeholder="https://…" className={inputClass} /></label><div className="mt-3 flex flex-wrap gap-2"><label className="inline-flex cursor-pointer items-center rounded-full border border-border px-4 py-2 text-xs font-semibold text-foreground hover:bg-muted"><Upload className="mr-2 h-3.5 w-3.5" />Upload photo<input type="file" accept="image/jpeg,image/png,image/gif,image/webp" className="sr-only" onChange={event => selectFile('avatar', event.currentTarget.files?.[0])} /></label><Button type="button" variant="ghost" onClick={() => { setAvatarFile(null); updateDraft('avatar', ''); }} className="rounded-full text-xs text-muted-foreground"><Trash2 className="mr-2 h-3.5 w-3.5" />Remove</Button></div></div>
        <div className="rounded-2xl border border-border bg-card p-4"><div className="mb-4 flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-muted"><Image className="h-4 w-4" /></span><div><p className="text-sm font-semibold text-card-foreground">Cover photo</p><p className="text-xs text-muted-foreground">Upload a wide profile background.</p></div></div><div className="mb-4 grid aspect-[16/7] place-items-center overflow-hidden rounded-2xl bg-muted">{coverPreview ? <img src={coverPreview} alt="Cover photo preview" className="h-full w-full object-cover" /> : <Image className="h-8 w-8 text-muted-foreground" />}</div><label className="block text-sm font-medium text-foreground">Cover URL<input value={draft.profile_cover_image} onChange={event => updateDraft('profile_cover_image', event.target.value)} placeholder="https://…" className={inputClass} /></label><div className="mt-3 flex flex-wrap gap-2"><label className="inline-flex cursor-pointer items-center rounded-full border border-border px-4 py-2 text-xs font-semibold text-foreground hover:bg-muted"><Upload className="mr-2 h-3.5 w-3.5" />Upload cover<input type="file" accept="image/jpeg,image/png,image/gif,image/webp" className="sr-only" onChange={event => selectFile('cover', event.currentTarget.files?.[0])} /></label><Button type="button" variant="ghost" onClick={() => { setCoverFile(null); updateDraft('profile_cover_image', ''); }} className="rounded-full text-xs text-muted-foreground"><Trash2 className="mr-2 h-3.5 w-3.5" />Remove</Button></div></div>
      </div>
      <div className="rounded-2xl border border-border bg-muted/40 px-4 py-3 text-xs leading-5 text-muted-foreground">Uploads use the public Synapse avatar-image bucket. Images must be JPG, PNG, GIF or WebP and no larger than 5 MB. Old images remain available in history after you change or remove them.</div>
      {historyError && <p role="alert" className="text-sm text-zinc-600">{historyError}</p>}
      {renderHistory('avatar')}
      {renderHistory('cover')}
    </div>;

    if (section === 'personal') return <div className="space-y-5">
      <SelectField id="profile-gender" label="Gender" value={draft.gender} onChange={value => updateDraft('gender', value)} options={[{ value: 'hidden', label: 'Prefer not to say' }, { value: 'male', label: 'Male' }, { value: 'female', label: 'Female' }]} />
      <Field id="profile-pronouns" label="Pronouns" value={draft.pronouns} onChange={value => updateDraft('pronouns', value)} maxLength={40} placeholder="e.g. she/her" hint="Shown on your profile when provided." />
      <Field id="profile-birthday" label="Birthday" value={draft.birthday} onChange={value => updateDraft('birthday', value)} type="date" />
      <SelectField id="profile-relationship" label="Relationship status" value={draft.relationship_status} onChange={value => updateDraft('relationship_status', value)} options={[{ value: '', label: 'Not specified' }, { value: 'Single', label: 'Single' }, { value: 'In a relationship', label: 'In a relationship' }, { value: 'Married', label: 'Married' }, { value: "It's complicated", label: "It's complicated" }, { value: 'Prefer not to say', label: 'Prefer not to say' }, ...(draft.relationship_status && !['Single', 'In a relationship', 'Married', "It's complicated", 'Prefer not to say'].includes(draft.relationship_status) ? [{ value: draft.relationship_status, label: draft.relationship_status }] : [])]} />
      <p className="rounded-2xl border border-border bg-muted/40 px-4 py-3 text-xs leading-5 text-muted-foreground">Personal information may be visible according to your account privacy settings. Review Privacy & Security before adding sensitive details.</p>
    </div>;

    if (section === 'location') return <div className="space-y-5"><Field id="profile-region" label="Region" value={draft.region} onChange={value => updateDraft('region', value)} maxLength={100} placeholder="Country or region" hint="Android uses a region picker; enter your region here." /><Field id="profile-current-city" label="Current city" value={draft.current_city} onChange={value => updateDraft('current_city', value)} maxLength={100} placeholder="e.g. New York" /><Field id="profile-hometown" label="Hometown" value={draft.hometown} onChange={value => updateDraft('hometown', value)} maxLength={100} placeholder="Where you grew up" /></div>;

    if (section === 'work') return <div className="space-y-5"><Field id="profile-occupation" label="Occupation" value={draft.occupation} onChange={value => updateDraft('occupation', value)} maxLength={100} placeholder="e.g. Designer" /><Field id="profile-workplace" label="Workplace" value={draft.workplace} onChange={value => updateDraft('workplace', value)} maxLength={120} placeholder="Company or organisation" /><TextArea id="profile-education" label="Education" value={draft.education} onChange={value => updateDraft('education', value)} maxLength={240} rows={3} placeholder="Schools or qualifications, separated by commas" /></div>;

    if (section === 'social') return <div className="space-y-5"><Field id="profile-discord" label="Discord tag" value={draft.discord_tag} onChange={value => updateDraft('discord_tag', value)} maxLength={50} placeholder="Your Discord username" /><Field id="profile-github" label="GitHub" value={draft.github_profile} onChange={value => updateDraft('github_profile', value)} maxLength={100} placeholder="username or github.com/username" /><Field id="profile-personal-website" label="Personal website" value={draft.personal_website} onChange={value => { updateDraft('personal_website', value); updateDraft('website', value); }} maxLength={240} inputMode="url" placeholder="https://example.com" /><Field id="profile-public-email" label="Public email" value={draft.public_email} onChange={value => updateDraft('public_email', value)} maxLength={254} type="email" inputMode="email" placeholder="you@example.com" hint="This is separate from your sign-in email." /></div>;

    if (section === 'interests') return <div className="space-y-5"><div><h3 className="text-sm font-semibold text-foreground">Your interests</h3><p className="mt-1 text-sm text-muted-foreground">Add topics you care about. Reorder them to choose how they appear.</p></div><div className="flex gap-2"><input value={pendingInterest} onChange={event => setPendingInterest(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') { event.preventDefault(); addInterest(); } }} maxLength={32} placeholder="Add an interest" className="h-11 min-w-0 flex-1 rounded-2xl border border-border bg-background px-4 text-sm text-foreground outline-none focus:ring-2 focus:ring-ring/30" /><Button type="button" onClick={addInterest} className="h-11 rounded-2xl bg-foreground px-4 text-background"><Plus className="mr-2 h-4 w-4" />Add</Button></div>{error && <p role="alert" className="text-sm text-destructive">{error}</p>}{draft.interests.length ? <div className="space-y-2">{draft.interests.map((interest,index) => <div key={`${interest}-${index}`} className="flex items-center gap-3 rounded-2xl border border-border bg-card px-4 py-3"><span className="min-w-0 flex-1 truncate text-sm font-medium text-card-foreground">{interest}</span><Button type="button" variant="ghost" onClick={() => updateDraft('interests', moveItem(draft.interests,index,-1))} disabled={index===0} aria-label={`Move ${interest} up`} className="h-8 w-8 rounded-full p-0"><ArrowUp className="h-4 w-4" /></Button><Button type="button" variant="ghost" onClick={() => updateDraft('interests', moveItem(draft.interests,index,1))} disabled={index===draft.interests.length-1} aria-label={`Move ${interest} down`} className="h-8 w-8 rounded-full p-0"><ArrowDown className="h-4 w-4" /></Button><Button type="button" variant="ghost" onClick={() => updateDraft('interests', draft.interests.filter((_,itemIndex)=>itemIndex!==index))} aria-label={`Remove ${interest}`} className="h-8 w-8 rounded-full p-0 text-muted-foreground"><X className="h-4 w-4" /></Button></div>)}</div> : <div className="rounded-2xl border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">No interests added yet.</div>}<p className="text-xs text-muted-foreground">The Android page titles this section “Interests & Skills”; its current profile model persists interests only.</p></div>;

    if (section === 'professional') return <div className="space-y-5"><div className="rounded-2xl border border-border bg-muted/40 p-4"><p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">Account type</p><p className="mt-2 text-lg font-semibold text-foreground">{businessAccount?.account_type === 'BUSINESS' ? 'Business account' : 'Personal account'}</p><p className="mt-1 text-sm leading-5 text-muted-foreground">{businessAccount?.account_type === 'BUSINESS' ? 'Your business account is connected to this profile.' : 'Switch to a business account to unlock the Android business-platform flow.'}</p></div>{businessError && <p role="alert" className="text-sm text-destructive">{businessError}</p>}{businessLoading ? <div className="h-16 animate-pulse rounded-2xl bg-muted" /> : businessAccount?.account_type === 'BUSINESS' ? <div className="rounded-2xl border border-border bg-card p-4 text-sm text-muted-foreground">Business profile settings are linked to this account. More business-platform controls are currently in Android Settings.</div> : confirmBusiness ? <div className="rounded-2xl border border-border bg-card p-4"><p className="font-semibold text-card-foreground">Switch to a business account?</p><p className="mt-1 text-sm leading-5 text-muted-foreground">This creates a business-account record for your Synapse profile. Continue only if you want to enable business features.</p><div className="mt-4 flex gap-2"><Button type="button" variant="outline" onClick={() => setConfirmBusiness(false)} disabled={switchingBusiness} className="rounded-full">Cancel</Button><Button type="button" onClick={() => void changeBusinessAccount()} disabled={switchingBusiness} className="rounded-full bg-foreground text-background">{switchingBusiness ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Check className="mr-2 h-4 w-4" />}{switchingBusiness ? 'Switching…' : 'Confirm switch'}</Button></div></div> : <Button type="button" onClick={() => setConfirmBusiness(true)} className="rounded-full bg-foreground px-5 text-background"><Briefcase className="mr-2 h-4 w-4" />Switch to business account</Button>}</div>;

    if (section === 'appearance') return <div className="space-y-5"><div className="overflow-hidden rounded-3xl border border-border bg-card"><div className="grid aspect-[16/5] place-items-center bg-gradient-to-r from-zinc-100 to-zinc-300 dark:from-zinc-800 dark:to-zinc-700">{coverPreview ? <img src={coverPreview} alt="Cover preview" className="h-full w-full object-cover" /> : <span className="text-sm text-muted-foreground">Profile cover preview</span>}</div><div className="flex items-center gap-3 p-4"><img src={avatarPreview || undefined} alt="" className="h-14 w-14 rounded-full border-2 border-card bg-muted object-cover" /><div><p className="font-semibold text-card-foreground">{draft.display_name || user.fullName}</p><p className="text-sm text-muted-foreground">@{draft.username || user.username}</p>{draft.bio && <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">{draft.bio}</p>}</div></div></div><div className="rounded-2xl border border-border bg-card p-4"><h3 className="font-semibold text-card-foreground">Cover appearance</h3><p className="mt-1 text-sm text-muted-foreground">Choose, replace or remove the background shown on your profile.</p><Field id="appearance-cover-url" label="Cover image URL" value={draft.profile_cover_image} onChange={value => updateDraft('profile_cover_image', value)} placeholder="https://…" /><div className="mt-3 flex flex-wrap gap-2"><label className="inline-flex cursor-pointer items-center rounded-full border border-border px-4 py-2 text-xs font-semibold text-foreground hover:bg-muted"><Upload className="mr-2 h-3.5 w-3.5" />Choose cover<input type="file" accept="image/jpeg,image/png,image/gif,image/webp" className="sr-only" onChange={event => selectFile('cover',event.currentTarget.files?.[0])} /></label><Button type="button" variant="outline" onClick={() => { setCoverFile(null); updateDraft('profile_cover_image', textValue(profile.profile_cover_image)); }} className="rounded-full">Reset</Button><Button type="button" variant="ghost" onClick={() => { setCoverFile(null); updateDraft('profile_cover_image',''); }} className="rounded-full text-muted-foreground"><Trash2 className="mr-2 h-3.5 w-3.5" />Remove cover</Button></div></div>{renderHistory('cover')}</div>;

    if (section === 'featured') {
      const media = candidateMedia;
      return <div className="space-y-5"><div className="rounded-2xl border border-border bg-muted/40 p-4"><p className="font-semibold text-foreground">Profile highlights</p><p className="mt-1 text-sm leading-5 text-muted-foreground">Select posts or media and order them for your featured profile area.</p><p className="mt-2 text-xs text-muted-foreground">{draft.featured_post_ids.length} posts · {draft.featured_media_urls.length} media items selected</p></div>{featuredError && <p role="alert" className="text-sm text-destructive">{featuredError}</p>}<div className="flex gap-2 border-b border-border pb-2">{(['posts','media','collections'] as const).map(tab => <button type="button" key={tab} onClick={() => setFeaturedTab(tab)} className={`rounded-full px-4 py-2 text-sm font-semibold capitalize ${featuredTab===tab?'bg-foreground text-background':'text-muted-foreground hover:bg-muted'}`}>{tab}</button>)}</div>{featuredTab==='collections' ? <div className="rounded-2xl border border-border bg-card p-5"><p className="font-semibold text-card-foreground">Collections</p><p className="mt-1 text-sm leading-5 text-muted-foreground">The Android profile editor currently shows a capability notice for collections. Collection creation and persistence aren’t available in this web editor yet.</p></div> : featuredLoading ? <div className="h-40 animate-pulse rounded-2xl bg-muted" /> : featuredTab==='posts' ? candidatePosts.length ? <div className="space-y-2">{candidatePosts.map(post=>{const selected=draft.featured_post_ids.includes(post.id);const order=draft.featured_post_ids.indexOf(post.id);return <div key={post.id} className={`flex items-center gap-3 rounded-2xl border p-3 ${selected?'border-foreground/40 bg-muted/40':'border-border bg-card'}`}><button type="button" onClick={()=>updateDraft('featured_post_ids',selected?draft.featured_post_ids.filter(id=>id!==post.id):[...draft.featured_post_ids,post.id])} aria-pressed={selected} className={`grid h-6 w-6 shrink-0 place-items-center rounded-md border ${selected?'border-foreground bg-foreground text-background':'border-border bg-background'}`}>{selected&&<Check className="h-4 w-4" />}</button><div className="min-w-0 flex-1"><p className="truncate text-sm font-medium text-card-foreground">{post.post_text||'Post'}</p><p className="text-xs text-muted-foreground">{post.timestamp ? new Date(post.timestamp).toLocaleDateString() : 'Post'}</p></div>{order>=0&&<><span className="text-xs text-muted-foreground">#{order+1}</span><Button type="button" variant="ghost" onClick={()=>updateDraft('featured_post_ids',moveItem(draft.featured_post_ids,order,-1))} disabled={order===0} aria-label="Move featured post up" className="h-8 w-8 rounded-full p-0"><ArrowUp className="h-4 w-4" /></Button><Button type="button" variant="ghost" onClick={()=>updateDraft('featured_post_ids',moveItem(draft.featured_post_ids,order,1))} disabled={order===draft.featured_post_ids.length-1} aria-label="Move featured post down" className="h-8 w-8 rounded-full p-0"><ArrowDown className="h-4 w-4" /></Button></>}</div>})}</div> : <div className="rounded-2xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">You don’t have any posts to feature yet.</div> : media.length ? <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">{media.map(item=>{const selected=draft.featured_media_urls.includes(item.url);return <button type="button" key={item.url} onClick={()=>updateDraft('featured_media_urls',selected?draft.featured_media_urls.filter(url=>url!==item.url):[...draft.featured_media_urls,item.url])} aria-pressed={selected} className={`relative overflow-hidden rounded-2xl border text-left ${selected?'border-foreground ring-2 ring-foreground/20':'border-border'}`}><img src={item.url} alt="Media from your post" className="aspect-square w-full bg-muted object-cover" loading="lazy" /><span className="absolute left-2 top-2 grid h-6 w-6 place-items-center rounded-full bg-background/90 text-foreground">{selected?<Check className="h-4 w-4" />:<Plus className="h-4 w-4" />}</span><span className="block truncate px-3 py-2 text-xs text-muted-foreground">{item.caption||'Media item'}</span></button>})}</div> : <div className="rounded-2xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">No post media found.</div>}</div>;
    }

    if (section === 'layout') return <div className="space-y-5">
      <div><h3 className="text-sm font-semibold text-foreground">Choose a profile layout</h3><p className="mt-1 text-sm leading-6 text-muted-foreground">This changes how your posts are presented on your profile. It does not change post visibility or privacy.</p></div>
      <div className="grid gap-3">
        {([
          { value: 'feed', title: 'Classic feed', description: 'Show your recent posts as full-width cards.', icon: UserRound },
          { value: 'grid', title: 'Media grid', description: 'Browse posts in a compact, tappable gallery.', icon: Image },
          { value: 'highlights', title: 'Featured first', description: 'Put your chosen posts and media before the recent feed.', icon: Star },
        ] as const).map(option => {
          const Icon = option.icon;
          const selected = draft.profile_layout === option.value;
          return <button type="button" key={option.value} aria-pressed={selected} onClick={() => updateDraft('profile_layout', option.value)} className={`flex w-full items-start gap-4 rounded-2xl border p-4 text-left transition-colors ${selected ? 'border-foreground bg-muted/60 ring-2 ring-foreground/10' : 'border-border bg-card hover:bg-muted/40'}`}>
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-muted text-foreground"><Icon className="h-5 w-5" /></span>
            <span className="min-w-0 flex-1"><span className="flex flex-wrap items-center gap-2"><span className="font-semibold text-card-foreground">{option.title}</span>{selected && <span className="rounded-full bg-foreground px-2.5 py-1 text-[10px] font-bold uppercase tracking-wide text-background">Selected</span>}</span><span className="mt-1 block text-sm leading-5 text-muted-foreground">{option.description}</span>
              <span aria-hidden="true" className="mt-3 flex h-10 items-center gap-2 overflow-hidden rounded-xl bg-background p-2">
                {option.value === 'feed' ? <><span className="h-6 w-6 rounded-full bg-muted" /><span className="h-2 flex-1 rounded-full bg-muted" /><span className="h-5 w-10 rounded-md bg-muted" /></> : option.value === 'grid' ? <><span className="h-6 flex-1 rounded-md bg-muted" /><span className="h-6 flex-1 rounded-md bg-muted" /><span className="h-6 flex-1 rounded-md bg-muted" /></> : <><span className="h-6 w-12 rounded-md bg-foreground/15" /><span className="h-6 w-12 rounded-md bg-foreground/15" /><span className="h-6 flex-1 rounded-md bg-muted" /></>}
              </span>
            </span>
          </button>;
        })}
      </div>
    </div>;

    return null;
  };

  const accountInitial = draft.display_name.trim().slice(0,1).toUpperCase() || 'S';
  const meta = activeMeta;
  const imageFor = (url: string | null) => {
    if (!url) return null;
    try { const candidate = url.startsWith('/') ? url : /^https?:\/\//i.test(url) ? url : `https://${url}`; const parsed = new URL(candidate,window.location.origin); return parsed.protocol==='https:'||parsed.origin===window.location.origin?parsed.toString():null; } catch { return null; }
  };

  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="sticky top-0 z-30 border-b border-border bg-card"><div className="mx-auto flex h-16 max-w-4xl items-center gap-3 px-4 sm:px-6"><Button type="button" variant="ghost" onClick={() => section ? setSection(null) : requestClose()} aria-label={section ? 'Back to profile editor' : 'Back to profile'} className="h-10 w-10 shrink-0 rounded-full p-0 text-foreground hover:bg-muted"><ArrowLeft className="h-5 w-5" /></Button><div className="min-w-0 flex-1"><p className="truncate text-[10px] font-bold uppercase tracking-[0.16em] text-muted-foreground">{section ? 'Edit profile' : 'Profile settings'}</p><h1 className="truncate text-base font-semibold text-card-foreground">{meta?.title || 'Edit profile'}</h1></div>{saveButton}</div></header>
      <main className="mx-auto w-full max-w-4xl space-y-5 px-4 py-6 sm:px-6 sm:py-8">
        {(error||notice)&&<div role={error?'alert':'status'} className={error?'rounded-2xl border border-border bg-card px-4 py-3 text-sm text-foreground':'rounded-2xl border border-border bg-muted/50 px-4 py-3 text-sm text-foreground'}>{error||notice}</div>}
        {section ? <section className="space-y-5"><div className="rounded-3xl border border-border bg-card p-5 shadow-sm sm:p-7"><div className="flex items-start gap-4">{meta&&<span className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-muted text-foreground"><meta.icon className="h-5 w-5" /></span>}<div><p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">{meta?.group}</p><h2 className="mt-1 text-2xl font-semibold tracking-tight text-card-foreground">{meta?.title}</h2><p className="mt-2 text-sm leading-6 text-muted-foreground">{meta?.subtitle}</p></div></div></div><div className="rounded-3xl border border-border bg-card p-5 shadow-sm sm:p-7">{renderProfileSection()}</div><div className="flex justify-end"><SaveButton isSaving={saving} onClick={()=>void saveSection(section)} dirty={isSectionDirty(section)} /></div></section> : <div className="space-y-6">
          <div className="space-y-2"><p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">Your profile</p><h2 className="text-3xl font-semibold tracking-tight text-foreground">Edit profile</h2><p className="max-w-2xl text-sm leading-6 text-muted-foreground">Edit every profile section available in the Android app. Each section opens its own screen and saves to your account.</p></div>
          <div className="overflow-hidden rounded-3xl border border-border bg-card shadow-sm"><div className="grid aspect-[16/5] place-items-center bg-gradient-to-r from-zinc-100 to-zinc-300 dark:from-zinc-800 dark:to-zinc-700">{imageFor(draft.profile_cover_image)?<img src={imageFor(draft.profile_cover_image)||undefined} alt="Profile cover preview" className="h-full w-full object-cover" />:<span className="text-sm text-muted-foreground">Add a cover photo</span>}</div><div className="flex items-center gap-4 p-5"><span className="grid h-16 w-16 shrink-0 place-items-center overflow-hidden rounded-full border-2 border-card bg-muted text-lg font-semibold text-muted-foreground">{imageFor(draft.avatar)?<img src={imageFor(draft.avatar)||undefined} alt="" className="h-full w-full object-cover" />:accountInitial}</span><div className="min-w-0 flex-1"><p className="truncate text-lg font-semibold text-card-foreground">{draft.display_name||user.fullName}</p><p className="truncate text-sm text-muted-foreground">@{draft.username||user.username}</p>{draft.bio&&<p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{draft.bio}</p>}</div><div className="hidden rounded-full bg-muted px-3 py-1.5 text-xs text-muted-foreground sm:block">{completedCount}/8 profile details</div></div></div>
          {isDirty&&<div className="rounded-2xl border border-border bg-muted/50 px-4 py-3 text-xs text-muted-foreground">You have unsaved edits. Open the section and choose Save changes to keep them.</div>}
          {grouped.map(group=><section key={group} className="space-y-2"><h3 className="px-1 text-xs font-bold uppercase tracking-[0.16em] text-muted-foreground">{group}</h3><div className="overflow-hidden rounded-3xl border border-border bg-card shadow-sm divide-y divide-border">{SECTIONS.filter(item=>item.group===group).map(item=>{const Icon=item.icon;return <button type="button" key={item.id} onClick={()=>{setSection(item.id);setError(null);setNotice(null);}} className="flex w-full items-center gap-4 px-4 py-4 text-left transition-colors hover:bg-muted/50 sm:px-5"><span className="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-muted text-foreground"><Icon className="h-4 w-4" /></span><span className="min-w-0 flex-1"><span className="block text-sm font-semibold text-card-foreground">{item.title}</span><span className="mt-1 block text-xs leading-5 text-muted-foreground">{item.subtitle}</span></span><ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" /></button>})}</div></section>)}
          {showDiscard&&<div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4"><div role="alertdialog" aria-modal="true" className="w-full max-w-sm rounded-3xl border border-border bg-card p-6 shadow-xl"><h3 className="text-lg font-semibold text-card-foreground">Discard unsaved edits?</h3><p className="mt-2 text-sm leading-6 text-muted-foreground">Changes that you haven’t saved in a section will be lost.</p><div className="mt-5 flex justify-end gap-2"><Button type="button" variant="outline" onClick={()=>setShowDiscard(false)} className="rounded-full">Keep editing</Button><Button type="button" onClick={onCancel} className="rounded-full bg-foreground text-background">Discard</Button></div></div></div>}
        </div>}
      </main>
    </div>
  );
};
