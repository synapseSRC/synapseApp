import { useEffect, useMemo, useState } from 'react';
import { supabase } from '@/lib/supabase';
import { useAuthStore } from '@/features/auth/useAuthStore';
import type { UserProfile } from '@/types';
import { STORAGE_BUCKETS } from '@/config/storageBuckets';
import { EDITABLE_PROFILE_SELECT, SECTIONS, mediaUrls, normalizeImageUrl, normalizeUrl, githubUrl, sectionKeys, toDraft, type BusinessAccount, type EditableProfile, type FeaturedMedia, type FeaturedPost, type HistoryItem, type ProfileDraft, type ProfileEditorProps, type SectionId } from './profileEditorShared';

export const useProfileEditor = ({ user, profile, onCancel, onSaved }: ProfileEditorProps) => {
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
    const { error } = await supabase.storage.from(STORAGE_BUCKETS.AVATARS).upload(path, file, { cacheControl: '31536000', contentType: file.type, upsert: false });
    if (error) throw error;
    return supabase.storage.from(STORAGE_BUCKETS.AVATARS).getPublicUrl(path).data.publicUrl;
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

  const grouped = ['Profile', 'Professional', 'Customize'] as const;


  const accountInitial = draft.display_name.trim().slice(0, 1).toUpperCase() || 'S';
  const imageFor = (url: string | null) => {
    if (!url) return null;
    try { const candidate = url.startsWith('/') ? url : /^https?:\/\//i.test(url) ? url : `https://${url}`; const parsed = new URL(candidate, window.location.origin); return parsed.protocol === 'https:' || parsed.origin === window.location.origin ? parsed.toString() : null; } catch { return null; }
  };

  return { user, profile, onCancel, section, setSection, draft, savedDraft, setDraft, setSavedDraft, avatarFile, setAvatarFile, coverFile, setCoverFile, avatarFilePreview, coverFilePreview, saving, error, setError, notice, setNotice, showDiscard, setShowDiscard, pendingInterest, setPendingInterest, avatarHistory, coverHistory, historyLoading, historyError, candidatePosts, candidateMedia, featuredLoading, featuredError, featuredTab, setFeaturedTab, businessAccount, businessLoading, businessError, confirmBusiness, setConfirmBusiness, switchingBusiness, isDirty, activeMeta, avatarPreview, coverPreview, updateDraft, saveSection, addInterest, changeBusinessAccount, isSectionDirty, completedCount, requestClose, selectFile, grouped, accountInitial, imageFor };
};

export type ProfileEditorModel = ReturnType<typeof useProfileEditor>;
