import React, { useEffect, useRef, useState } from 'react';
import { supabase } from '@/lib/supabase';
import { STORAGE_BUCKETS } from '@/config/storageBuckets';
import type { CreatePostScreenProps, Audience, ToolKey, PostToolState, MediaAttachment, TaggedPerson, LocationChoice, PlaceResult } from './createPostShared';
import { ALLOWED_MEDIA, MAX_MEDIA_BYTES, MAX_MEDIA_FILES, TEXT_BACKGROUNDS, getDraftKey, isYouTubeUrl, loadDraftText, localDateTime, safeFileName } from './createPostShared';

const MEDIA_BUCKET = STORAGE_BUCKETS.POSTS_MEDIA;

export const useCreatePost = ({ user, onClose, onPostCreated }: CreatePostScreenProps) => {
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

  return { user, onClose, onPostCreated, content, setContent, audience, setAudience, tools, setTools, attachments, setAttachments, textBackground, setTextBackground, selectedBackground, pollQuestion, setPollQuestion, pollOptions, setPollOptions, pollDuration, setPollDuration, pollAllowMultiple, setPollAllowMultiple, tagQuery, setTagQuery, tagResults, taggedPeople, setTaggedPeople, tagLoading, tagError, feeling, setFeeling, activity, setActivity, locationQuery, setLocationQuery, locationResults, selectedLocation, setSelectedLocation, locationLoading, locationError, youtubeUrl, setYoutubeUrl, scheduledAt, setScheduledAt, hideViews, setHideViews, hideLikes, setHideLikes, hideComments, setHideComments, disableComments, setDisableComments, threadPosts, setThreadPosts, isSaving, uploadMessage, formError, successMessage, fileInputRef, toggleTool, handleFiles, removeAttachment, addTaggedPerson, useCurrentLocation, saveDraftAndClose, handleSubmit, isReadyToPost };
};

export type CreatePostModel = ReturnType<typeof useCreatePost>;
