import React, { useEffect, useRef, useState } from 'react';
import type { LucideIcon } from 'lucide-react';
import {
  ArrowLeft,
  AtSign,
  CalendarClock,
  Check,
  CheckCircle2,
  Clock3,
  EyeOff,
  ImagePlus,
  Loader2,
  MapPin,
  MessageCircleOff,
  Plus,
  Settings2,
  Smile,
  Sparkles,
  Trash2,
  UserPlus,
  Video,
  X,
  Youtube,
} from 'lucide-react';
import type { UserProfile } from '@/types';
import { supabase } from '@/lib/supabase';

type Audience = 'public' | 'followers' | 'private';
type ToolKey = 'media' | 'poll' | 'tagged' | 'feeling' | 'location' | 'youtube' | 'schedule' | 'settings' | 'thread';
type PostToolState = Record<ToolKey, boolean>;
type MediaAttachment = { id: string; file: File; previewUrl: string; kind: 'image' | 'video' };
type TaggedPerson = { uid: string; username: string | null; display_name: string | null; avatar: string | null };
type LocationChoice = { name: string; address: string; latitude: number | null; longitude: number | null; placeId: string | null };
type PlaceResult = { place_id?: number | string; name?: string; display_name?: string; lat?: string; lon?: string; address?: Record<string, string> };

interface CreatePostScreenProps {
  user: UserProfile;
  onClose: () => void;
  onPostCreated?: () => void;
}

const MEDIA_BUCKET = 'posts';
const MAX_MEDIA_FILES = 8;
const MAX_MEDIA_BYTES = 10 * 1024 * 1024;
const ALLOWED_MEDIA = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'video/mp4', 'video/quicktime']);
const POLL_DURATIONS = [
  { value: 1, label: '1 hour' },
  { value: 6, label: '6 hours' },
  { value: 24, label: '1 day' },
  { value: 72, label: '3 days' },
  { value: 168, label: '7 days' },
];
const TOOL_ITEMS: { key: ToolKey; label: string; icon: LucideIcon }[] = [
  { key: 'media', label: 'Media', icon: ImagePlus },
  { key: 'poll', label: 'Poll', icon: Check },
  { key: 'tagged', label: 'Tag people', icon: UserPlus },
  { key: 'feeling', label: 'Feeling / activity', icon: Smile },
  { key: 'location', label: 'Location', icon: MapPin },
  { key: 'youtube', label: 'YouTube', icon: Youtube },
  { key: 'schedule', label: 'Schedule', icon: CalendarClock },
  { key: 'settings', label: 'Post settings', icon: Settings2 },
  { key: 'thread', label: 'Add to thread', icon: AtSign },
];
const TEXT_BACKGROUNDS = [
  { value: 'none', label: 'Plain', color: '#ffffff', text: '#18181b' },
  { value: '#f4f4f5', label: 'Mist', color: '#f4f4f5', text: '#18181b' },
  { value: '#e4e4e7', label: 'Stone', color: '#e4e4e7', text: '#18181b' },
  { value: '#18181b', label: 'Ink', color: '#18181b', text: '#ffffff' },
];

const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0] || '').join('').toUpperCase() || 'S';
const getDraftKey = (userId: string) => `synapse:create-post:draft:${userId}`;
const loadDraftText = (key: string) => {
  try { return window.localStorage.getItem(key) || ''; } catch { return ''; }
};
const localDateTime = (date: Date) => {
  const adjusted = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return adjusted.toISOString().slice(0, 16);
};
const safeFileName = (name: string) => name.replace(/[^a-zA-Z0-9._-]+/g, '-').slice(-80) || 'upload';
const formatBytes = (bytes: number) => bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
const isYouTubeUrl = (value: string) => {
  try {
    const url = new URL(value);
    return ['youtube.com', 'www.youtube.com', 'm.youtube.com', 'youtu.be', 'www.youtu.be'].includes(url.hostname.toLowerCase());
  } catch { return false; }
};
const profileLabel = (person: TaggedPerson) => person.display_name || person.username || 'Synapse member';

const SectionCard: React.FC<{ title: string; description?: string; icon: React.ReactNode; onRemove?: () => void; children: React.ReactNode }> = ({ title, description, icon, onRemove, children }) => (
  <section className="rounded-2xl border border-zinc-200 bg-white p-4 sm:p-5">
    <div className="mb-4 flex items-start gap-3">
      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-zinc-100 text-zinc-700">{icon}</span>
      <div className="min-w-0 flex-1">
        <h2 className="text-sm font-semibold text-zinc-950">{title}</h2>
        {description && <p className="mt-1 text-xs leading-5 text-zinc-500">{description}</p>}
      </div>
      {onRemove && <button type="button" onClick={onRemove} aria-label={`Remove ${title}`} className="rounded-lg p-2 text-zinc-400 transition hover:bg-zinc-100 hover:text-zinc-800"><X className="h-4 w-4" /></button>}
    </div>
    {children}
  </section>
);

const ToggleRow: React.FC<{ title: string; description?: string; checked: boolean; onChange: (checked: boolean) => void }> = ({ title, description, checked, onChange }) => (
  <label className="flex cursor-pointer items-center justify-between gap-4 rounded-xl px-3 py-3 transition hover:bg-zinc-50">
    <span className="min-w-0"><span className="block text-sm font-medium text-zinc-800">{title}</span>{description && <span className="mt-0.5 block text-xs leading-5 text-zinc-500">{description}</span>}</span>
    <input type="checkbox" checked={checked} onChange={event => onChange(event.target.checked)} className="h-4 w-4 shrink-0 accent-zinc-950" />
  </label>
);

export const CreatePostScreen: React.FC<CreatePostScreenProps> = ({ user, onClose, onPostCreated }) => {
  const draftKey = getDraftKey(user.id);
  const [content, setContent] = useState(() => loadDraftText(draftKey));
  const [audience, setAudience] = useState<Audience>('public');
  const [tools, setTools] = useState<PostToolState>({ media: false, poll: false, tagged: false, feeling: false, location: false, youtube: false, schedule: false, settings: false, thread: false });
  const [attachments, setAttachments] = useState<MediaAttachment[]>([]);
  const [textBackground, setTextBackground] = useState('none');
  const [pollQuestion, setPollQuestion] = useState('');
  const [pollOptions, setPollOptions] = useState(['', '']);
  const [pollDuration, setPollDuration] = useState(24);
  const [pollAllowMultiple, setPollAllowMultiple] = useState(false);
  const [tagQuery, setTagQuery] = useState('');
  const [tagResults, setTagResults] = useState<TaggedPerson[]>([]);
  const [taggedPeople, setTaggedPeople] = useState<TaggedPerson[]>([]);
  const [tagLoading, setTagLoading] = useState(false);
  const [tagError, setTagError] = useState<string | null>(null);
  const [feeling, setFeeling] = useState('');
  const [activity, setActivity] = useState('');
  const [locationQuery, setLocationQuery] = useState('');
  const [locationResults, setLocationResults] = useState<LocationChoice[]>([]);
  const [selectedLocation, setSelectedLocation] = useState<LocationChoice | null>(null);
  const [locationLoading, setLocationLoading] = useState(false);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [youtubeUrl, setYoutubeUrl] = useState('');
  const [scheduledAt, setScheduledAt] = useState(() => localDateTime(new Date(Date.now() + 60 * 60_000)));
  const [hideViews, setHideViews] = useState(false);
  const [hideLikes, setHideLikes] = useState(false);
  const [hideComments, setHideComments] = useState(false);
  const [disableComments, setDisableComments] = useState(false);
  const [threadPosts, setThreadPosts] = useState<string[]>([]);
  const [isSaving, setIsSaving] = useState(false);
  const [uploadMessage, setUploadMessage] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const previewUrlsRef = useRef(new Set<string>());

  useEffect(() => {
    const timer = window.setTimeout(() => {
      try {
        if (content) window.localStorage.setItem(draftKey, content);
        else window.localStorage.removeItem(draftKey);
      } catch { /* Browser storage may be disabled; the editor remains usable. */ }
    }, 250);
    return () => window.clearTimeout(timer);
  }, [content, draftKey]);

  useEffect(() => () => {
    previewUrlsRef.current.forEach(url => URL.revokeObjectURL(url));
    previewUrlsRef.current.clear();
  }, []);

  useEffect(() => {
    const query = tagQuery.trim().replace(/[%,_()]/g, '');
    if (!tools.tagged || query.length < 2) {
      setTagResults([]);
      setTagError(null);
      setTagLoading(false);
      return;
    }
    let active = true;
    const timer = window.setTimeout(async () => {
      setTagLoading(true);
      setTagError(null);
      const { data, error } = await supabase.from('users').select('uid,username,display_name,avatar').ilike('username', `%${query}%`).limit(8);
      if (!active) return;
      if (error) setTagError('People search is temporarily unavailable.');
      else setTagResults(((data || []) as TaggedPerson[]).filter(person => person.uid !== user.id && !taggedPeople.some(selected => selected.uid === person.uid)));
      setTagLoading(false);
    }, 280);
    return () => { active = false; window.clearTimeout(timer); };
  }, [tagQuery, taggedPeople, tools.tagged, user.id]);

  useEffect(() => {
    const query = locationQuery.trim();
    if (!tools.location || query.length < 3 || selectedLocation) {
      setLocationResults([]);
      setLocationLoading(false);
      return;
    }
    let active = true;
    const controller = new AbortController();
    const timer = window.setTimeout(async () => {
      setLocationLoading(true);
      setLocationError(null);
      try {
        const params = new URLSearchParams({ q: query, format: 'jsonv2', addressdetails: '1', limit: '6' });
        const response = await fetch(`https://nominatim.openstreetmap.org/search?${params.toString()}`, { signal: controller.signal, headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error(`Location search returned ${response.status}`);
        const data = await response.json() as PlaceResult[];
        if (!active) return;
        setLocationResults(data.map(place => {
          const address = place.address || {};
          const shortAddress = [address.city || address.town || address.village || address.county, address.state, address.country].filter(Boolean).join(', ');
          const display = place.display_name || query;
          return {
            name: place.name || display.split(',')[0] || query,
            address: shortAddress || display,
            latitude: Number.isFinite(Number(place.lat)) ? Number(place.lat) : null,
            longitude: Number.isFinite(Number(place.lon)) ? Number(place.lon) : null,
            placeId: place.place_id == null ? null : String(place.place_id),
          };
        }));
      } catch (error) {
        if (active && !(error instanceof DOMException && error.name === 'AbortError')) setLocationError('Location search is unavailable. You can still add a place name manually.');
      } finally {
        if (active) setLocationLoading(false);
      }
    }, 420);
    return () => { active = false; window.clearTimeout(timer); controller.abort(); };
  }, [locationQuery, selectedLocation, tools.location]);

  const toggleTool = (key: ToolKey) => {
    setFormError(null);
    if (key === 'poll' && !tools.poll && attachments.length > 0) {
      setFormError('Remove media attachments before adding a poll.');
      return;
    }
    setTools(current => ({ ...current, [key]: !current[key] }));
  };

  const handleFiles = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files || []);
    event.target.value = '';
    setFormError(null);
    if (tools.poll && files.length) {
      setFormError('Remove the poll before adding media.');
      return;
    }
    if (attachments.length + files.length > MAX_MEDIA_FILES) {
      setFormError(`Add up to ${MAX_MEDIA_FILES} media files to one post.`);
      return;
    }
    const invalid = files.find(file => !ALLOWED_MEDIA.has(file.type) || file.size > MAX_MEDIA_BYTES);
    if (invalid) {
      setFormError(!ALLOWED_MEDIA.has(invalid.type)
        ? 'Use a JPEG, PNG, GIF, WebP, MP4, or QuickTime video file.'
        : `${invalid.name} is over the 10 MB per-file limit.`);
      return;
    }
    const next = files.map(file => {
      const previewUrl = URL.createObjectURL(file);
      previewUrlsRef.current.add(previewUrl);
      return { id: crypto.randomUUID(), file, previewUrl, kind: file.type.startsWith('video/') ? 'video' as const : 'image' as const };
    });
    setAttachments(current => [...current, ...next]);
    if (next.length) setTools(current => ({ ...current, media: true }));
  };

  const removeAttachment = (id: string) => {
    setAttachments(current => {
      const removed = current.find(item => item.id === id);
      if (removed) {
        URL.revokeObjectURL(removed.previewUrl);
        previewUrlsRef.current.delete(removed.previewUrl);
      }
      return current.filter(item => item.id !== id);
    });
  };

  const addTaggedPerson = (person: TaggedPerson) => {
    setTaggedPeople(current => current.some(item => item.uid === person.uid) ? current : [...current, person]);
    setTagQuery('');
    setTagResults([]);
  };

  const useCurrentLocation = () => {
    if (!navigator.geolocation) {
      setLocationError('This browser does not provide location access. Enter a place name instead.');
      return;
    }
    setLocationLoading(true);
    setLocationError(null);
    navigator.geolocation.getCurrentPosition(
      position => {
        setSelectedLocation({ name: 'Current location', address: 'Added from this device', latitude: position.coords.latitude, longitude: position.coords.longitude, placeId: null });
        setLocationQuery('Current location');
        setLocationLoading(false);
      },
      () => {
        setLocationError('Location permission was not granted. You can search for or enter a place instead.');
        setLocationLoading(false);
      },
      { enableHighAccuracy: false, timeout: 10_000, maximumAge: 60_000 },
    );
  };

  const saveDraftAndClose = () => {
    try {
      if (content) window.localStorage.setItem(draftKey, content);
      else window.localStorage.removeItem(draftKey);
    } catch { /* The editor can still close when storage is disabled. */ }
    onClose();
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (isSaving) return;
    setFormError(null);
    setSuccessMessage(null);

    const text = content.trim();
    const cleanOptions = pollOptions.map(option => option.trim()).filter(Boolean);
    const threadText = threadPosts.map(post => post.trim()).filter(Boolean);
    const youtube = youtubeUrl.trim();
    if (tools.poll && (!pollQuestion.trim() || cleanOptions.length < 2 || new Set(cleanOptions.map(option => option.toLowerCase())).size !== cleanOptions.length)) {
      setFormError('A poll needs a question and at least two different answer options.');
      return;
    }
    if (tools.poll && cleanOptions.length > 4) {
      setFormError('A poll can have up to four answer options.');
      return;
    }
    if (youtube && !isYouTubeUrl(youtube)) {
      setFormError('Enter a valid YouTube or youtu.be link.');
      return;
    }
    if (tools.schedule) {
      const scheduledDate = new Date(scheduledAt);
      if (!Number.isFinite(scheduledDate.getTime()) || scheduledDate.getTime() < Date.now() + 60_000) {
        setFormError('Choose a schedule time at least one minute in the future.');
        return;
      }
    }
    if (!text && attachments.length === 0 && !tools.poll && !youtube) {
      setFormError('Add some text, media, a poll, or a YouTube link before publishing.');
      return;
    }

    setIsSaving(true);
    const uploadedPaths: string[] = [];
    const createdPostIds: string[] = [];
    try {
      const uploadedMedia: { id: string; url: string; type: 'image' | 'video'; mimeType: string; name: string; bucket: string }[] = [];
      for (let index = 0; index < attachments.length; index += 1) {
        const attachment = attachments[index];
        setUploadMessage(attachments.length > 1 ? `Uploading ${index + 1} of ${attachments.length}…` : 'Uploading media…');
        const path = `${user.id}/${crypto.randomUUID()}-${safeFileName(attachment.file.name)}`;
        const { error } = await supabase.storage.from(MEDIA_BUCKET).upload(path, attachment.file, { contentType: attachment.file.type, upsert: false, cacheControl: '3600' });
        if (error) throw error;
        uploadedPaths.push(path);
        uploadedMedia.push({ id: attachment.id, url: path, type: attachment.kind, mimeType: attachment.file.type, name: attachment.file.name, bucket: MEDIA_BUCKET });
      }

      const publishTime = tools.schedule ? new Date(scheduledAt) : new Date();
      const pollEndTime = tools.poll ? new Date(publishTime.getTime() + pollDuration * 60 * 60_000).toISOString() : null;
      const metadata = {
        layout_type: 'standard',
        taggedPeople: taggedPeople.map(person => ({ uid: person.uid, username: person.username, displayName: person.display_name, avatar: person.avatar })),
        feeling: feeling.trim() || null,
        activity: activity.trim() || null,
        textBackgroundColor: textBackground === 'none' ? null : textBackground,
      };
      const postPayload = {
        post_text: text || null,
        post_image: null,
        media_items: uploadedMedia,
        post_type: tools.poll ? 'POLL' : uploadedMedia.length ? 'MEDIA' : youtube ? 'YOUTUBE' : 'TEXT',
        post_visibility: audience,
        is_deleted: false,
        has_location: Boolean(selectedLocation),
        location_name: selectedLocation?.name || null,
        location_address: selectedLocation?.address || null,
        location_latitude: selectedLocation?.latitude ?? null,
        location_longitude: selectedLocation?.longitude ?? null,
        location_place_id: selectedLocation?.placeId || null,
        has_poll: Boolean(tools.poll),
        poll_question: tools.poll ? pollQuestion.trim() : null,
        poll_options: tools.poll ? cleanOptions.map((option, index) => ({ id: String(index), text: option, votes: 0 })) : [],
        poll_end_time: pollEndTime,
        poll_allow_multiple: tools.poll && pollAllowMultiple,
        post_hide_views_count: hideViews ? 'true' : 'false',
        post_hide_like_count: hideLikes ? 'true' : 'false',
        post_hide_comments_count: hideComments ? 'true' : 'false',
        post_disable_comments: disableComments ? 'true' : 'false',
        youtube_url: youtube || null,
        metadata,
      };

      if (tools.schedule) {
        setUploadMessage('Saving your scheduled post…');
        const { error } = await supabase.from('scheduled_posts').insert({
          owner_uid: user.id,
          scheduled_at: publishTime.toISOString(),
          payload: { ...postPayload, thread_posts: threadText },
          status: 'pending',
        });
        if (error) throw error;
      } else {
        setUploadMessage('Publishing…');
        const now = new Date();
        const rootRow = { ...postPayload, author_uid: user.id, timestamp: now.getTime(), publish_date: now.toISOString() };
        const { data: root, error: rootError } = await supabase.from('posts').insert(rootRow).select('id').single();
        if (rootError) throw rootError;
        if (root?.id) createdPostIds.push(String(root.id));
        let previousPostId = root?.id ? String(root.id) : null;
        for (const threadBody of threadText) {
          if (!previousPostId || !root?.id) throw new Error('The first post in the thread was not returned by the server.');
          const threadNow = new Date();
          const { data: reply, error: replyError } = await supabase.from('posts').insert({
            author_uid: user.id,
            post_text: threadBody,
            post_image: null,
            media_items: [],
            post_type: 'TEXT',
            post_visibility: audience,
            timestamp: threadNow.getTime(),
            publish_date: threadNow.toISOString(),
            is_deleted: false,
            has_location: false,
            has_poll: false,
            poll_options: [],
            post_hide_views_count: hideViews ? 'true' : 'false',
            post_hide_like_count: hideLikes ? 'true' : 'false',
            post_hide_comments_count: hideComments ? 'true' : 'false',
            post_disable_comments: disableComments ? 'true' : 'false',
            in_reply_to_post_id: previousPostId,
            root_post_id: String(root.id),
            reply_to_usernames: [],
          }).select('id').single();
          if (replyError) throw replyError;
          if (reply?.id) {
            createdPostIds.push(String(reply.id));
            previousPostId = String(reply.id);
          }
        }
      }

      try { window.localStorage.removeItem(draftKey); } catch { /* Ignore disabled storage. */ }
      setSuccessMessage(tools.schedule ? `Scheduled for ${publishTime.toLocaleString()}.` : 'Your post has been published.');
      setUploadMessage('');
      window.setTimeout(() => {
        onPostCreated?.();
        onClose();
      }, 850);
    } catch (error) {
      if (uploadedPaths.length) {
        const { error: cleanupError } = await supabase.storage.from(MEDIA_BUCKET).remove(uploadedPaths);
        if (cleanupError) console.error('Could not clean up uploaded post media after a failed post save.', cleanupError);
      }
      if (createdPostIds.length) {
        const { error: rollbackError } = await supabase.from('posts').delete().in('id', createdPostIds);
        if (rollbackError) console.error('Could not roll back every post in a partially created thread.', rollbackError);
      }
      setUploadMessage('');
      setFormError(error instanceof Error ? error.message : 'Could not save your post. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  const isReadyToPost = Boolean(content.trim() || attachments.length || (tools.poll && pollQuestion.trim()) || youtubeUrl.trim());
  const selectedBackground = TEXT_BACKGROUNDS.find(option => option.value === textBackground) || TEXT_BACKGROUNDS[0];

  return (
    <div className="min-h-screen bg-[#f7f7f8] text-zinc-950">
      <header className="sticky top-0 z-30 border-b border-zinc-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-3 px-4 sm:px-6">
          <button type="button" onClick={saveDraftAndClose} className="inline-flex items-center gap-2 rounded-full px-3 py-2 text-sm font-medium text-zinc-600 transition hover:bg-zinc-100 hover:text-zinc-950"><ArrowLeft className="h-4 w-4" /><span className="hidden sm:inline">Back to home</span></button>
          <div className="text-center"><p className="text-sm font-semibold text-zinc-950">Create a post</p><p className="hidden text-[11px] text-zinc-500 sm:block">Share a thought, a moment, or a conversation</p></div>
          <button type="button" onClick={saveDraftAndClose} className="rounded-full border border-zinc-200 bg-white px-4 py-2 text-xs font-semibold text-zinc-700 transition hover:border-zinc-300 hover:bg-zinc-50">Save draft</button>
        </div>
      </header>

      <main className="mx-auto grid max-w-7xl gap-6 px-4 py-6 sm:px-6 sm:py-9 lg:grid-cols-[minmax(0,760px)_300px] lg:items-start lg:justify-center">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="rounded-3xl border border-zinc-200 bg-white p-5 shadow-[0_4px_24px_-18px_rgba(0,0,0,0.22)] sm:p-7">
            <div className="flex items-start gap-3">
              <span className="grid h-12 w-12 shrink-0 place-items-center overflow-hidden rounded-full bg-zinc-100 text-sm font-semibold text-zinc-700 ring-1 ring-zinc-200">{user.avatarUrl ? <img src={user.avatarUrl} alt="" className="h-full w-full object-cover" /> : initials(user.fullName || user.username)}</span>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0"><p className="truncate text-sm font-semibold text-zinc-950">{user.fullName || user.username}</p><p className="mt-0.5 truncate text-xs text-zinc-500">@{user.username}</p></div>
                  <label className="flex items-center gap-2 rounded-xl border border-zinc-200 bg-zinc-50 px-3 py-2 text-xs font-medium text-zinc-700">
                    <span className="text-zinc-500">Audience</span>
                    <select value={audience} onChange={event => setAudience(event.target.value as Audience)} className="max-w-32 bg-transparent font-semibold text-zinc-900 outline-none" aria-label="Post audience">
                      <option value="public">Everyone</option>
                      <option value="followers">Followers</option>
                      <option value="private">Only me</option>
                    </select>
                  </label>
                </div>
                <label htmlFor="create-post-content" className="sr-only">Write your post</label>
                <textarea
                  id="create-post-content"
                  value={content}
                  onChange={event => setContent(event.target.value)}
                  maxLength={5000}
                  rows={7}
                  placeholder="What would you like to share?"
                  style={{ backgroundColor: selectedBackground.color, color: selectedBackground.text }}
                  className="mt-5 min-h-44 w-full resize-y rounded-2xl border border-zinc-100 px-4 py-4 text-[16px] leading-7 outline-none transition placeholder:text-zinc-400 focus:border-zinc-300 focus:ring-2 focus:ring-zinc-100"
                />
                <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
                  <div className="flex items-center gap-2" role="group" aria-label="Text background">
                    <span className="text-[11px] font-medium text-zinc-500">Text style</span>
                    {TEXT_BACKGROUNDS.map(option => (
                      <button key={option.value} type="button" title={option.label} aria-label={`${option.label} text background`} aria-pressed={textBackground === option.value} onClick={() => setTextBackground(option.value)} className={`h-6 w-6 rounded-full border transition ${textBackground === option.value ? 'ring-2 ring-zinc-800 ring-offset-2' : 'border-zinc-300 hover:scale-105'}`} style={{ backgroundColor: option.color }} />
                    ))}
                  </div>
                  <span className="text-[11px] tabular-nums text-zinc-400">{content.length.toLocaleString()} / 5,000</span>
                </div>
              </div>
            </div>
          </div>

          {tools.media && (
            <SectionCard title="Add media" description="Images and videos up to 10 MB each. Choose up to eight files." icon={<ImagePlus className="h-4 w-4" />} onRemove={() => setTools(current => ({ ...current, media: false }))}>
              <input ref={fileInputRef} type="file" multiple accept="image/jpeg,image/png,image/gif,image/webp,video/mp4,video/quicktime" onChange={handleFiles} className="sr-only" aria-label="Choose images or videos" />
              <div className="grid gap-3 sm:grid-cols-2">
                {attachments.map(item => (
                  <div key={item.id} className="group relative overflow-hidden rounded-2xl border border-zinc-200 bg-zinc-50">
                    {item.kind === 'video' ? <video src={item.previewUrl} className="h-48 w-full object-cover" controls playsInline /> : <img src={item.previewUrl} alt={item.file.name} className="h-48 w-full object-cover" />}
                    <div className="flex items-center justify-between gap-2 px-3 py-2"><span className="truncate text-xs text-zinc-600">{item.file.name} · {formatBytes(item.file.size)}</span><button type="button" onClick={() => removeAttachment(item.id)} aria-label={`Remove ${item.file.name}`} className="rounded-lg p-1.5 text-zinc-500 hover:bg-white hover:text-zinc-950"><Trash2 className="h-4 w-4" /></button></div>
                  </div>
                ))}
              </div>
              <button type="button" onClick={() => fileInputRef.current?.click()} className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-zinc-300 bg-zinc-50 px-4 py-4 text-sm font-medium text-zinc-600 transition hover:border-zinc-500 hover:bg-white hover:text-zinc-950"><Plus className="h-4 w-4" />{attachments.length ? 'Add another file' : 'Choose photos or videos'}</button>
            </SectionCard>
          )}

          {tools.poll && (
            <SectionCard title="Create a poll" description="Ask a question and add two to four answers." icon={<Check className="h-4 w-4" />} onRemove={() => setTools(current => ({ ...current, poll: false }))}>
              <label className="mb-3 block text-xs font-semibold text-zinc-600" htmlFor="poll-question">Question</label>
              <input id="poll-question" value={pollQuestion} onChange={event => setPollQuestion(event.target.value)} maxLength={180} placeholder="Ask your community…" className="h-11 w-full rounded-xl border border-zinc-200 px-3 text-sm outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100" />
              <div className="mt-4 space-y-2">
                {pollOptions.map((option, index) => (
                  <div key={index} className="flex gap-2"><label className="sr-only" htmlFor={`poll-option-${index}`}>Option {index + 1}</label><input id={`poll-option-${index}`} value={option} onChange={event => setPollOptions(current => current.map((value, optionIndex) => optionIndex === index ? event.target.value : value))} maxLength={80} placeholder={`Option ${index + 1}`} className="h-10 min-w-0 flex-1 rounded-xl border border-zinc-200 px-3 text-sm outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100" />{pollOptions.length > 2 && <button type="button" onClick={() => setPollOptions(current => current.filter((_, optionIndex) => optionIndex !== index))} aria-label={`Remove option ${index + 1}`} className="rounded-xl px-3 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-900"><X className="h-4 w-4" /></button>}</div>
                ))}
              </div>
              {pollOptions.length < 4 && <button type="button" onClick={() => setPollOptions(current => [...current, ''])} className="mt-3 inline-flex items-center gap-2 rounded-lg px-2 py-1.5 text-xs font-semibold text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"><Plus className="h-3.5 w-3.5" />Add option</button>}
              <div className="mt-4 grid gap-3 border-t border-zinc-100 pt-4 sm:grid-cols-2">
                <label className="text-xs font-semibold text-zinc-600">Poll duration<select value={pollDuration} onChange={event => setPollDuration(Number(event.target.value))} className="mt-2 h-10 w-full rounded-xl border border-zinc-200 bg-white px-3 text-sm font-medium text-zinc-800 outline-none focus:border-zinc-400">{POLL_DURATIONS.map(duration => <option key={duration.value} value={duration.value}>{duration.label}</option>)}</select></label>
                <ToggleRow title="Allow multiple choices" description="Let people select more than one answer." checked={pollAllowMultiple} onChange={setPollAllowMultiple} />
              </div>
            </SectionCard>
          )}

          {tools.tagged && (
            <SectionCard title="Tag people" description="Mention people who are part of this post." icon={<UserPlus className="h-4 w-4" />} onRemove={() => setTools(current => ({ ...current, tagged: false }))}>
              <div className="relative"><label htmlFor="tag-search" className="sr-only">Search by username</label><input id="tag-search" value={tagQuery} onChange={event => setTagQuery(event.target.value.replace(/^@/, ''))} placeholder="Search usernames…" className="h-11 w-full rounded-xl border border-zinc-200 px-3 text-sm outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100" />{tagLoading && <Loader2 className="absolute right-3 top-3 h-4 w-4 animate-spin text-zinc-400" />}</div>
              {tagError && <p role="status" className="mt-2 text-xs text-zinc-500">{tagError}</p>}
              {tagResults.length > 0 && <div className="mt-2 overflow-hidden rounded-xl border border-zinc-200">{tagResults.map(person => <button key={person.uid} type="button" onClick={() => addTaggedPerson(person)} className="flex w-full items-center gap-3 px-3 py-2.5 text-left hover:bg-zinc-50"><span className="grid h-8 w-8 place-items-center overflow-hidden rounded-full bg-zinc-100 text-[10px] font-semibold text-zinc-700">{person.avatar ? <img src={person.avatar} alt="" className="h-full w-full object-cover" /> : initials(profileLabel(person))}</span><span className="min-w-0 flex-1"><span className="block truncate text-sm font-medium text-zinc-900">{profileLabel(person)}</span><span className="block truncate text-xs text-zinc-500">@{person.username || 'member'}</span></span><Plus className="h-4 w-4 text-zinc-400" /></button>)}</div>}
              {tagQuery.trim().length >= 2 && !tagLoading && !tagError && tagResults.length === 0 && <p className="mt-2 text-xs text-zinc-500">No matching people found.</p>}
              {taggedPeople.length > 0 && <div className="mt-3 flex flex-wrap gap-2">{taggedPeople.map(person => <span key={person.uid} className="inline-flex items-center gap-2 rounded-full border border-zinc-200 bg-zinc-50 py-1 pl-1 pr-2 text-xs font-medium text-zinc-700"><span className="grid h-6 w-6 place-items-center overflow-hidden rounded-full bg-white text-[9px]">{person.avatar ? <img src={person.avatar} alt="" className="h-full w-full object-cover" /> : initials(profileLabel(person))}</span>@{person.username || person.display_name || 'member'}<button type="button" onClick={() => setTaggedPeople(current => current.filter(item => item.uid !== person.uid))} aria-label={`Remove ${profileLabel(person)}`}><X className="h-3.5 w-3.5 text-zinc-400 hover:text-zinc-900" /></button></span>)}</div>}
            </SectionCard>
          )}

          {tools.feeling && (
            <SectionCard title="Feeling or activity" description="Add a little context to your update." icon={<Smile className="h-4 w-4" />} onRemove={() => setTools(current => ({ ...current, feeling: false }))}>
              <div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-semibold text-zinc-600">Feeling<input value={feeling} onChange={event => setFeeling(event.target.value)} maxLength={60} placeholder="How are you feeling?" className="mt-2 h-11 w-full rounded-xl border border-zinc-200 px-3 text-sm font-normal text-zinc-900 outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100" /></label><label className="text-xs font-semibold text-zinc-600">Activity<input value={activity} onChange={event => setActivity(event.target.value)} maxLength={80} placeholder="What are you doing?" className="mt-2 h-11 w-full rounded-xl border border-zinc-200 px-3 text-sm font-normal text-zinc-900 outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100" /></label></div>
            </SectionCard>
          )}

          {tools.location && (
            <SectionCard title="Add a location" description="Search for a place or use your device location. Location is optional." icon={<MapPin className="h-4 w-4" />} onRemove={() => { setTools(current => ({ ...current, location: false })); setSelectedLocation(null); }}>
              {selectedLocation ? <div className="flex items-center gap-3 rounded-xl border border-zinc-200 bg-zinc-50 p-3"><span className="grid h-9 w-9 place-items-center rounded-lg bg-white text-zinc-700"><MapPin className="h-4 w-4" /></span><span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold text-zinc-900">{selectedLocation.name}</span><span className="block truncate text-xs text-zinc-500">{selectedLocation.address}</span></span><button type="button" onClick={() => { setSelectedLocation(null); setLocationQuery(''); }} aria-label="Remove location" className="rounded-lg p-2 text-zinc-400 hover:bg-white hover:text-zinc-900"><X className="h-4 w-4" /></button></div> : <>
                <div className="flex flex-col gap-2 sm:flex-row"><label className="sr-only" htmlFor="location-search">Search for a place</label><input id="location-search" value={locationQuery} onChange={event => setLocationQuery(event.target.value)} placeholder="Search a city, venue, or place…" className="h-11 min-w-0 flex-1 rounded-xl border border-zinc-200 px-3 text-sm outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100" /><button type="button" onClick={useCurrentLocation} className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-xl border border-zinc-200 px-3 text-xs font-semibold text-zinc-700 hover:bg-zinc-50"><MapPin className="h-4 w-4" />Use my location</button></div>
                {locationLoading && <p className="mt-2 inline-flex items-center gap-2 text-xs text-zinc-500"><Loader2 className="h-3.5 w-3.5 animate-spin" />Searching places…</p>}
                {locationError && <p role="status" className="mt-2 text-xs text-zinc-500">{locationError}</p>}
                {locationResults.length > 0 && <div className="mt-2 overflow-hidden rounded-xl border border-zinc-200">{locationResults.map((place, index) => <button key={`${place.placeId || place.name}-${index}`} type="button" onClick={() => { setSelectedLocation(place); setLocationQuery(place.name); setLocationResults([]); }} className="flex w-full items-start gap-3 border-b border-zinc-100 px-3 py-3 text-left last:border-b-0 hover:bg-zinc-50"><MapPin className="mt-0.5 h-4 w-4 shrink-0 text-zinc-400" /><span className="min-w-0"><span className="block truncate text-sm font-medium text-zinc-900">{place.name}</span><span className="mt-0.5 block truncate text-xs text-zinc-500">{place.address}</span></span></button>)}</div>}
                {locationQuery.trim().length >= 3 && !locationLoading && locationResults.length === 0 && <button type="button" onClick={() => setSelectedLocation({ name: locationQuery.trim(), address: '', latitude: null, longitude: null, placeId: null })} className="mt-2 rounded-lg px-2 py-1.5 text-xs font-semibold text-zinc-600 hover:bg-zinc-100">Use “{locationQuery.trim()}” as the place name</button>}
                <p className="mt-3 text-[10px] leading-4 text-zinc-400">Place suggestions by OpenStreetMap contributors.</p>
              </>}
            </SectionCard>
          )}

          {tools.youtube && (
            <SectionCard title="YouTube link" description="Attach a video from YouTube or youtu.be." icon={<Youtube className="h-4 w-4" />} onRemove={() => { setTools(current => ({ ...current, youtube: false })); setYoutubeUrl(''); }}>
              <label htmlFor="youtube-url" className="sr-only">YouTube URL</label><input id="youtube-url" type="url" value={youtubeUrl} onChange={event => setYoutubeUrl(event.target.value)} placeholder="https://www.youtube.com/watch?v=…" className="h-11 w-full rounded-xl border border-zinc-200 px-3 text-sm outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100" />
              {youtubeUrl && !isYouTubeUrl(youtubeUrl) && <p role="alert" className="mt-2 text-xs text-zinc-600">Use a YouTube or youtu.be link.</p>}
            </SectionCard>
          )}

          {tools.schedule && (
            <SectionCard title="Schedule your post" description="It will publish automatically at the selected time, even if you leave this page." icon={<CalendarClock className="h-4 w-4" />} onRemove={() => setTools(current => ({ ...current, schedule: false }))}>
              <label htmlFor="scheduled-at" className="block text-xs font-semibold text-zinc-600">Publish date and time</label><input id="scheduled-at" type="datetime-local" min={localDateTime(new Date(Date.now() + 60_000))} value={scheduledAt} onChange={event => setScheduledAt(event.target.value)} className="mt-2 h-11 w-full rounded-xl border border-zinc-200 bg-white px-3 text-sm outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100" />
              <p className="mt-2 text-xs leading-5 text-zinc-500">Scheduled posts are stored privately and published by Synapse when they are due.</p>
            </SectionCard>
          )}

          {tools.settings && (
            <SectionCard title="Post settings" description="Choose what people can see and how they can respond." icon={<Settings2 className="h-4 w-4" />} onRemove={() => setTools(current => ({ ...current, settings: false }))}>
              <div className="divide-y divide-zinc-100">
                <ToggleRow title="Hide view count" description="Keep the view total private on this post." checked={hideViews} onChange={setHideViews} />
                <ToggleRow title="Hide like count" description="Hide the number of likes from other people." checked={hideLikes} onChange={setHideLikes} />
                <ToggleRow title="Hide comment count" description="Hide the number of comments on this post." checked={hideComments} onChange={setHideComments} />
                <ToggleRow title="Turn off comments" description="People will be able to view, but not comment on this post." checked={disableComments} onChange={setDisableComments} />
              </div>
            </SectionCard>
          )}

          {tools.thread && (
            <SectionCard title="Continue as a thread" description="Add follow-up posts that will be linked in order." icon={<AtSign className="h-4 w-4" />} onRemove={() => setTools(current => ({ ...current, thread: false }))}>
              <div className="space-y-3">{threadPosts.map((post, index) => <label key={index} className="block"><span className="mb-1.5 block text-xs font-semibold text-zinc-600">Post {index + 2}</span><div className="flex items-start gap-2"><textarea value={post} onChange={event => setThreadPosts(current => current.map((value, i) => i === index ? event.target.value : value))} rows={3} maxLength={5000} placeholder="Continue the thought…" className="min-h-20 min-w-0 flex-1 resize-y rounded-xl border border-zinc-200 px-3 py-2.5 text-sm leading-6 outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100" /><button type="button" onClick={() => setThreadPosts(current => current.filter((_, i) => i !== index))} aria-label={`Remove thread post ${index + 2}`} className="rounded-lg p-2 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-900"><X className="h-4 w-4" /></button></div></label>)}</div>
              {threadPosts.length < 5 && <button type="button" onClick={() => setThreadPosts(current => [...current, ''])} className="mt-3 inline-flex items-center gap-2 rounded-lg px-2 py-1.5 text-xs font-semibold text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950"><Plus className="h-3.5 w-3.5" />Add another post</button>}
            </SectionCard>
          )}

          <div className="rounded-2xl border border-zinc-200 bg-white p-4 sm:p-5">
            <p className="mb-3 text-xs font-semibold uppercase tracking-[0.14em] text-zinc-500">Add to your post</p>
            <div className="flex flex-wrap gap-2">
              {TOOL_ITEMS.map(item => {
                const Icon = item.icon;
                const active = tools[item.key];
                return <button key={item.key} type="button" aria-pressed={active} onClick={() => toggleTool(item.key)} className={`inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-xs font-semibold transition ${active ? 'border-zinc-950 bg-zinc-950 text-white' : 'border-zinc-200 bg-white text-zinc-600 hover:border-zinc-400 hover:text-zinc-950'}`}><Icon className="h-3.5 w-3.5" />{item.label}</button>;
              })}
            </div>
          </div>

          {formError && <div role="alert" className="rounded-2xl border border-zinc-300 bg-white px-4 py-3 text-sm text-zinc-700">{formError}</div>}
          {successMessage && <div role="status" className="flex items-center gap-2 rounded-2xl border border-zinc-300 bg-white px-4 py-3 text-sm font-medium text-zinc-800"><CheckCircle2 className="h-4 w-4" />{successMessage}</div>}

          <div className="sticky bottom-3 z-20 flex items-center justify-between gap-3 rounded-2xl border border-zinc-200 bg-white/95 p-3 shadow-[0_10px_32px_-16px_rgba(0,0,0,0.25)] backdrop-blur sm:p-4">
            <span className="min-w-0 truncate text-xs text-zinc-500">{uploadMessage || (tools.schedule ? `Scheduled · ${new Date(scheduledAt).toLocaleString()}` : 'Your draft saves on this device')}</span>
            <button type="submit" disabled={!isReadyToPost || isSaving || Boolean(successMessage)} className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-xl bg-zinc-950 px-5 text-sm font-semibold text-white transition hover:bg-zinc-800 disabled:cursor-not-allowed disabled:bg-zinc-300">
              {isSaving ? <><Loader2 className="h-4 w-4 animate-spin" />{uploadMessage || 'Saving…'}</> : tools.schedule ? <><CalendarClock className="h-4 w-4" />Schedule post</> : <><Sparkles className="h-4 w-4" />Publish post</>}
            </button>
          </div>
        </form>

        <aside className="hidden space-y-4 lg:sticky lg:top-24 lg:block">
          <div className="rounded-3xl border border-zinc-200 bg-white p-5">
            <div className="flex items-center gap-2"><Sparkles className="h-4 w-4 text-zinc-700" /><h2 className="text-sm font-semibold text-zinc-950">Make it yours</h2></div>
            <p className="mt-3 text-sm leading-6 text-zinc-600">Add a photo, start a poll, tag people, or give your post a place and a little context.</p>
            <div className="mt-4 space-y-3 border-t border-zinc-100 pt-4 text-xs leading-5 text-zinc-500">
              <p className="flex gap-2"><Clock3 className="mt-0.5 h-3.5 w-3.5 shrink-0" />Your text draft autosaves in this browser.</p>
              <p className="flex gap-2"><EyeOff className="mt-0.5 h-3.5 w-3.5 shrink-0" />Audience and post settings apply to every post in a thread.</p>
              <p className="flex gap-2"><MessageCircleOff className="mt-0.5 h-3.5 w-3.5 shrink-0" />Media uploads are private to the people allowed to view your post.</p>
              <p className="flex gap-2"><Video className="mt-0.5 h-3.5 w-3.5 shrink-0" />Supported uploads: JPEG, PNG, GIF, WebP, MP4, and QuickTime.</p>
            </div>
          </div>
          <div className="rounded-3xl border border-zinc-200 bg-zinc-50 p-5"><p className="text-xs font-semibold uppercase tracking-[0.14em] text-zinc-500">Before you publish</p><p className="mt-2 text-sm leading-6 text-zinc-600">Check your audience, review tagged people, and make sure any location or link is something you want to share.</p></div>
        </aside>
      </main>
    </div>
  );
};
