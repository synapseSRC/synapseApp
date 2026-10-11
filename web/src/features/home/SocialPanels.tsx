import React, { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, ArrowLeft, CalendarDays, MessageCircle, Pencil, RefreshCw, Search } from 'lucide-react';
import { ChatThread } from './ChatThread';
import { EDITABLE_PROFILE_SELECT, ProfileEditor, type EditableProfile, type ProfileLayout } from '@/features/profile/ProfileEditor';
import { Button } from '@/components/ui/Button';
import { supabase } from '@/lib/supabase';
import type { UserProfile } from '@/types';

type ProfileRecord = EditableProfile;

interface ProfilePost {
  id: string;
  author_uid: string;
  post_text: string | null;
  post_image: string | null;
  media_items: unknown;
  timestamp: number;
  publish_date: string | null;
  created_at: string | null;
  likes_count: number | null;
  comments_count: number | null;
  reshares_count: number | null;
  is_deleted: boolean | null;
  post_type: string | null;
}

interface ProfilePanelProps {
  user: UserProfile;
  onBack: () => void;
  onProfileUpdated?: (profile: EditableProfile) => void;
}

const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0] || '').join('').toUpperCase() || 'S';

const safeImage = (value: string | null | undefined) => {
  if (!value) return null;
  try {
    const url = new URL(value, window.location.origin);
    return url.protocol === 'https:' || url.origin === window.location.origin ? url.toString() : null;
  } catch {
    return null;
  }
};

type ProfilePostMedia = { url: string; isVideo: boolean };

const profilePostMedia = (post: ProfilePost): ProfilePostMedia | null => {
  const postTypeIsVideo = (post.post_type || '').toLowerCase().includes('video');
  const candidates: ProfilePostMedia[] = post.post_image ? [{ url: post.post_image, isVideo: false }] : [];
  if (Array.isArray(post.media_items)) {
    for (const item of post.media_items) {
      if (typeof item === 'string') candidates.push({ url: item, isVideo: postTypeIsVideo });
      else if (item && typeof item === 'object') {
        const row = item as Record<string, unknown>;
        const url = row.url || row.media_url || row.uri || row.src;
        if (typeof url !== 'string') continue;
        const kind = typeof row.type === 'string' ? row.type : typeof row.media_type === 'string' ? row.media_type : '';
        candidates.push({ url, isVideo: kind.toLowerCase().includes('video') || postTypeIsVideo });
      }
    }
  }
  for (const candidate of candidates) {
    const url = safeImage(candidate.url);
    if (url) return { ...candidate, url };
  }
  return null;
};

const stringList = (value: unknown): string[] => Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : [];

const Avatar: React.FC<{ name: string; image?: string | null; size?: 'md' | 'lg' }> = ({ name, image, size = 'md' }) => {
  const src = safeImage(image);
  return (
    <span className={`grid ${size === 'lg' ? 'h-20 w-20 text-xl' : 'h-11 w-11 text-sm'} shrink-0 place-items-center overflow-hidden rounded-full bg-zinc-100 dark:bg-zinc-800 font-semibold text-zinc-700 dark:text-zinc-300 ring-1 ring-zinc-200 dark:ring-zinc-700`}>
      {src ? <img src={src} alt="" className="h-full w-full object-cover" loading="lazy" /> : initials(name)}
    </span>
  );
};

const relativeTime = (post: ProfilePost) => {
  let value = Number(post.timestamp);
  if (!Number.isFinite(value) || value <= 0) value = Date.parse(post.publish_date || post.created_at || '');
  else if (value < 1_000_000_000_000) value *= 1000;
  if (!Number.isFinite(value) || value <= 0) return 'Recently';
  const days = Math.max(0, Math.floor((Date.now() - value) / 86_400_000));
  return days === 0 ? 'Today' : days === 1 ? '1 day ago' : `${days} days ago`;
};

export const ProfilePanel: React.FC<ProfilePanelProps> = ({ user, onBack, onProfileUpdated }) => {
  const [profile, setProfile] = useState<ProfileRecord | null>(null);
  const [posts, setPosts] = useState<ProfilePost[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);
  const [isEditing, setIsEditing] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [selectedGridPostId, setSelectedGridPostId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const [profileResult, postResult] = await Promise.all([
          supabase.from('users')
            .select(EDITABLE_PROFILE_SELECT)
            .eq('uid', user.id)
            .maybeSingle(),
          supabase.from('posts')
            .select('id,author_uid,post_text,post_image,media_items,timestamp,publish_date,created_at,likes_count,comments_count,reshares_count,is_deleted,post_type')
            .eq('author_uid', user.id)
            .or('is_deleted.is.null,is_deleted.eq.false')
            .order('timestamp', { ascending: false })
            .limit(100),
        ]);
        if (profileResult.error) throw profileResult.error;
        if (postResult.error) throw postResult.error;
        if (!cancelled) {
          setProfile(profileResult.data as ProfileRecord | null);
          setPosts(((postResult.data || []) as ProfilePost[]).filter(post => post.is_deleted !== true));
        }
      } catch (error) {
        if (!cancelled) setError(error instanceof Error ? error.message : 'Could not load this profile.');
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };
    void load();
    return () => { cancelled = true; };
  }, [user.id, refreshToken]);

  const displayName = profile?.display_name || user.fullName || user.username;
  const username = profile?.username || user.username;
  const avatar = profile?.avatar || user.avatarUrl;
  const joinedAt = profile?.created_at || user.createdAt;
  const joinedLabel = joinedAt && Number.isFinite(Date.parse(joinedAt))
    ? new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric' }).format(new Date(joinedAt))
    : null;
  const website = safeImage(profile?.website);
  const cover = safeImage(profile?.profile_cover_image);
  const profileLayout: ProfileLayout = profile?.profile_layout || 'feed';
  const featuredPosts = stringList(profile?.featured_post_ids)
    .map(id => posts.find(post => post.id === id))
    .filter((post): post is ProfilePost => Boolean(post));
  const featuredMedia = stringList(profile?.featured_media_urls).map(url => safeImage(url)).filter((url): url is string => Boolean(url));
  const selectedGridPost = posts.find(post => post.id === selectedGridPostId) || null;
  const emptyPosts = <div className="rounded-3xl border border-zinc-200 bg-white px-6 py-12 text-center dark:border-zinc-800 dark:bg-zinc-900"><p className="font-medium text-zinc-900 dark:text-zinc-100">No posts yet</p><p className="mt-2 text-sm text-zinc-500 dark:text-zinc-400">Your shared updates will appear here.</p></div>;
  const renderPost = (post: ProfilePost) => {
    const media = profilePostMedia(post);
    return <article key={post.id} className="rounded-3xl border border-zinc-200 bg-white p-5 dark:border-zinc-800 dark:bg-zinc-900">
      <div className="flex items-center gap-3"><Avatar name={displayName} image={avatar} /><div><p className="text-sm font-semibold text-zinc-950 dark:text-zinc-100">{displayName}</p><p className="text-xs text-zinc-500 dark:text-zinc-400">@{username} · {relativeTime(post)}</p></div></div>
      {post.post_text && <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-zinc-800 dark:text-zinc-200">{post.post_text}</p>}
      {media && (media.isVideo ? <video controls playsInline preload="metadata" src={media.url} aria-label="Post video" className="mt-4 max-h-[32rem] w-full rounded-2xl bg-zinc-100 object-cover dark:bg-zinc-800" /> : <img src={media.url} alt="Post attachment" className="mt-4 max-h-[32rem] w-full rounded-2xl bg-zinc-100 object-cover dark:bg-zinc-800" loading="lazy" />)}
      <div className="mt-4 flex gap-5 border-t border-zinc-100 pt-3 text-xs text-zinc-500 dark:border-zinc-800 dark:text-zinc-400"><span>{post.likes_count || 0} likes</span><span>{post.comments_count || 0} comments</span></div>
    </article>;
  };

  if (isEditing && profile) {
    return <ProfileEditor user={user} profile={profile} onCancel={() => setIsEditing(false)} onSaved={(updated, warning) => {
      setProfile(updated);
      setNotice(warning || 'Your profile has been updated.');
      onProfileUpdated?.(updated);
    }} />;
  }

  return (
    <section className="mx-auto w-full max-w-3xl space-y-5">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Button type="button" variant="outline" onClick={onBack} className="h-10 w-10 rounded-full border-zinc-300 dark:border-zinc-700 p-0 text-zinc-700 dark:text-zinc-300 shadow-none"><ArrowLeft className="h-4 w-4" /><span className="sr-only">Back to Home</span></Button>
          <div><p className="text-xs font-semibold uppercase tracking-[0.16em] text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">Your account</p><h2 className="text-xl font-semibold text-zinc-950 dark:text-zinc-100">Profile</h2></div>
        </div>
        <div className="flex items-center gap-2">
          <Button type="button" variant="outline" onClick={() => setIsEditing(true)} disabled={isLoading || !profile} className="h-10 rounded-full border-zinc-300 px-4 text-xs font-semibold text-zinc-800 shadow-none hover:bg-zinc-100 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-800"><Pencil className="mr-2 h-3.5 w-3.5" />Edit profile</Button>
          <Button type="button" variant="ghost" onClick={() => setRefreshToken(value => value + 1)} disabled={isLoading} aria-label="Refresh profile" className="h-10 w-10 rounded-full p-0 text-zinc-600 hover:bg-zinc-200 dark:text-zinc-400 dark:hover:bg-zinc-800"><RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} /></Button>
        </div>
      </div>

      {notice && <p role="status" className="rounded-2xl border border-zinc-200 bg-white px-4 py-3 text-sm text-zinc-700 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300">{notice}</p>}
      <article className="overflow-hidden rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-[0_2px_14px_-10px_rgba(0,0,0,0.16)]">
        <div className="relative h-28 overflow-hidden bg-gradient-to-r from-zinc-100 via-white to-zinc-200 dark:from-zinc-800 dark:via-zinc-900 dark:to-zinc-700">{cover && <img src={cover} alt="" aria-hidden="true" className="h-full w-full object-cover" />}</div>
        <div className="px-6 pb-6">
          <div className="-mt-10 flex items-end justify-between gap-4">
            <Avatar name={displayName} image={avatar} size="lg" />
            <span className="mb-1 rounded-full border border-zinc-200 dark:border-zinc-800 px-3 py-1 text-xs text-zinc-600 dark:text-zinc-400 dark:text-zinc-500">Synapse member</span>
          </div>
          <div className="mt-4">
            <div className="flex items-center gap-2"><h3 className="text-2xl font-semibold tracking-tight text-zinc-950 dark:text-zinc-100">{displayName}</h3>{profile?.verify && <span className="grid h-5 w-5 place-items-center rounded-full bg-zinc-950 text-[11px] font-bold text-white">✓</span>}</div>
            <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">@{username}</p>
            {profile?.bio && <p className="mt-4 max-w-xl whitespace-pre-wrap text-sm leading-6 text-zinc-700 dark:text-zinc-300">{profile.bio}</p>}
            <div className="mt-4 flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">
              {joinedLabel && <span className="inline-flex items-center gap-2"><CalendarDays className="h-4 w-4" />Joined {joinedLabel}</span>}
              {website && <a className="underline underline-offset-2 hover:text-zinc-950 dark:text-zinc-100" href={website} target="_blank" rel="noreferrer">{profile?.website?.replace(/^https?:\/\//, '')}</a>}
              {profile?.region && <span>{profile.region}</span>}
            </div>
          </div>
          {profile && <div className="mt-6 grid grid-cols-3 border-t border-zinc-100 dark:border-zinc-800 pt-5 text-center">
            <div><p className="text-lg font-semibold text-zinc-950 dark:text-zinc-100">{profile.posts_count ?? '—'}</p><p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">Posts</p></div>
            <div><p className="text-lg font-semibold text-zinc-950 dark:text-zinc-100">{profile.followers_count ?? '—'}</p><p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">Followers</p></div>
            <div><p className="text-lg font-semibold text-zinc-950 dark:text-zinc-100">{profile.following_count ?? '—'}</p><p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">Following</p></div>
          </div>}
        </div>
      </article>

      {error && <div role="alert" className="rounded-2xl border border-zinc-300 bg-white px-5 py-4 text-sm text-zinc-700 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-300">{error}</div>}
      <div className="flex items-end justify-between px-1"><div><h3 className="text-lg font-semibold text-zinc-950 dark:text-zinc-100">{profileLayout === 'grid' ? 'Post gallery' : profileLayout === 'highlights' ? 'Featured profile' : 'Your posts'}</h3><p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">{profileLayout === 'grid' ? 'Select a tile to preview the post.' : profileLayout === 'highlights' ? 'Your chosen content appears before recent posts.' : 'Recent updates from your profile.'}</p></div><span className="text-xs text-zinc-500 dark:text-zinc-400">{posts.length} loaded</span></div>
      {isLoading ? <div className="h-40 animate-pulse rounded-3xl border border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-900" /> : error ? null : profileLayout === 'grid' ? posts.length ? <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">{posts.map(post => {
          const media = profilePostMedia(post);
          const selected = selectedGridPostId === post.id;
          return <button type="button" key={post.id} onClick={() => setSelectedGridPostId(current => current === post.id ? null : post.id)} aria-pressed={selected} aria-label={`${selected ? 'Close preview for' : 'Preview'} post from ${relativeTime(post)}`} className={`group relative aspect-square overflow-hidden rounded-2xl border text-left transition ${selected ? 'border-zinc-950 ring-2 ring-zinc-950/20 dark:border-white dark:ring-white/20' : 'border-zinc-200 dark:border-zinc-800'}`}>
            {media ? media.isVideo ? <video muted playsInline preload="metadata" src={media.url} aria-hidden="true" className="h-full w-full object-cover" /> : <img src={media.url} alt="" aria-hidden="true" className="h-full w-full object-cover transition-transform group-hover:scale-[1.03]" loading="lazy" /> : <span className="flex h-full w-full items-center justify-center overflow-hidden bg-zinc-100 p-4 text-sm leading-5 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-200">{post.post_text || 'Text post'}</span>}
            <span className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/75 to-transparent px-3 pb-3 pt-8 text-xs font-medium text-white">{relativeTime(post)}{media?.isVideo ? ' · Video' : ''}</span>
          </button>;
        })}</div>
        {selectedGridPost && <div className="space-y-3"><div className="flex items-center justify-between px-1"><h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">Post preview</h4><Button type="button" variant="ghost" onClick={() => setSelectedGridPostId(null)} className="h-8 rounded-full px-3 text-xs">Close</Button></div>{renderPost(selectedGridPost)}</div>}
      </div> : emptyPosts : profileLayout === 'highlights' ? <div className="space-y-7">
        <section className="space-y-3"><div className="flex items-center justify-between px-1"><h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">Highlights</h4><span className="text-xs text-zinc-500 dark:text-zinc-400">{featuredPosts.length + featuredMedia.length} selected</span></div>
          {featuredPosts.length || featuredMedia.length ? <div className="space-y-3">{featuredPosts.map(renderPost)}{featuredMedia.length > 0 && <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">{featuredMedia.map((url, index) => <img key={`${url}-${index}`} src={url} alt={`Featured profile media ${index + 1}`} className="aspect-square w-full rounded-2xl border border-zinc-200 bg-zinc-100 object-cover dark:border-zinc-800 dark:bg-zinc-900" loading="lazy" />)}</div>}</div> : <div className="rounded-2xl border border-dashed border-zinc-300 px-5 py-6 text-sm leading-6 text-zinc-600 dark:border-zinc-700 dark:text-zinc-300">No featured content selected. Use <strong>Edit profile → Featured content</strong> to choose posts or media.</div>}
        </section>
        <section className="space-y-3"><h4 className="px-1 text-sm font-semibold text-zinc-900 dark:text-zinc-100">Recent posts</h4>{posts.length ? <div className="space-y-4">{posts.map(renderPost)}</div> : emptyPosts}</section>
      </div> : posts.length ? <div className="space-y-4">{posts.map(renderPost)}</div> : emptyPosts}
    </section>
  );
};

interface ChatRow { id: string; name: string | null; is_group: boolean | null; avatar_url: string | null; updated_at: string | null; }
interface ChatMemberRow { chat_id: string; user_id: string; }
interface ChatMembershipRow { chat_id: string; is_archived: boolean | null; is_pinned: boolean | null; }
interface ChatProfile { uid: string; username: string | null; display_name: string | null; avatar: string | null; verify: boolean | null; }
interface ChatConversationView { chat: ChatRow; otherMembers: ChatProfile[]; title: string; avatar: string | null; }

const activityLabel = (value: string | null) => {
  if (!value || !Number.isFinite(Date.parse(value))) return 'Conversation';
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(new Date(value));
};

export const MessagesPanel: React.FC<{ user: UserProfile; onBack: () => void; onThreadOpenChange?: (open: boolean) => void }> = ({ user, onBack, onThreadOpenChange }) => {
  const [conversations, setConversations] = useState<ChatConversationView[]>([]);
  const [selectedChatId, setSelectedChatId] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadConversations = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const { data: memberships, error: membershipError } = await supabase
        .from('chat_participants')
        .select('chat_id,is_archived,is_pinned')
        .eq('user_id', user.id)
        .or('is_archived.is.null,is_archived.eq.false');
      if (membershipError) throw membershipError;
      const membershipRows = (memberships || []) as ChatMembershipRow[];
      const chatIds = [...new Set(membershipRows.map(row => row.chat_id))];
      if (chatIds.length === 0) {
        setConversations([]);
        setSelectedChatId(null);
        return;
      }

      const [chatResult, memberResult] = await Promise.all([
        supabase.from('chats').select('id,name,is_group,avatar_url,updated_at').in('id', chatIds),
        supabase.from('chat_participants').select('chat_id,user_id').in('chat_id', chatIds),
      ]);
      if (chatResult.error) throw chatResult.error;
      if (memberResult.error) throw memberResult.error;
      const chats = (chatResult.data || []) as ChatRow[];
      const members = (memberResult.data || []) as ChatMemberRow[];
      const otherIds = [...new Set(members.filter(member => member.user_id !== user.id).map(member => member.user_id))];
      let profileMap: Record<string, ChatProfile> = {};
      if (otherIds.length) {
        const { data: profileRows, error: profileError } = await supabase.from('users').select('uid,username,display_name,avatar,verify').in('uid', otherIds);
        if (profileError) throw profileError;
        profileMap = Object.fromEntries(((profileRows || []) as ChatProfile[]).map(profile => [profile.uid, profile]));
      }
      const mapped = chats.map(chat => {
        const otherMembers = members.filter(member => member.chat_id === chat.id && member.user_id !== user.id).map(member => profileMap[member.user_id]).filter(Boolean);
        const title = chat.is_group ? (chat.name || 'Group conversation') : (otherMembers[0]?.display_name || otherMembers[0]?.username || 'Conversation');
        const avatar = chat.avatar_url || otherMembers[0]?.avatar || null;
        return { chat, otherMembers, title, avatar };
      }).sort((a, b) => Date.parse(b.chat.updated_at || '') - Date.parse(a.chat.updated_at || ''));
      setConversations(mapped);
      setSelectedChatId(current => current && mapped.some(conversation => conversation.chat.id === current) ? current : null);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Could not load conversations. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }, [user.id]);

  useEffect(() => { void loadConversations(); }, [loadConversations]);
  useEffect(() => { onThreadOpenChange?.(Boolean(selectedChatId)); }, [selectedChatId, onThreadOpenChange]);
  useEffect(() => () => onThreadOpenChange?.(false), [onThreadOpenChange]);

  const filtered = conversations.filter(conversation => {
    const query = search.trim().toLowerCase();
    return !query || conversation.title.toLowerCase().includes(query) || conversation.otherMembers.some(member => member.username?.toLowerCase().includes(query));
  });
  const selected = conversations.find(conversation => conversation.chat.id === selectedChatId) || null;
  if (selected) return <ChatThread chatId={selected.chat.id} title={selected.title} subtitle={selected.chat.is_group ? 'Group conversation' : selected.otherMembers[0]?.username ? `@${selected.otherMembers[0].username}` : 'Direct conversation'} avatarUrl={selected.avatar} user={user} onBack={() => { onThreadOpenChange?.(false); setSelectedChatId(null); }} />;

  return (
    <section className="mx-auto w-full max-w-6xl space-y-5">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3"><Button type="button" variant="outline" onClick={onBack} className="h-10 w-10 rounded-full border-zinc-300 dark:border-zinc-700 p-0 text-zinc-700 dark:text-zinc-300 shadow-none"><ArrowLeft className="h-4 w-4" /><span className="sr-only">Back to Home</span></Button><div><p className="text-xs font-semibold uppercase tracking-[0.16em] text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">Your conversations</p><h2 className="text-xl font-semibold text-zinc-950 dark:text-zinc-100">Messages</h2></div></div>
        <Button type="button" variant="ghost" onClick={() => void loadConversations()} disabled={isLoading} aria-label="Refresh conversations" className="h-10 w-10 rounded-full p-0 text-zinc-600 dark:text-zinc-400 dark:text-zinc-500 hover:bg-zinc-200 dark:bg-zinc-800"><RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} /></Button>
      </div>
      {error && <div role="alert" className="rounded-2xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-5 py-4 text-sm text-zinc-700 dark:text-zinc-300">{error}</div>}
      <div role="note" className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-amber-950"><AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" /><p className="text-xs leading-5"><strong>Web chat is not end-to-end encrypted.</strong> Text and media sent here can be accessed by Synapse’s server. Avoid sensitive information. E2EE messages from Android can’t be read in this web app.</p></div>
      <div className="grid min-h-[540px] overflow-hidden rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-[0_2px_14px_-10px_rgba(0,0,0,0.16)] lg:grid-cols-[330px_minmax(0,1fr)]">
        <aside className={`${selectedChatId ? 'hidden lg:flex' : 'flex'} min-h-[540px] flex-col border-r border-zinc-200 dark:border-zinc-800`}>
          <div className="space-y-4 border-b border-zinc-100 dark:border-zinc-800 p-5"><div><h3 className="font-semibold text-zinc-950 dark:text-zinc-100">Inbox</h3><p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">{conversations.length} conversations</p></div><label className="relative block"><Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400 dark:text-zinc-500" /><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Search people" className="h-10 w-full rounded-full border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-950 pl-9 pr-4 text-sm outline-none focus:border-zinc-400" /></label></div>
          <div className="flex-1 overflow-y-auto">
            {isLoading ? <div className="space-y-3 p-4">{[1, 2, 3].map(item => <div key={item} className="h-16 animate-pulse rounded-2xl bg-zinc-100 dark:bg-zinc-800" />)}</div> : filtered.length ? filtered.map(conversation => {
              const selectedNow = selectedChatId === conversation.chat.id;
              const memberName = conversation.chat.is_group ? `${conversation.otherMembers.length + 1} members` : `@${conversation.otherMembers[0]?.username || 'member'}`;
              const avatarName = conversation.otherMembers[0]?.display_name || conversation.title;
              return <button type="button" key={conversation.chat.id} onClick={() => { onThreadOpenChange?.(true); setSelectedChatId(conversation.chat.id); }} className={`flex w-full items-center gap-3 border-b border-zinc-100 dark:border-zinc-800 px-4 py-4 text-left transition-colors ${selectedNow ? 'bg-zinc-100 dark:bg-zinc-800' : 'hover:bg-zinc-50 dark:bg-zinc-950'}`}>
                <Avatar name={avatarName} image={conversation.avatar} />
                <span className="min-w-0 flex-1"><span className="flex items-center justify-between gap-2"><span className="truncate text-sm font-semibold text-zinc-950 dark:text-zinc-100">{conversation.title}</span><span className="shrink-0 text-[11px] text-zinc-400 dark:text-zinc-500">{activityLabel(conversation.chat.updated_at)}</span></span><span className="mt-1 block truncate text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">{memberName}</span></span>
              </button>;
            }) : !error && <div className="p-6 text-center"><p className="text-sm font-medium text-zinc-800 dark:text-zinc-200">No conversations yet</p><p className="mt-1 text-xs leading-5 text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">Your existing Synapse conversations will appear here.</p></div>}
          </div>
        </aside>
        <div className="hidden min-h-[540px] flex-1 flex-col bg-zinc-50 dark:bg-zinc-950 lg:flex">
          <div className="flex flex-1 flex-col items-center justify-center p-8 text-center"><span className="grid h-14 w-14 place-items-center rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-zinc-800 dark:text-zinc-200"><MessageCircle className="h-6 w-6" /></span><h3 className="mt-5 text-base font-semibold text-zinc-950 dark:text-zinc-100">Your inbox</h3><p className="mt-2 max-w-sm text-sm leading-6 text-zinc-600 dark:text-zinc-400">Select a conversation to continue.</p></div>
        </div>
      </div>
    </section>
  );
};
