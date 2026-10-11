import React, { useCallback, useEffect, useState } from 'react';
import { AlertCircle, ArrowLeft, Check, Image, Loader2, LogOut, MapPin, MessageCircle, RefreshCw, Send, Settings2, Sparkles, Users, Youtube } from 'lucide-react';
import { MessagesPanel, ProfilePanel } from './SocialPanels';
import { PostActionBar } from './PostActionBar';
import { buildPostInteractionStates, parseReplyUsernames, type PostInteractionState } from './postActions';
import { SettingsScreen } from '@/features/settings/SettingsScreen';
import { Button } from '@/components/ui/Button';
import { supabase } from '@/lib/supabase';
import type { UserProfile } from '@/types';

type FeedPost = {
  id: string;
  author_uid: string;
  post_text: string | null;
  post_image: string | null;
  media_items: unknown;
  post_type: string | null;
  timestamp: number;
  publish_date: string | null;
  created_at: string | null;
  likes_count: number | null;
  comments_count: number | null;
  reshares_count: number | null;
  is_deleted: boolean | null;
  has_poll?: boolean | null;
  poll_question?: string | null;
  poll_options?: unknown;
  poll_end_time?: string | null;
  youtube_url?: string | null;
  location_name?: string | null;
  metadata?: unknown;
  post_hide_like_count?: string | null;
  post_hide_comments_count?: string | null;
  post_disable_comments?: string | null;
  post_visibility?: string | null;
  in_reply_to_post_id?: string | null;
  root_post_id?: string | null;
  reply_to_usernames?: unknown;
  quoted_post_id?: string | null;
  is_quote?: boolean | null;
  is_edited?: boolean | null;
  edited_at?: string | null;
};

const PAGE_SIZE = 20;
const FEED_POST_FIELDS = 'id,author_uid,post_text,post_image,media_items,post_type,timestamp,publish_date,created_at,likes_count,comments_count,reshares_count,is_deleted,has_poll,poll_question,poll_options,poll_end_time,youtube_url,location_name,metadata,post_hide_like_count,post_hide_comments_count,post_disable_comments,post_visibility,in_reply_to_post_id,root_post_id,reply_to_usernames,quoted_post_id,is_quote,is_edited,edited_at';

type FeedStory = {
  id: string;
  user_id: string;
  content: string | null;
  media_url: string | null;
  thumbnail_url: string | null;
  media_type: string | null;
  created_at: string | null;
  expires_at: string | null;
};

type FeedPerson = {
  uid: string;
  username: string | null;
  display_name: string | null;
  avatar: string | null;
  verify: boolean | null;
  bio?: string | null;
  followers_count?: number | null;
  following_count?: number | null;
  posts_count?: number | null;
};

interface HomeScreenProps {
  user: UserProfile;
  onLogout: () => void;
  onCreatePost: () => void;
}

const safeMediaUrl = (value: string | null | undefined) => {
  if (!value) return null;
  try {
    const isAbsolute = /^https?:\/\//i.test(value);
    if (!isAbsolute && !value.startsWith('/')) return null;
    const url = new URL(value, window.location.origin);
    return url.protocol === 'https:' || url.origin === window.location.origin ? url.toString() : null;
  } catch {
    return null;
  }
};

const postMediaItems = (post: FeedPost) => {
  const items: { url: string; type: string }[] = [];
  if (post.post_image) items.push({ url: post.post_image, type: 'image' });
  if (!Array.isArray(post.media_items)) return items;
  for (const entry of post.media_items) {
    if (typeof entry === 'string') items.push({ url: entry, type: post.post_type?.toLowerCase() || 'image' });
    else if (entry && typeof entry === 'object') {
      const item = entry as Record<string, unknown>;
      const url = item.url || item.media_url || item.uri || item.src;
      if (typeof url === 'string' && !items.some(existing => existing.url === url)) {
        items.push({ url, type: String(item.type || item.media_type || post.post_type || 'image').toLowerCase() });
      }
    }
  }
  return items;
};

const signPrivatePostMedia = async (rows: FeedPost[]) => {
  const paths = new Set<string>();
  const add = (value: unknown) => { if (typeof value === 'string' && value && !/^https?:\/\//i.test(value) && !value.startsWith('/')) paths.add(value); };
  for (const post of rows) {
    add(post.post_image);
    if (Array.isArray(post.media_items)) for (const entry of post.media_items) {
      if (typeof entry === 'string') add(entry);
      else if (entry && typeof entry === 'object') {
        const item = entry as Record<string, unknown>;
        if (!item.bucket || item.bucket === 'posts') add(item.url || item.media_url || item.uri || item.src);
      }
    }
  }
  if (!paths.size) return rows;
  const { data, error } = await supabase.storage.from('posts').createSignedUrls([...paths], 60 * 60);
  if (error) throw error;
  const signedUrls = new Map((data || []).flatMap(item => item.signedUrl && item.path ? [[item.path, item.signedUrl] as const] : []));
  const resolve = (value: unknown) => typeof value === 'string' ? signedUrls.get(value) || value : value;
  return rows.map(post => ({
    ...post,
    post_image: typeof post.post_image === 'string' ? String(resolve(post.post_image)) : post.post_image,
    media_items: Array.isArray(post.media_items) ? post.media_items.map(entry => {
      if (typeof entry === 'string') return resolve(entry);
      if (entry && typeof entry === 'object') {
        const item = entry as Record<string, unknown>;
        return { ...item, url: resolve(item.url || item.media_url || item.uri || item.src) };
      }
      return entry;
    }) : post.media_items,
  }));
};

const defaultInteraction = (post: FeedPost): PostInteractionState => ({
  isLiked: false,
  likesCount: post.likes_count || 0,
  isReshared: false,
  resharesCount: post.reshares_count || 0,
  isSaved: false,
  repliesCount: post.comments_count || 0,
});

const getPollOptions = (value: unknown) => Array.isArray(value) ? value.map(option => {
  if (typeof option === 'string') return option;
  if (option && typeof option === 'object' && 'text' in option) return String((option as { text: unknown }).text || '');
  return '';
}).filter(Boolean) : [];

const timeAgo = (post: FeedPost) => {
  let milliseconds = Number(post.timestamp);
  if (!Number.isFinite(milliseconds) || milliseconds <= 0) {
    milliseconds = Date.parse(post.publish_date || post.created_at || '');
  } else if (milliseconds < 1_000_000_000_000) {
    milliseconds *= 1000;
  }
  if (!Number.isFinite(milliseconds) || milliseconds <= 0) return 'Recently';
  const minutes = Math.max(0, Math.floor((Date.now() - milliseconds) / 60_000));
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h`;
  const days = Math.floor(hours / 24);
  return days < 7 ? `${days}d` : new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(milliseconds);
};

const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0] || '').join('').toUpperCase() || 'S';

const PersonAvatar: React.FC<{ name: string; image?: string | null; size?: 'sm' | 'md' | 'lg' }> = ({ name, image, size = 'md' }) => {
  const dimensions = size === 'lg' ? 'h-14 w-14 text-base' : size === 'sm' ? 'h-9 w-9 text-xs' : 'h-11 w-11 text-sm';
  const src = safeMediaUrl(image);
  return (
    <span className={`grid ${dimensions} shrink-0 place-items-center overflow-hidden rounded-full bg-zinc-100 dark:bg-zinc-800 font-semibold text-zinc-700 dark:text-zinc-300 ring-1 ring-zinc-200 dark:ring-zinc-700`}>
      {src ? <img src={src} alt="" className="h-full w-full object-cover" loading="lazy" /> : initials(name)}
    </span>
  );
};

interface FeedPostCardProps {
  post: FeedPost;
  author?: FeedPerson;
  replyTarget?: FeedPerson;
  currentUser: UserProfile;
  interaction: PostInteractionState;
  actionBusy?: boolean;
  onOpenPost: (post: FeedPost) => void;
  onLike: (post: FeedPost) => void;
  onReshare: (post: FeedPost) => void;
  onSave: (post: FeedPost) => void;
  onShare: (post: FeedPost) => void;
}

const FeedPostCard: React.FC<FeedPostCardProps> = ({ post, author, replyTarget, currentUser, interaction, actionBusy, onOpenPost, onLike, onReshare, onSave, onShare }) => {
  const isCurrentUser = post.author_uid === currentUser.id;
  const name = author?.display_name || author?.username || (isCurrentUser ? currentUser.fullName : 'Synapse member');
  const username = author?.username || (isCurrentUser ? currentUser.username : 'member');
  const avatar = author?.avatar || (isCurrentUser ? currentUser.avatarUrl : undefined);
  const media = postMediaItems(post).map(item => ({ ...item, url: safeMediaUrl(item.url) })).filter((item): item is { url: string; type: string } => Boolean(item.url));
  const pollOptions = getPollOptions(post.poll_options);
  const commentsHidden = post.post_hide_comments_count === 'true';
  const likesHidden = post.post_hide_like_count === 'true';
  const metadata = post.metadata && typeof post.metadata === 'object' ? post.metadata as Record<string, unknown> : {};
  const context = [metadata.feeling, metadata.activity].filter((item): item is string => typeof item === 'string' && Boolean(item.trim())).join(' · ');
  const textBackground = typeof metadata.textBackgroundColor === 'string' && /^#[0-9a-f]{6}$/i.test(metadata.textBackgroundColor) ? metadata.textBackgroundColor : null;
  const youtube = post.youtube_url && /^https?:\/\//i.test(post.youtube_url) ? post.youtube_url : null;
  const replyUsernames = parseReplyUsernames(post.reply_to_usernames);
  const replyUsername = replyTarget?.username || replyUsernames[0] || null;

  return (
    <article className="rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-5 shadow-[0_2px_14px_-10px_rgba(0,0,0,0.16)] sm:p-6">
      <header className="flex items-start gap-3">
        <PersonAvatar name={name} image={avatar} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
            <span className="truncate text-sm font-semibold text-zinc-950 dark:text-zinc-100">{name}</span>
            {author?.verify && <Check className="h-4 w-4 rounded-full bg-zinc-950 p-0.5 text-white" aria-label="Verified account" />}
            <span className="truncate text-sm text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">@{username}</span>
            <span className="text-zinc-300">·</span>
            <time className="text-sm text-zinc-500 dark:text-zinc-400">{timeAgo(post)}</time>
            {post.is_edited && <span className="text-xs text-zinc-400 dark:text-zinc-500">edited</span>}
          </div>
          {post.location_name && <span className="mt-1 inline-flex items-center gap-1 text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500"><MapPin className="h-3 w-3" />{post.location_name}</span>}
          {context && <span className="mt-1 block text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">{context}</span>}
        </div>
      </header>

      {(post.in_reply_to_post_id || replyUsernames.length > 0) && <button type="button" onClick={() => onOpenPost(post)} className="mt-3 text-left text-xs text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100">Replying to {replyUsername ? <span className="font-semibold text-zinc-800 dark:text-zinc-200">@{replyUsername.replace(/^@/, '')}</span> : 'a post'}</button>}
      {post.post_text && <p style={textBackground ? { backgroundColor: textBackground, color: textBackground.toLowerCase() === '#18181b' ? '#ffffff' : '#27272a' } : undefined} className={`mt-4 whitespace-pre-wrap break-words text-[15px] leading-7 text-zinc-800 dark:text-zinc-200 ${textBackground ? 'rounded-2xl px-4 py-3' : ''}`}>{post.post_text}</p>}

      {post.has_poll && (
        <div className="mt-4 rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-950 p-4">
          <p className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">{post.poll_question || 'Poll'}</p>
          <div className="mt-3 space-y-2">{pollOptions.map((option, index) => <div key={`${index}-${option}`} className="rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2.5 text-sm text-zinc-700 dark:text-zinc-300">{option}</div>)}</div>
          {post.poll_end_time && <p className="mt-3 text-[11px] text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">Ends {new Date(post.poll_end_time).toLocaleString()}</p>}
        </div>
      )}

      {media.length > 0 && <div className={`mt-4 grid gap-2 ${media.length === 1 ? 'grid-cols-1' : 'grid-cols-2'}`}>{media.slice(0, 4).map((item, index) => item.type.includes('video') || item.type === 'reel' ? <video key={`${item.url}-${index}`} src={item.url} controls playsInline className={`max-h-[34rem] w-full rounded-2xl bg-zinc-100 dark:bg-zinc-800 object-cover ${media.length === 3 && index === 2 ? 'col-span-2' : ''}`} /> : <img key={`${item.url}-${index}`} src={item.url} alt="Post attachment" loading="lazy" className={`max-h-[34rem] w-full rounded-2xl bg-zinc-100 dark:bg-zinc-800 object-cover ${media.length === 3 && index === 2 ? 'col-span-2' : ''}`} />)}</div>}

      {youtube && <a href={youtube} target="_blank" rel="noreferrer" className="mt-4 flex items-center gap-3 rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-950 p-3 text-sm font-medium text-zinc-800 dark:text-zinc-200 transition hover:bg-zinc-100 dark:bg-zinc-800"><Youtube className="h-5 w-5 shrink-0" />Open YouTube video<span className="ml-auto text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">↗</span></a>}

      {post.post_disable_comments === 'true' && <p className="mt-4 text-xs text-zinc-500 dark:text-zinc-400">Replies are disabled on this post.</p>}
      <PostActionBar
        postId={post.id}
        hideLikes={likesHidden}
        hideReplyCount={commentsHidden}
        repliesDisabled={post.post_disable_comments === 'true'}
        state={interaction}
        busy={actionBusy}
        onLike={() => onLike(post)}
        onReply={() => onOpenPost(post)}
        onReshare={() => onReshare(post)}
        onSave={() => onSave(post)}
        onShare={() => onShare(post)}
      />
    </article>
  );
};

interface PostDetailPanelProps {
  postId: string;
  initialPost?: FeedPost;
  user: UserProfile;
  interactions: Record<string, PostInteractionState>;
  busyPostActions: Record<string, boolean>;
  actionError: string | null;
  onBack: () => void;
  onLike: (post: FeedPost) => void;
  onReshare: (post: FeedPost) => void;
  onSave: (post: FeedPost) => void;
  onShare: (post: FeedPost) => void;
  onLoadInteractions: (posts: FeedPost[]) => Promise<void>;
  onReplyCreated: (parentPostId: string, reply: FeedPost) => void;
}

export const PostDetailPanel: React.FC<PostDetailPanelProps> = ({ postId, initialPost, user, interactions, busyPostActions, actionError, onBack, onLike, onReshare, onSave, onShare, onLoadInteractions, onReplyCreated }) => {
  const [rootPost, setRootPost] = useState<FeedPost | null>(initialPost || null);
  const [replies, setReplies] = useState<FeedPost[]>([]);
  const [threadPeople, setThreadPeople] = useState<Record<string, FeedPerson>>({});
  const [replyTargetId, setReplyTargetId] = useState(postId);
  const [draft, setDraft] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mediaWarning, setMediaWarning] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setIsLoading(true); setError(null); setMediaWarning(null);
      try {
        let selected = initialPost;
        if (!selected || selected.id !== postId) {
          const { data, error: selectedError } = await supabase.from('posts').select(FEED_POST_FIELDS).eq('id', postId).or('is_deleted.is.null,is_deleted.eq.false').maybeSingle();
          if (selectedError) throw selectedError;
          if (!data) throw new Error('This post is unavailable or you do not have permission to view it.');
          selected = data as FeedPost;
        }
        const rootId = selected.root_post_id || selected.id;
        const [rootResult, threadResult, directResult] = await Promise.all([
          supabase.from('posts').select(FEED_POST_FIELDS).eq('id', rootId).maybeSingle(),
          supabase.from('posts').select(FEED_POST_FIELDS).eq('root_post_id', rootId).or('is_deleted.is.null,is_deleted.eq.false').order('timestamp', { ascending: true }).limit(100),
          supabase.from('posts').select(FEED_POST_FIELDS).eq('in_reply_to_post_id', rootId).or('is_deleted.is.null,is_deleted.eq.false').order('timestamp', { ascending: true }).limit(100),
        ]);
        if (rootResult.error) throw rootResult.error;
        if (threadResult.error) throw threadResult.error;
        if (directResult.error) throw directResult.error;
        const root = (rootResult.data as FeedPost | null) || selected;
        const uniqueRows = new Map<string, FeedPost>();
        for (const row of [...((threadResult.data || []) as FeedPost[]), ...((directResult.data || []) as FeedPost[]), selected]) {
          if (row.id !== root.id && row.is_deleted !== true) uniqueRows.set(row.id, row);
        }
        let orderedReplies = [...uniqueRows.values()].sort((a, b) => Number(a.timestamp || 0) - Number(b.timestamp || 0));
        let visibleRows = [root, ...orderedReplies];
        try { visibleRows = await signPrivatePostMedia(visibleRows); }
        catch { if (!cancelled) setMediaWarning('Some post attachments could not be opened.'); }
        const authorIds = [...new Set(visibleRows.map(row => row.author_uid))];
        const { data: profileRows, error: profileError } = await supabase.from('users').select('uid,username,display_name,avatar,verify,bio,followers_count,following_count,posts_count').in('uid', authorIds);
        if (profileError) throw profileError;
        const authorMap = Object.fromEntries(((profileRows || []) as FeedPerson[]).map(person => [person.uid, person]));
        orderedReplies = visibleRows.slice(1);
        if (!cancelled) {
          setRootPost(visibleRows[0]);
          setReplies(orderedReplies);
          setThreadPeople(authorMap);
          setReplyTargetId(selected.id);
          void onLoadInteractions(visibleRows);
        }
      } catch (error) {
        if (!cancelled) setError(error instanceof Error ? error.message : 'Could not load this post.');
      } finally { if (!cancelled) setIsLoading(false); }
    };
    void load();
    return () => { cancelled = true; };
  }, [postId, user.id, initialPost, onLoadInteractions]);

  const allThreadPosts = rootPost ? [rootPost, ...replies] : [];
  const replyTarget = allThreadPosts.find(post => post.id === replyTargetId) || rootPost;
  const replyTargetAuthor = replyTarget ? threadPeople[replyTarget.author_uid] : undefined;
  const replyTargetUsername = replyTargetAuthor?.username || (replyTarget ? parseReplyUsernames(replyTarget.reply_to_usernames)[0] : null);

  const submitReply = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const text = draft.trim();
    if (!text || !rootPost || !replyTarget || isSending) return;
    if (replyTarget.post_disable_comments === 'true') { setError('Replies are disabled for this post.'); return; }
    setIsSending(true); setError(null);
    try {
      const now = new Date();
      const { data, error: insertError } = await supabase.from('posts').insert({
        author_uid: user.id,
        post_text: text,
        post_image: null,
        media_items: [],
        post_type: 'TEXT',
        post_visibility: replyTarget.post_visibility || rootPost.post_visibility || 'public',
        timestamp: now.getTime(),
        publish_date: now.toISOString(),
        is_deleted: false,
        is_edited: false,
        has_poll: false,
        poll_options: [],
        in_reply_to_post_id: replyTarget.id,
        root_post_id: rootPost.root_post_id || rootPost.id,
        reply_to_usernames: replyTargetAuthor?.username ? [replyTargetAuthor.username] : [],
      }).select(FEED_POST_FIELDS).single();
      if (insertError) throw insertError;
      let savedReply = data as FeedPost;
      try { [savedReply] = await signPrivatePostMedia([savedReply]); } catch { /* Text reply remains usable if signing fails. */ }
      setReplies(current => [...current, savedReply]);
      setDraft(''); setReplyTargetId(rootPost.id);
      void onLoadInteractions([savedReply]);
      onReplyCreated(replyTarget.id, savedReply);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Could not publish your reply.');
    } finally { setIsSending(false); }
  };

  const getInteraction = (post: FeedPost) => interactions[post.id] || defaultInteraction(post);
  const renderPost = (post: FeedPost) => {
    const parent = allThreadPosts.find(item => item.id === post.in_reply_to_post_id);
    return <FeedPostCard key={post.id} post={post} author={threadPeople[post.author_uid]} replyTarget={parent ? threadPeople[parent.author_uid] : undefined} currentUser={user} interaction={getInteraction(post)} actionBusy={Boolean(busyPostActions[post.id])} onOpenPost={target => setReplyTargetId(target.id)} onLike={onLike} onReshare={onReshare} onSave={onSave} onShare={onShare} />;
  };

  return (
    <div className="flex h-[100dvh] min-h-0 flex-col overflow-hidden bg-[#f7f7f8] text-zinc-950 dark:bg-zinc-950 dark:text-zinc-100">
      <header className="sticky top-0 z-30 shrink-0 border-b border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
        <div className="mx-auto flex h-16 max-w-3xl items-center gap-3 px-4 sm:px-6"><Button type="button" variant="outline" onClick={onBack} aria-label="Back to feed" className="h-10 w-10 rounded-full border-zinc-300 p-0 text-zinc-700 shadow-none dark:border-zinc-700 dark:text-zinc-300"><ArrowLeft className="h-4 w-4" /></Button><div><p className="text-[10px] font-bold uppercase tracking-[0.16em] text-zinc-500 dark:text-zinc-400">Conversation</p><h1 className="text-base font-semibold text-zinc-950 dark:text-zinc-100">Post details</h1></div></div>
      </header>
      <main className="mx-auto flex min-h-0 w-full max-w-3xl flex-1 flex-col space-y-4 overflow-y-auto overscroll-contain px-4 py-6 sm:px-6 sm:py-8">
        {actionError && <p role="status" className="rounded-2xl border border-zinc-200 bg-white px-4 py-3 text-sm text-zinc-700 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300">{actionError}</p>}
        {mediaWarning && <p role="status" className="rounded-2xl border border-zinc-200 bg-white px-4 py-3 text-sm text-zinc-600 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-400">{mediaWarning}</p>}
        {error && !rootPost && <div role="alert" className="rounded-2xl border border-zinc-300 bg-white px-5 py-4 text-sm text-zinc-700 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-300">{error}</div>}
        {isLoading && !rootPost ? <div className="h-56 animate-pulse rounded-3xl border border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-900" /> : rootPost ? <>
          {renderPost(rootPost)}
          <div className="flex items-center justify-between px-1"><h2 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">Replies</h2><span className="text-xs text-zinc-500 dark:text-zinc-400">{replies.length}</span></div>
          {replies.length ? <div className="space-y-4">{replies.map(renderPost)}</div> : <div className="rounded-3xl border border-zinc-200 bg-white px-6 py-10 text-center dark:border-zinc-800 dark:bg-zinc-900"><p className="font-medium text-zinc-900 dark:text-zinc-100">No replies yet</p><p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">Start the conversation below.</p></div>}
        </> : null}
      </main>
      {rootPost && <div data-testid="post-reply-dock" className="z-30 shrink-0 border-t border-zinc-200 bg-white px-4 pt-3 pb-[max(env(safe-area-inset-bottom),0.75rem)] dark:border-zinc-800 dark:bg-zinc-900 sm:px-6">
        <div className="mx-auto w-full max-w-3xl">
          {error && <div role="alert" className="mb-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-800 dark:border-rose-900 dark:bg-rose-950/40 dark:text-rose-200">{error}</div>}
          <form onSubmit={submitReply} className="rounded-2xl border border-zinc-200 bg-zinc-50 p-3 dark:border-zinc-700 dark:bg-zinc-950">
            <div className="mb-2 flex items-center justify-between gap-3"><p className="truncate text-xs text-zinc-500 dark:text-zinc-400">Replying to <span className="font-semibold text-zinc-800 dark:text-zinc-200">@{replyTargetUsername || 'post'}</span></p>{replyTarget?.id !== rootPost.id && <button type="button" onClick={() => setReplyTargetId(rootPost.id)} className="shrink-0 text-xs font-medium text-zinc-500 underline underline-offset-2 hover:text-zinc-950 dark:text-zinc-400 dark:hover:text-zinc-100">Reply to thread</button>}</div>
            <label htmlFor="post-detail-reply" className="sr-only">Write a reply</label><textarea id="post-detail-reply" value={draft} onChange={event => { setDraft(event.target.value); if (error) setError(null); }} maxLength={5000} rows={2} placeholder="Write a reply…" className="max-h-32 min-h-12 w-full resize-y rounded-xl border border-zinc-200 bg-white px-3 py-2.5 text-sm text-zinc-900 outline-none placeholder:text-zinc-400 focus:border-zinc-400 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100" />
            <div className="mt-2 flex items-center justify-between gap-3"><span className="text-xs text-zinc-500 dark:text-zinc-400">{draft.length}/5000</span><Button type="submit" disabled={!draft.trim() || isSending || replyTarget?.post_disable_comments === 'true'} className="rounded-full bg-zinc-950 px-5 text-sm font-semibold text-white shadow-none hover:bg-zinc-800 disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-950 dark:hover:bg-zinc-200">{isSending ? 'Replying…' : 'Reply'}</Button></div>
          </form>
        </div>
      </div>}
    </div>
  );
};

export const HomeScreen: React.FC<HomeScreenProps> = ({ user, onLogout, onCreatePost }) => {
  const [activeView, setActiveView] = useState<'home' | 'profile' | 'messages' | 'settings' | 'post-detail'>('home');
  const [isChatThreadOpen, setIsChatThreadOpen] = useState(false);
  const [feedMode, setFeedMode] = useState<'for-you' | 'following'>('for-you');
  const [posts, setPosts] = useState<FeedPost[]>([]);
  const [stories, setStories] = useState<FeedStory[]>([]);
  const [people, setPeople] = useState<Record<string, FeedPerson>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasMorePosts, setHasMorePosts] = useState(true);
  const [loadMoreError, setLoadMoreError] = useState<string | null>(null);
  const [feedError, setFeedError] = useState<string | null>(null);
  const [postInteractions, setPostInteractions] = useState<Record<string, PostInteractionState>>({});
  const [replyTargets, setReplyTargets] = useState<Record<string, FeedPerson>>({});
  const [busyPostActions, setBusyPostActions] = useState<Record<string, boolean>>({});
  const [postActionError, setPostActionError] = useState<string | null>(null);
  const [selectedPostId, setSelectedPostId] = useState<string | null>(null);
  const [profileWarning, setProfileWarning] = useState<string | null>(null);
  const [storyWarning, setStoryWarning] = useState<string | null>(null);
  const [mediaWarning, setMediaWarning] = useState<string | null>(null);

  const loadPostInteractions = useCallback(async (rows: FeedPost[], append = true) => {
    const postIds = [...new Set(rows.map(post => post.id))];
    if (!postIds.length) return;
    try {
      const [likesResult, resharesResult, favoritesResult, repliesResult] = await Promise.all([
        supabase.from('post_likes').select('post_id,user_id').in('post_id', postIds),
        supabase.from('reshares').select('post_id,user_id').in('post_id', postIds),
        supabase.from('favorites').select('post_id,user_id').eq('user_id', user.id).in('post_id', postIds),
        supabase.from('posts').select('in_reply_to_post_id').in('in_reply_to_post_id', postIds).or('is_deleted.is.null,is_deleted.eq.false'),
      ]);
      const failed = [likesResult.error, resharesResult.error, favoritesResult.error, repliesResult.error].find(Boolean);
      if (failed) throw failed;
      const next = buildPostInteractionStates(
        rows,
        (likesResult.data || []) as { post_id: string; user_id: string }[],
        (resharesResult.data || []) as { post_id: string; user_id: string }[],
        (favoritesResult.data || []) as { post_id: string; user_id: string }[],
        (repliesResult.data || []) as { in_reply_to_post_id: string | null }[],
        user.id,
      );
      setPostInteractions(current => append ? { ...current, ...next } : next);
      setPostActionError(null);
    } catch {
      const fallback = Object.fromEntries(rows.map(post => [post.id, defaultInteraction(post)]));
      setPostInteractions(current => append ? { ...current, ...fallback } : fallback);
      setPostActionError('Post interaction counts could not be refreshed. Try again shortly.');
    }
  }, [user.id]);

  const loadFeed = useCallback(async (offset = 0, append = false) => {
    if (append) { setIsLoadingMore(true); setLoadMoreError(null); }
    else {
      setIsLoading(true);
      setFeedError(null);
      setProfileWarning(null);
      setStoryWarning(null);
      setMediaWarning(null);
      setLoadMoreError(null);
    }
    try {
      let followedIds: string[] | null = null;
      if (feedMode === 'following') {
        const { data, error } = await supabase.from('follows').select('following_id').eq('follower_id', user.id);
        if (error) throw error;
        followedIds = (data || []).map(row => String(row.following_id));
      }

      const storyRequest = supabase
        .from('stories')
        .select('id,user_id,content,media_url,thumbnail_url,media_type,created_at,expires_at,is_active')
        .eq('is_active', true)
        .order('created_at', { ascending: false })
        .limit(30);

      let feedRows: FeedPost[] = [];
      if (feedMode !== 'following' || (followedIds && followedIds.length > 0)) {
        let query = supabase
          .from('posts')
          .select(FEED_POST_FIELDS)
          .or('is_deleted.is.null,is_deleted.eq.false')
          .order('timestamp', { ascending: false })
          .range(offset, offset + PAGE_SIZE - 1);
        if (followedIds) query = query.in('author_uid', followedIds);
        const { data, error } = await query;
        if (error) throw error;
        feedRows = ((data || []) as FeedPost[]).filter(post => post.is_deleted !== true);
      }

      const privateMediaPaths = new Set<string>();
      const addPrivatePath = (value: unknown) => {
        if (typeof value === 'string' && value && !/^https?:\/\//i.test(value) && !value.startsWith('/')) privateMediaPaths.add(value);
      };
      for (const post of feedRows) {
        addPrivatePath(post.post_image);
        if (Array.isArray(post.media_items)) for (const entry of post.media_items) {
          if (typeof entry === 'string') addPrivatePath(entry);
          else if (entry && typeof entry === 'object') {
            const item = entry as Record<string, unknown>;
            if (!item.bucket || item.bucket === 'posts') addPrivatePath(item.url || item.media_url || item.uri || item.src);
          }
        }
      }
      if (privateMediaPaths.size) {
        const { data: signed, error: signError } = await supabase.storage.from('posts').createSignedUrls([...privateMediaPaths], 60 * 60);
        if (signError) setMediaWarning('Some post media could not be opened.');
        else {
          const signedUrls = new Map((signed || []).flatMap(item => item.signedUrl && item.path ? [[item.path, item.signedUrl] as const] : []));
          const resolveUrl = (value: unknown) => typeof value === 'string' ? signedUrls.get(value) || value : value;
          feedRows = feedRows.map(post => ({
            ...post,
            post_image: typeof post.post_image === 'string' ? String(resolveUrl(post.post_image)) : post.post_image,
            media_items: Array.isArray(post.media_items) ? post.media_items.map(entry => {
              if (typeof entry === 'string') return resolveUrl(entry);
              if (entry && typeof entry === 'object') {
                const item = entry as Record<string, unknown>;
                return { ...item, url: resolveUrl(item.url || item.media_url || item.uri || item.src) };
              }
              return entry;
            }) : post.media_items,
          }));
        }
      }

      const replyParentIds = [...new Set(feedRows.map(post => post.in_reply_to_post_id).filter((id): id is string => Boolean(id)))];
      const parentAuthorByPost: Record<string, string> = {};
      if (replyParentIds.length) {
        const { data: parentRows, error: parentError } = await supabase.from('posts').select('id,author_uid').in('id', replyParentIds);
        if (parentError) setProfileWarning('Some reply context could not be loaded.');
        else for (const row of parentRows || []) parentAuthorByPost[String(row.id)] = String(row.author_uid);
      }

      const { data: storyRows, error: storiesError } = await storyRequest;
      if (storiesError) setStoryWarning('Stories are temporarily unavailable.');
      const now = Date.now();
      const visibleStories = ((storyRows || []) as FeedStory[]).filter(story => !story.expires_at || Date.parse(story.expires_at) > now);
      const profileIds = [...new Set([user.id, ...feedRows.map(post => post.author_uid), ...visibleStories.map(story => story.user_id), ...Object.values(parentAuthorByPost)])];
      let personMap: Record<string, FeedPerson> = {};
      if (profileIds.length > 0) {
        const { data: profileRows, error: profileError } = await supabase
          .from('users')
          .select('uid,username,display_name,avatar,verify,bio,followers_count,following_count,posts_count')
          .in('uid', profileIds);
        if (profileError) setProfileWarning('Some profile details could not be loaded.');
        else personMap = Object.fromEntries(((profileRows || []) as FeedPerson[]).map(person => [person.uid, person]));
      }
      const targets = Object.fromEntries(feedRows.flatMap(post => {
        const parentAuthor = post.in_reply_to_post_id ? personMap[parentAuthorByPost[post.in_reply_to_post_id]] : undefined;
        return parentAuthor ? [[post.id, parentAuthor] as const] : [];
      }));
      await loadPostInteractions(feedRows, append);
      setPosts(current => {
        if (!append) return feedRows;
        const existingIds = new Set(current.map(post => post.id));
        return [...current, ...feedRows.filter(post => !existingIds.has(post.id))];
      });
      setStories(visibleStories);
      setPeople(current => append ? { ...current, ...personMap } : personMap);
      setReplyTargets(current => append ? { ...current, ...targets } : targets);
      setHasMorePosts(feedRows.length === PAGE_SIZE);
    } catch (error) {
      if (append) setLoadMoreError(error instanceof Error ? error.message : 'Could not load more posts.');
      else setFeedError(error instanceof Error ? error.message : 'Could not load the feed. Please try again.');
    } finally {
      if (append) setIsLoadingMore(false);
      else setIsLoading(false);
    }
  }, [feedMode, user.id, loadPostInteractions]);

  useEffect(() => { void loadFeed(); }, [loadFeed]);

  useEffect(() => {
    const openSharedPost = () => {
      const postId = new URLSearchParams(window.location.hash.replace(/^#/, '')).get('post');
      if (postId) { setSelectedPostId(postId); setActiveView('post-detail'); }
    };
    openSharedPost();
    window.addEventListener('hashchange', openSharedPost);
    return () => window.removeEventListener('hashchange', openSharedPost);
  }, []);

  const openPostDetail = (post: FeedPost) => {
    setSelectedPostId(post.id);
    setActiveView('post-detail');
    const url = new URL(window.location.href);
    url.hash = 'post=' + encodeURIComponent(post.id);
    window.history.replaceState({}, '', url.toString());
  };

  const closePostDetail = () => {
    setSelectedPostId(null);
    setActiveView('home');
    if (new URLSearchParams(window.location.hash.replace(/^#/, '')).has('post')) {
      const url = new URL(window.location.href); url.hash = ''; window.history.replaceState({}, '', url.toString());
    }
  };

  const togglePostAction = async (post: FeedPost, action: 'like' | 'reshare' | 'save') => {
    if (busyPostActions[post.id]) return;
    const previous = postInteractions[post.id] || defaultInteraction(post);
    const next = { ...previous };
    if (action === 'like') { next.isLiked = !previous.isLiked; next.likesCount = Math.max(0, previous.likesCount + (next.isLiked ? 1 : -1)); }
    if (action === 'reshare') { next.isReshared = !previous.isReshared; next.resharesCount = Math.max(0, previous.resharesCount + (next.isReshared ? 1 : -1)); }
    if (action === 'save') next.isSaved = !previous.isSaved;
    setBusyPostActions(current => ({ ...current, [post.id]: true }));
    setPostInteractions(current => ({ ...current, [post.id]: next }));
    setPostActionError(null);
    try {
      const table = action === 'like' ? 'post_likes' : action === 'reshare' ? 'reshares' : 'favorites';
      const shouldDelete = action === 'like' ? previous.isLiked : action === 'reshare' ? previous.isReshared : previous.isSaved;
      let request;
      if (shouldDelete) {
        request = supabase.from(table).delete().eq('post_id', post.id).eq('user_id', user.id);
      } else {
        request = supabase.from(table).insert(action === 'reshare' ? { post_id: post.id, user_id: user.id, comment: null } : { post_id: post.id, user_id: user.id });
      }
      const { error } = await request;
      if (error) throw error;
    } catch (error) {
      setPostInteractions(current => ({ ...current, [post.id]: previous }));
      setPostActionError(error instanceof Error ? error.message : 'Could not update this post action. Try again.');
    } finally {
      setBusyPostActions(current => { const nextBusy = { ...current }; delete nextBusy[post.id]; return nextBusy; });
    }
  };

  const sharePost = async (post: FeedPost) => {
    const url = new URL(window.location.href);
    url.hash = 'post=' + encodeURIComponent(post.id);
    try {
      if (navigator.share) await navigator.share({ title: 'Synapse post', url: url.toString() });
      else if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(url.toString());
      else throw new Error('Sharing is not available in this browser.');
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') return;
      setPostActionError(error instanceof Error ? error.message : 'Could not share this post.');
    }
  };

  const handleReplyCreated = (parentPostId: string, _reply: FeedPost) => {
    setPostInteractions(current => {
      const parent = posts.find(post => post.id === parentPostId);
      const previous = current[parentPostId] || (parent ? defaultInteraction(parent) : { isLiked: false, likesCount: 0, isReshared: false, resharesCount: 0, isSaved: false, repliesCount: 0 });
      return { ...current, [parentPostId]: { ...previous, repliesCount: previous.repliesCount + 1 } };
    });
    void loadFeed(0, false);
  };

  const currentProfile = people[user.id];
  const currentName = currentProfile?.display_name || user.fullName || user.username;
  const currentUsername = currentProfile?.username || user.username;
  const currentAvatar = currentProfile?.avatar || user.avatarUrl;
  const currentBio = currentProfile?.bio || user.bio;
  const profileCounts = [
    { label: 'Posts', value: currentProfile?.posts_count },
    { label: 'Followers', value: currentProfile?.followers_count },
    { label: 'Following', value: currentProfile?.following_count },
  ].filter(item => typeof item.value === 'number');
  const storyLabel = stories.length === 1 ? '1 active story' : `${stories.length} active stories`;

  if (activeView === 'post-detail' && selectedPostId) {
    return <PostDetailPanel
      postId={selectedPostId}
      initialPost={posts.find(post => post.id === selectedPostId)}
      user={user}
      interactions={postInteractions}
      busyPostActions={busyPostActions}
      actionError={postActionError}
      onBack={closePostDetail}
      onLike={post => void togglePostAction(post, 'like')}
      onReshare={post => void togglePostAction(post, 'reshare')}
      onSave={post => void togglePostAction(post, 'save')}
      onShare={post => void sharePost(post)}
      onLoadInteractions={loadPostInteractions}
      onReplyCreated={handleReplyCreated}
    />;
  }

  if (activeView === 'settings') {
    return <SettingsScreen user={user} onClose={() => setActiveView('home')} onLogout={onLogout} onOpenProfile={() => setActiveView('profile')} />;
  }

  return (
    <div className="min-h-screen bg-[#f7f7f8] dark:bg-zinc-950 text-zinc-950 dark:text-zinc-100">
      {!(activeView === 'messages' && isChatThreadOpen) && <header className="sticky top-0 z-30 border-b border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6">
          <div><h1 className="text-lg font-semibold leading-5 text-zinc-950 dark:text-zinc-100">{activeView === 'home' ? 'Home' : activeView === 'profile' ? 'Profile' : 'Messages'}</h1></div>
          <div className="flex items-center gap-2 sm:gap-3">
            <div className="hidden text-right sm:block"><p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">{currentName}</p><p className="text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">@{currentUsername}</p></div>
            <button type="button" onClick={() => setActiveView('messages')} aria-label="Open messages" aria-pressed={activeView === 'messages'} className={`grid h-10 w-10 place-items-center rounded-full transition-colors ${activeView === 'messages' ? 'bg-zinc-950 text-white dark:bg-zinc-100 dark:text-zinc-950' : 'text-zinc-600 dark:text-zinc-400 dark:text-zinc-500 hover:bg-zinc-100 dark:bg-zinc-800 hover:text-zinc-950 dark:text-zinc-100'}`}><MessageCircle className="h-5 w-5" /></button>
            <button type="button" onClick={() => setActiveView('settings')} aria-label="Open settings" aria-pressed={false} className="grid h-10 w-10 place-items-center rounded-full text-zinc-600 transition-colors hover:bg-zinc-100 hover:text-zinc-950 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-100"><Settings2 className="h-5 w-5" /></button>
            <button type="button" onClick={() => setActiveView('profile')} aria-label="Open profile" aria-pressed={activeView === 'profile'} className={`rounded-full p-0.5 transition-shadow ${activeView === 'profile' ? 'ring-2 ring-zinc-950 ring-offset-2' : 'hover:ring-2 hover:ring-zinc-300 dark:ring-zinc-600 hover:ring-offset-1'}`}><PersonAvatar name={currentName} image={currentAvatar} size="sm" /></button>
            <Button type="button" variant="outline" onClick={onLogout} className="h-9 rounded-full border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 text-xs font-medium text-zinc-700 dark:text-zinc-300 shadow-none hover:bg-zinc-100 dark:bg-zinc-800 hover:text-zinc-950 dark:text-zinc-100"><LogOut className="mr-2 h-3.5 w-3.5" /> Sign out</Button>
          </div>
        </div>
      </header>}

      <main className={`mx-auto ${activeView === 'messages' && isChatThreadOpen ? 'w-full max-w-none px-0 py-0' : 'max-w-7xl px-4 py-6 sm:px-6 sm:py-8'}`}>
        {activeView === 'home' ? (
          <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_290px] lg:gap-8">
        <section className="mx-auto w-full max-w-3xl space-y-5">
          <div className="flex items-end justify-between gap-4 px-1">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">Your community</p>
              <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-400 dark:text-zinc-500">A fresh look at what people are sharing.</p>
            </div>
            <Button type="button" variant="ghost" onClick={() => void loadFeed()} disabled={isLoading} aria-label="Refresh feed" className="h-10 w-10 rounded-full p-0 text-zinc-600 dark:text-zinc-400 dark:text-zinc-500 hover:bg-zinc-200 dark:bg-zinc-800 hover:text-zinc-950 dark:text-zinc-100">
              <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
            </Button>
          </div>

          <div className="flex w-full items-center gap-2 rounded-full border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-1.5 shadow-sm">
            <button type="button" onClick={() => setFeedMode('for-you')} className={`flex-1 rounded-full px-4 py-2.5 text-sm font-semibold transition-colors ${feedMode === 'for-you' ? 'bg-zinc-950 text-white dark:bg-zinc-100 dark:text-zinc-950' : 'text-zinc-600 dark:text-zinc-400 dark:text-zinc-500 hover:bg-zinc-100 dark:bg-zinc-800 hover:text-zinc-950 dark:text-zinc-100'}`}>For you</button>
            <button type="button" onClick={() => setFeedMode('following')} className={`flex-1 rounded-full px-4 py-2.5 text-sm font-semibold transition-colors ${feedMode === 'following' ? 'bg-zinc-950 text-white dark:bg-zinc-100 dark:text-zinc-950' : 'text-zinc-600 dark:text-zinc-400 dark:text-zinc-500 hover:bg-zinc-100 dark:bg-zinc-800 hover:text-zinc-950 dark:text-zinc-100'}`}>Following</button>
          </div>

          <div className="rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-5 shadow-[0_2px_14px_-10px_rgba(0,0,0,0.16)] sm:p-6">
            <div className="flex items-center gap-3">
              <PersonAvatar name={currentName} image={currentAvatar} />
              <button type="button" onClick={onCreatePost} className="min-h-12 flex-1 rounded-full border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-950 px-5 text-left text-sm text-zinc-500 dark:text-zinc-400 dark:text-zinc-500 transition hover:border-zinc-300 dark:border-zinc-700 hover:bg-white dark:bg-zinc-900 hover:text-zinc-800 dark:text-zinc-200">What would you like to share?</button>
            </div>
            <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-zinc-100 dark:border-zinc-800 pt-4">
              <span className="inline-flex items-center gap-2 text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500"><Users className="h-4 w-4" /> Your audience and post options are up to you</span>
              <Button type="button" onClick={onCreatePost} className="h-10 rounded-full bg-zinc-950 px-5 text-sm font-semibold text-white shadow-none hover:bg-zinc-800"><Send className="mr-2 h-4 w-4" />Create post</Button>
            </div>
          </div>

          <section className="rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-5 shadow-[0_2px_14px_-10px_rgba(0,0,0,0.12)] sm:p-6" aria-labelledby="stories-heading">
            <div className="mb-4 flex items-center justify-between gap-4">
              <div><h2 id="stories-heading" className="text-base font-semibold text-zinc-950 dark:text-zinc-100">Stories</h2><p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">{stories.length ? storyLabel : 'Stories from your community will appear here.'}</p></div>
              <Sparkles className="h-4 w-4 text-zinc-400 dark:text-zinc-500" />
            </div>
            {storyWarning ? <p role="status" className="text-sm text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">{storyWarning}</p> : stories.length > 0 ? (
              <div className="flex gap-4 overflow-x-auto pb-1">
                {stories.map(story => {
                  const author = people[story.user_id];
                  const name = author?.display_name || author?.username || 'Synapse member';
                  const storyImage = safeMediaUrl(story.thumbnail_url || story.media_url);
                  return <div key={story.id} className="flex w-16 shrink-0 flex-col items-center gap-2 text-center">
                    <span className="grid h-14 w-14 place-items-center rounded-full border-2 border-zinc-900 p-0.5">
                      <span className="grid h-full w-full place-items-center overflow-hidden rounded-full bg-zinc-100 dark:bg-zinc-800 text-xs font-semibold text-zinc-700 dark:text-zinc-300">{storyImage ? <img src={storyImage} alt="" className="h-full w-full object-cover" loading="lazy" /> : initials(name)}</span>
                    </span>
                    <span className="w-full truncate text-[11px] text-zinc-600 dark:text-zinc-400 dark:text-zinc-500">{author?.username ? `@${author.username}` : 'Member'}</span>
                  </div>;
                })}
              </div>
            ) : <div className="rounded-2xl bg-zinc-50 dark:bg-zinc-950 px-4 py-5 text-sm text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">No active stories right now.</div>}
          </section>

          {profileWarning && <p role="status" className="rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-4 py-3 text-sm text-zinc-600 dark:text-zinc-400 dark:text-zinc-500">{profileWarning}</p>}
          {mediaWarning && <p role="status" className="rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-4 py-3 text-sm text-zinc-600 dark:text-zinc-400 dark:text-zinc-500">{mediaWarning}</p>}
          {feedError && (
            <div role="alert" className="rounded-3xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-900 p-6">
              <div className="flex items-start gap-3"><AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-zinc-700 dark:text-zinc-300" /><div className="flex-1"><p className="font-semibold text-zinc-900 dark:text-zinc-100">We couldn’t load your feed</p><p className="mt-1 text-sm text-zinc-600 dark:text-zinc-400 dark:text-zinc-500">{feedError}</p><Button type="button" variant="outline" onClick={() => void loadFeed()} className="mt-4 h-9 rounded-full border-zinc-300 dark:border-zinc-700 px-4 text-sm text-zinc-800 dark:text-zinc-200 shadow-none">Try again</Button></div></div>
            </div>
          )}

          {isLoading ? (
            <div className="space-y-4" aria-label="Loading feed"><div className="h-44 animate-pulse rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900" /><div className="h-64 animate-pulse rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900" /></div>
          ) : !feedError && posts.length === 0 ? (
            <div className="rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-6 py-12 text-center">
              <div className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300"><Image className="h-5 w-5" /></div>
              <h2 className="mt-4 text-lg font-semibold text-zinc-950 dark:text-zinc-100">{feedMode === 'following' ? 'Your following feed is quiet' : 'No posts yet'}</h2>
              <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">{feedMode === 'following' ? 'Posts from people you follow will show up here.' : 'Be the first to share something with your community.'}</p>
            </div>
          ) : !feedError ? (
            <div className="space-y-4">
              <div className="space-y-4">{posts.map(post => <FeedPostCard
                key={post.id}
                post={post}
                author={people[post.author_uid]}
                replyTarget={replyTargets[post.id]}
                currentUser={user}
                interaction={postInteractions[post.id] || defaultInteraction(post)}
                actionBusy={Boolean(busyPostActions[post.id])}
                onOpenPost={openPostDetail}
                onLike={item => void togglePostAction(item, 'like')}
                onReshare={item => void togglePostAction(item, 'reshare')}
                onSave={item => void togglePostAction(item, 'save')}
                onShare={item => void sharePost(item)}
              />)}</div>
              {postActionError && <p role="status" className="rounded-2xl border border-zinc-200 bg-white px-4 py-3 text-sm text-zinc-700 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300">{postActionError}</p>}
              {loadMoreError && <div role="alert" className="rounded-2xl border border-zinc-200 bg-white px-4 py-3 text-sm text-zinc-700 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300">{loadMoreError}<button type="button" onClick={() => void loadFeed(posts.length, true)} className="ml-2 font-semibold underline underline-offset-2">Try again</button></div>}
              {hasMorePosts ? <div className="flex justify-center"><Button type="button" variant="outline" onClick={() => void loadFeed(posts.length, true)} disabled={isLoadingMore} className="h-11 rounded-full border-zinc-300 px-6 text-sm font-semibold text-zinc-800 shadow-none dark:border-zinc-700 dark:text-zinc-200">{isLoadingMore ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Loading more…</> : 'Load more posts'}</Button></div> : posts.length > 0 ? <p className="py-3 text-center text-xs text-zinc-400 dark:text-zinc-500">You’re all caught up.</p> : null}
            </div>
          ) : null}
        </section>

        <aside className="hidden space-y-4 lg:block">
          <div className="rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-6 shadow-[0_2px_14px_-10px_rgba(0,0,0,0.12)]">
            <div className="flex items-center gap-3"><PersonAvatar name={currentName} image={currentAvatar} size="lg" /><div className="min-w-0"><p className="truncate font-semibold text-zinc-950 dark:text-zinc-100">{currentName}</p><p className="truncate text-sm text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">@{currentUsername}</p></div></div>
            {currentBio && <p className="mt-4 text-sm leading-6 text-zinc-600 dark:text-zinc-400 dark:text-zinc-500">{currentBio}</p>}
            {profileCounts.length > 0 && <div className="mt-5 grid grid-cols-3 gap-2 border-t border-zinc-100 dark:border-zinc-800 pt-4">{profileCounts.map(item => <div key={item.label}><p className="text-xs text-zinc-500 dark:text-zinc-400 dark:text-zinc-500">{item.label}</p><p className="mt-1 text-base font-semibold text-zinc-950 dark:text-zinc-100">{item.value}</p></div>)}</div>}
          </div>
          <div className="rounded-3xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-6">
            <h2 className="text-sm font-semibold text-zinc-950 dark:text-zinc-100">A place for your people</h2>
            <p className="mt-2 text-sm leading-6 text-zinc-600 dark:text-zinc-400 dark:text-zinc-500">Share updates, follow the voices you care about, and keep the conversation moving.</p>
          </div>
          <p className="px-2 text-xs leading-5 text-zinc-400 dark:text-zinc-500">Synapse Social · Community feed</p>
        </aside>
          </div>
        ) : activeView === 'profile' ? (
          <ProfilePanel user={user} onBack={() => setActiveView('home')} onProfileUpdated={updated => setPeople(current => ({ ...current, [updated.uid]: updated }))} />
        ) : (
          <MessagesPanel user={user} onBack={() => setActiveView('home')} onThreadOpenChange={setIsChatThreadOpen} />
        )}
      </main>
    </div>
  );
};
