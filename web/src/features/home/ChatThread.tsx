import React, { useCallback, useEffect, useRef, useState } from 'react';
import { AlertTriangle, ArrowLeft, Bell, BellOff, Check, CheckCheck, Download, FileText, Image, Loader2, LockKeyhole, Mic, MoreVertical, Pause, Play, Plus, Send, Square, Trash2, X } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { supabase } from '@/lib/supabase';
import { STORAGE_BUCKETS } from '@/config/storageBuckets';
import type { UserProfile } from '@/types';
import { getMessageDisplayText, isEncryptedMessageContent } from './chatMessageContent';
import { deliveryLabel, getMessageDeliveryState, type MessageDeliveryState } from './messageDelivery';
import { BatchMediaDeck } from './BatchMediaDeck';
import { BatchMediaGallery } from './BatchMediaGallery';
import { ChatComposerSheet, type ChatPickerKind, type ChatTypedMessageDraft } from './ChatComposerSheet';
import { ChatMessageCards, type MessagePollVote } from './ChatMessageCards';
import type { MediaAttachment, MediaKind } from './mediaAttachmentUtils';
import { collectActiveSyraRequests, isFreshSyraInvocation, isRecentSyraProcessingTimestamp, SYRA_TYPING_MAX_AGE_MS, type SyraInvocationStatusRow } from './syraTypingStatus';

interface ChatMessage {
  id: string;
  chat_id: string;
  sender_id: string;
  reply_to_id: string | null;
  content: string | null;
  message_type: string | null;
  content_state: string | null;
  created_at: string | null;
  is_deleted: boolean | null;
  expires_at: string | null;
  deleted_for_users: unknown;
  media_url: string | null;
  attachments: unknown;
  metadata?: unknown;
  delivery_status?: string | null;
  message_state?: string | null;
  delivered_at?: string | null;
  read_at?: string | null;
  read_by?: unknown;
  localStatus?: 'sending' | 'sent' | 'failed';
  animateIn?: boolean;
}

interface ChatThreadProps {
  chatId: string;
  title: string;
  subtitle: string;
  avatarUrl: string | null;
  user: UserProfile;
  onBack: () => void;
}

interface VoiceDraft {
  blob: Blob;
  url: string;
  durationSeconds: number;
}

const MEDIA_BUCKET = STORAGE_BUCKETS.CHAT_MEDIA;
const MAX_MEDIA_BYTES = 25 * 1024 * 1024;
const MAX_BATCH_MEDIA_ITEMS = 10;
const MAX_BATCH_MEDIA_BYTES = 100 * 1024 * 1024;
const SIGNED_URL_SECONDS = 60 * 60 * 24 * 365;
const MESSAGE_FIELDS = 'id,chat_id,sender_id,reply_to_id,content,message_type,content_state,created_at,is_deleted,expires_at,deleted_for_users,media_url,attachments,metadata,delivery_status,message_state,delivered_at,read_at,read_by';

const isRecord = (value: unknown): value is Record<string, unknown> => Boolean(value && typeof value === 'object' && !Array.isArray(value));
const normalizeMediaKind = (value: unknown, fallback: unknown): MediaKind => {
  const candidate = (typeof value === 'string' ? value : typeof fallback === 'string' ? fallback : 'file').toLowerCase();
  if (candidate === 'gif') return 'image';
  return candidate === 'image' || candidate === 'video' || candidate === 'audio' || candidate === 'file' ? candidate : 'file';
};
const getMessageMediaAttachments = (message: ChatMessage): MediaAttachment[] => {
  const attachments: MediaAttachment[] = [];
  if (Array.isArray(message.attachments)) {
    for (const value of message.attachments) {
      if (!isRecord(value) || typeof value.url !== 'string') continue;
      attachments.push({
        url: value.url,
        type: normalizeMediaKind(value.type, message.message_type),
        ...(typeof value.name === 'string' ? { name: value.name } : {}),
        ...(typeof value.mimeType === 'string' ? { mimeType: value.mimeType } : {}),
        ...(typeof value.size === 'number' ? { size: value.size } : {}),
        ...(typeof value.duration === 'number' ? { duration: value.duration } : {}),
        ...(typeof value.width === 'number' ? { width: value.width } : {}),
        ...(typeof value.height === 'number' ? { height: value.height } : {}),
      });
    }
  }
  if (attachments.length) return attachments;
  if (!message.media_url) return [];
  return [{
    url: message.media_url,
    type: normalizeMediaKind(message.message_type, 'file'),
    ...(message.content ? { name: message.content } : {}),
  }];
};
const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0] || '').join('').toUpperCase() || 'S';

const safeImage = (value: string | null) => {
  if (!value) return null;
  try {
    const url = new URL(value, window.location.origin);
    return url.protocol === 'https:' || url.origin === window.location.origin ? url.toString() : null;
  } catch {
    return null;
  }
};

const getObjectPathFromSignedUrl = (value: string | null) => {
  if (!value) return null;
  try {
    const pathname = new URL(value, window.location.origin).pathname;
    const marker = `/storage/v1/object/sign/${MEDIA_BUCKET}/`;
    const index = pathname.indexOf(marker);
    return index >= 0 ? decodeURIComponent(pathname.slice(index + marker.length)) : null;
  } catch {
    return null;
  }
};

const wasDeletedForUser = (value: unknown, userId: string) =>
  Array.isArray(value) && value.some(item => item === userId);

const visibleToUser = (message: ChatMessage, userId: string) =>
  message.is_deleted !== true &&
  message.content_state !== 'deleted' &&
  message.content_state !== 'expired' &&
  !wasDeletedForUser(message.deleted_for_users, userId) &&
  (!message.expires_at || !Number.isFinite(Date.parse(message.expires_at)) || Date.parse(message.expires_at) > Date.now());

const messageTime = (value: string | null) => {
  if (!value || !Number.isFinite(Date.parse(value))) return '';
  return new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(new Date(value));
};

const mediaTypeFor = (file: File): 'image' | 'video' | 'audio' | 'file' => {
  if (file.type.startsWith('image/')) return 'image';
  if (file.type.startsWith('video/')) return 'video';
  if (file.type.startsWith('audio/')) return 'audio';
  return 'file';
};

const extensionFor = (file: File, type: string) => {
  const fromName = file.name.split('.').pop()?.toLowerCase().replace(/[^a-z0-9]/g, '');
  if (fromName && fromName.length <= 8) return fromName;
  if (type === 'audio') return 'webm';
  if (type === 'video') return 'mp4';
  if (type === 'image') return 'jpg';
  return 'bin';
};

const formatFileSize = (value: unknown) => {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0) return '';
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
};

const fileTypeLabel = (fileName: string, mimeType: unknown) => {
  if (typeof mimeType === 'string' && mimeType.toLowerCase().includes('pdf')) return 'PDF';
  const extension = fileName.split('.').pop()?.replace(/[^a-z0-9]/gi, '').toUpperCase();
  return extension && extension.length <= 6 ? extension : 'FILE';
};

const formatPlaybackTime = (seconds: number) => {
  if (!Number.isFinite(seconds) || seconds < 0) return '00:00';
  const safeSeconds = Math.floor(seconds);
  return `${Math.floor(safeSeconds / 60).toString().padStart(2, '0')}:${(safeSeconds % 60).toString().padStart(2, '0')}`;
};

const isSupportedMediaFile = (file: File) =>
  file.type.startsWith('image/') || file.type.startsWith('video/') || file.type.startsWith('audio/') || file.type === 'application/pdf';

const readVideoMetadata = (file: File): Promise<{ duration?: number; width?: number; height?: number }> => new Promise(resolve => {
  if (typeof document === 'undefined' || typeof URL.createObjectURL !== 'function') {
    resolve({});
    return;
  }
  const video = document.createElement('video');
  const objectUrl = URL.createObjectURL(file);
  let finished = false;
  const finish = (metadata: { duration?: number; width?: number; height?: number }) => {
    if (finished) return;
    finished = true;
    window.clearTimeout(timeout);
    video.onloadedmetadata = null;
    video.onerror = null;
    URL.revokeObjectURL(objectUrl);
    resolve(metadata);
  };
  const timeout = window.setTimeout(() => finish({}), 3000);
  video.preload = 'metadata';
  video.onloadedmetadata = () => finish({
    ...(Number.isFinite(video.duration) && video.duration > 0 ? { duration: video.duration } : {}),
    ...(video.videoWidth > 0 ? { width: video.videoWidth } : {}),
    ...(video.videoHeight > 0 ? { height: video.videoHeight } : {}),
  });
  video.onerror = () => finish({});
  video.src = objectUrl;
});

const ChatAvatar: React.FC<{ title: string; url: string | null }> = ({ title, url }) => {
  const src = safeImage(url);
  return <span className="grid h-11 w-11 shrink-0 place-items-center overflow-hidden rounded-full bg-zinc-100 text-sm font-semibold text-zinc-700 ring-1 ring-zinc-200 dark:bg-zinc-800 dark:text-zinc-200 dark:ring-zinc-700">{src ? <img src={src} alt="" className="h-full w-full object-cover" /> : initials(title)}</span>;
};

const DeliveryIndicator: React.FC<{ state: MessageDeliveryState }> = ({ state }) => (
  <span title={deliveryLabel[state]} aria-label={deliveryLabel[state]} className={`inline-flex items-center ${state === 'read' ? 'font-semibold' : ''}`}>
    {state === 'sending' ? <Loader2 className="h-3 w-3 animate-[spin_0.65s_linear_infinite]" /> : state === 'failed' ? <AlertTriangle className="h-3 w-3" /> : state === 'sent' ? <Check className="h-3 w-3" /> : <CheckCheck className="h-3 w-3" />}
  </span>
);

const VOICE_WAVEFORM = [12, 20, 14, 24, 16, 28, 18, 23, 13, 27, 17, 21, 30, 16, 24, 13, 20, 27, 15, 23, 12, 28, 18, 24, 14, 20, 12, 26, 17, 22, 13, 19];

interface VoiceMessagePlayerProps {
  src: string;
  durationSeconds?: number;
  fromMe: boolean;
  onError?: () => void;
}

const VoiceMessagePlayer: React.FC<VoiceMessagePlayerProps> = ({ src, durationSeconds, fromMe, onError }) => {
  const audioRef = useRef<HTMLAudioElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [actualDuration, setActualDuration] = useState(durationSeconds || 0);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [playbackError, setPlaybackError] = useState(false);
  const duration = actualDuration || durationSeconds || 0;
  const progress = duration > 0 ? Math.min(1, Math.max(0, currentTime / duration)) : 0;
  const speedOptions = [1, 1.25, 1.5, 2];

  useEffect(() => {
    setCurrentTime(0);
    setActualDuration(durationSeconds || 0);
    setIsPlaying(false);
    setPlaybackError(false);
  }, [src, durationSeconds]);

  const seekTo = (nextTime: number) => {
    if (!duration) return;
    const bounded = Math.max(0, Math.min(duration, nextTime));
    if (audioRef.current) audioRef.current.currentTime = bounded;
    setCurrentTime(bounded);
  };

  const togglePlayback = () => {
    const audio = audioRef.current;
    if (!audio) return;
    setPlaybackError(false);
    if (audio.paused) {
      void audio.play().catch(() => setPlaybackError(true));
    } else {
      audio.pause();
    }
  };

  const cycleSpeed = () => {
    const currentIndex = speedOptions.indexOf(playbackRate);
    const nextRate = speedOptions[(currentIndex + 1) % speedOptions.length];
    setPlaybackRate(nextRate);
    if (audioRef.current) audioRef.current.playbackRate = nextRate;
  };

  const seekFromPointer = (event: React.PointerEvent<HTMLDivElement>) => {
    const bounds = event.currentTarget.getBoundingClientRect();
    if (bounds.width <= 0) return;
    seekTo(((event.clientX - bounds.left) / bounds.width) * duration);
  };

  const handleSeekKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'ArrowRight') { event.preventDefault(); seekTo(currentTime + 5); }
    else if (event.key === 'ArrowLeft') { event.preventDefault(); seekTo(currentTime - 5); }
    else if (event.key === 'Home') { event.preventDefault(); seekTo(0); }
    else if (event.key === 'End') { event.preventDefault(); seekTo(duration); }
  };

  return (
    <div className="w-full min-w-0">
      <audio
        ref={audioRef}
        src={src}
        preload="metadata"
        aria-hidden="true"
        className="sr-only"
        onLoadedMetadata={event => setActualDuration(Number.isFinite(event.currentTarget.duration) ? event.currentTarget.duration : durationSeconds || 0)}
        onDurationChange={event => setActualDuration(Number.isFinite(event.currentTarget.duration) ? event.currentTarget.duration : durationSeconds || 0)}
        onTimeUpdate={event => setCurrentTime(event.currentTarget.currentTime)}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onEnded={event => { setIsPlaying(false); setCurrentTime(event.currentTarget.duration || duration); }}
        onError={onError}
      />
      <div className="flex min-w-0 items-center gap-3">
        <button
          type="button"
          onClick={togglePlayback}
          aria-label={isPlaying ? 'Pause voice message' : 'Play voice message'}
          className={`grid h-12 w-12 shrink-0 place-items-center rounded-full transition-transform duration-100 active:scale-95 ${fromMe ? 'bg-white text-zinc-950' : 'bg-zinc-950 text-white dark:bg-zinc-100 dark:text-zinc-950'}`}
        >
          {isPlaying ? <Pause className="h-5 w-5 fill-current" /> : <Play className="ml-0.5 h-5 w-5 fill-current" />}
        </button>
        <div className="min-w-0 flex-1">
          <div
            role="slider"
            tabIndex={0}
            aria-label="Audio progress"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(progress * 100)}
            aria-valuetext={`${formatPlaybackTime(currentTime)} of ${formatPlaybackTime(duration)}`}
            onPointerDown={seekFromPointer}
            onKeyDown={handleSeekKeyDown}
            className="flex h-9 min-w-0 cursor-pointer items-center gap-[3px] rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-500"
          >
            {VOICE_WAVEFORM.map((height, index) => (
              <span
                key={index}
                aria-hidden="true"
                className={`w-[3px] shrink-0 rounded-full transition-colors duration-150 ${((index + 1) / VOICE_WAVEFORM.length) <= progress ? (fromMe ? 'bg-white/95' : 'bg-zinc-800 dark:bg-zinc-200') : (fromMe ? 'bg-white/30' : 'bg-zinc-300 dark:bg-zinc-600')}`}
                style={{ height: `${height}px` }}
              />
            ))}
          </div>
          <p className={`mt-0.5 text-xs tabular-nums ${fromMe ? 'text-zinc-300 dark:text-zinc-500' : 'text-zinc-500 dark:text-zinc-400'}`}>
            {formatPlaybackTime(currentTime)} / {formatPlaybackTime(duration)}
          </p>
        </div>
        <button
          type="button"
          onClick={cycleSpeed}
          aria-label={`Playback speed ${playbackRate}x`}
          title="Change playback speed"
          className={`h-10 min-w-12 shrink-0 rounded-full border px-2 text-xs font-semibold transition-colors ${fromMe ? 'border-white/25 hover:bg-white/10' : 'border-zinc-300 hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800'}`}
        >
          {playbackRate}x
        </button>
      </div>
      {playbackError && <p role="status" className="mt-1 text-xs text-zinc-500">Audio could not start. Try again.</p>}
    </div>
  );
};

export const ChatThread: React.FC<ChatThreadProps> = ({ chatId, title, subtitle, avatarUrl, user, onBack }) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [isUploadingMedia, setIsUploadingMedia] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [liveNotice, setLiveNotice] = useState<string | null>(null);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [voiceDraft, setVoiceDraft] = useState<VoiceDraft | null>(null);
  const [mediaUrls, setMediaUrls] = useState<Record<string, string>>({});
  const [showDetails, setShowDetails] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [isComposerFocused, setIsComposerFocused] = useState(false);
  const [syraProcessingRequests, setSyraProcessingRequests] = useState<Record<string, string>>({});
  const isSyraTyping = Object.keys(syraProcessingRequests).length > 0;
  const [mediaGallery, setMediaGallery] = useState<{ message: ChatMessage; attachments: MediaAttachment[]; caption: string } | null>(null);
  const [isComposerSheetOpen, setIsComposerSheetOpen] = useState(false);
  const [pollVotes, setPollVotes] = useState<MessagePollVote[]>([]);
  const [votingPollMessageIds, setVotingPollMessageIds] = useState<Record<string, boolean>>({});
  const [replyPreviews, setReplyPreviews] = useState<Record<string, ChatMessage | null>>({});
  const [viewport, setViewport] = useState(() => ({
    height: typeof window === 'undefined' ? 1 : window.visualViewport?.height || window.innerHeight,
    offsetTop: typeof window === 'undefined' ? 0 : window.visualViewport?.offsetTop || 0,
  }));
  const messagesScrollRef = useRef<HTMLDivElement>(null);
  const shouldStickToBottomRef = useRef(true);
  const hasInitialScrollRef = useRef(false);
  const currentScrollChatIdRef = useRef(chatId);
  if (currentScrollChatIdRef.current !== chatId) {
    currentScrollChatIdRef.current = chatId;
    hasInitialScrollRef.current = false;
  }
  const scrollToLatest = useCallback((behavior: ScrollBehavior = 'smooth') => {
    const scroller = messagesScrollRef.current;
    if (!scroller) return;
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    if (typeof scroller.scrollTo === 'function') {
      scroller.scrollTo({ top: scroller.scrollHeight, behavior: reduceMotion ? 'auto' : behavior });
    } else {
      scroller.scrollTop = scroller.scrollHeight;
    }
  }, []);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const documentInputRef = useRef<HTMLInputElement>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const chunksRef = useRef<BlobPart[]>([]);
  const recordingStartRef = useRef(0);
  const discardRecordingRef = useRef(false);

  useEffect(() => {
    const visualViewport = window.visualViewport;
    const updateViewport = () => {
      const next = {
        height: Math.max(1, visualViewport?.height || window.innerHeight),
        offsetTop: Math.max(0, visualViewport?.offsetTop ?? 0),
      };
      setViewport(current => Math.abs(current.height - next.height) < 1 && Math.abs(current.offsetTop - next.offsetTop) < 1 ? current : next);
    };
    updateViewport();
    visualViewport?.addEventListener('resize', updateViewport);
    visualViewport?.addEventListener('scroll', updateViewport);
    window.addEventListener('resize', updateViewport);
    return () => {
      visualViewport?.removeEventListener('resize', updateViewport);
      visualViewport?.removeEventListener('scroll', updateViewport);
      window.removeEventListener('resize', updateViewport);
    };
  }, []);

  useEffect(() => {
    if (!shouldStickToBottomRef.current || isLoading) return;
    if (!hasInitialScrollRef.current) {
      hasInitialScrollRef.current = true;
      scrollToLatest('auto');
      return;
    }
    scrollToLatest('smooth');
  }, [messages, isLoading, isSyraTyping, viewport.height, scrollToLatest]);

  useEffect(() => {
    let active = true;
    let historyLoaded = false;
    setMessages([]);
    setReplyPreviews({});
    setPollVotes([]);
    setVotingPollMessageIds({});
    setIsComposerSheetOpen(false);
    shouldStickToBottomRef.current = true;
    hasInitialScrollRef.current = false;
    setIsLoading(true);
    setError(null);
    setLiveNotice(null);

    const updateReadReceipts = async () => {
      try {
        const { error: deliveredError } = await supabase.rpc('mark_messages_as_delivered', { p_chat_id: chatId });
        const { error: readError } = await supabase.rpc('mark_messages_as_read', { p_chat_id: chatId, p_user_id: user.id });
        if (active && (deliveredError || readError)) setLiveNotice('Read receipts could not be updated.');
      } catch {
        if (active) setLiveNotice('Read receipts could not be updated.');
      }
    };

    const loadHistory = async () => {
      if (historyLoaded) return;
      historyLoaded = true;
      try {
        const { data, error: queryError } = await supabase
          .from('messages')
          .select(MESSAGE_FIELDS)
          .eq('chat_id', chatId)
          .eq('is_deleted', false)
          .or(`expires_at.is.null,expires_at.gt.${new Date().toISOString()}`)
          .order('created_at', { ascending: false })
          .limit(100);
        if (queryError) throw queryError;
        if (active) {
          const loaded = ((data || []) as ChatMessage[]).filter(message => visibleToUser(message, user.id)).reverse();
          setMessages(current => {
            const byId = new Map(loaded.map(message => [message.id, message]));
            current.forEach(message => byId.set(message.id, message));
            return [...byId.values()].sort((a, b) => Date.parse(a.created_at || '') - Date.parse(b.created_at || ''));
          });
          void updateReadReceipts();
        }
      } catch (error) {
        if (active) setError(error instanceof Error ? error.message : 'Could not load messages. Please try again.');
      } finally {
        if (active) setIsLoading(false);
      }
    };

    const channel = supabase
      .channel(`web-chat-${chatId}-${user.id}`)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'messages', filter: `chat_id=eq.${chatId}` }, payload => {
        if (!active) return;
        if (payload.eventType === 'DELETE') {
          const deletedId = (payload.old as Partial<ChatMessage>).id;
          if (deletedId) setMessages(current => current.filter(message => message.id !== deletedId));
          return;
        }
        const incoming = payload.new as ChatMessage;
        if (!incoming?.id) return;
        if (payload.eventType === 'INSERT' && incoming.sender_id !== user.id) void updateReadReceipts();
        if (!visibleToUser(incoming, user.id)) {
          setMessages(current => current.filter(message => message.id !== incoming.id));
          return;
        }
        setMessages(current => {
          const optimistic = payload.eventType === 'INSERT' && incoming.sender_id === user.id
            ? current.find(message => message.localStatus === 'sending' && message.sender_id === incoming.sender_id && message.content === incoming.content)
            : undefined;
          const next = current.filter(message => message.id !== incoming.id && message.id !== optimistic?.id);
          next.push(optimistic ? { ...incoming, localStatus: 'sent', animateIn: true } : incoming);
          return next.sort((a, b) => Date.parse(a.created_at || '') - Date.parse(b.created_at || ''));
        });
      })
      .subscribe(status => {
        if (status === 'SUBSCRIBED') void loadHistory();
        if ((status === 'CHANNEL_ERROR' || status === 'TIMED_OUT') && active) {
          setLiveNotice('Live updates are unavailable. Refresh the inbox to check for new messages.');
          void loadHistory();
        }
      });

    return () => {
      active = false;
      void supabase.removeChannel(channel);
    };
  }, [chatId, user.id]);

  useEffect(() => {
    const pollMessageIds = messages
      .filter(message => (message.message_type || '').toLowerCase() === 'poll')
      .map(message => message.id);
    if (!pollMessageIds.length) {
      setPollVotes([]);
      return;
    }
    let active = true;
    void supabase.from('message_poll_votes')
      .select('message_id,chat_id,user_id,option_index')
      .in('message_id', pollMessageIds)
      .then(({ data, error: voteError }) => {
        if (!active) return;
        if (voteError) {
          setLiveNotice('Poll results could not be loaded.');
          return;
        }
        setPollVotes((data || []) as MessagePollVote[]);
        setLiveNotice(current => current === 'Poll results could not be loaded.' ? null : current);
      });
    return () => { active = false; };
  }, [chatId, messages]);

  useEffect(() => {
    let active = true;
    const channel = supabase
      .channel(`chat-poll-votes-${chatId}-${user.id}`)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'message_poll_votes', filter: `chat_id=eq.${chatId}` }, payload => {
        if (!active) return;
        const row = (payload.eventType === 'DELETE' ? payload.old : payload.new) as MessagePollVote;
        if (!row?.message_id || !row?.user_id) return;
        setPollVotes(current => {
          const next = current.filter(vote => !(vote.message_id === row.message_id && vote.user_id === row.user_id));
          if (payload.eventType !== 'DELETE') next.push(row);
          return next;
        });
      })
      .subscribe();
    return () => { active = false; void supabase.removeChannel(channel); };
  }, [chatId, user.id]);

  useEffect(() => {
    let active = true;
    setSyraProcessingRequests({});

    const loadProcessingRequests = async () => {
      const since = new Date(Date.now() - SYRA_TYPING_MAX_AGE_MS).toISOString();
      const { data, error: queryError } = await supabase
        .from('syra_invocations')
        .select('request_id,status,user_id,chat_id,created_at')
        .eq('chat_id', chatId)
        .eq('user_id', user.id)
        .eq('status', 'processing')
        .gte('created_at', since);
      if (!active) return;
      if (queryError) {
        setSyraProcessingRequests({});
        setLiveNotice("Syra's response status could not be loaded.");
        return;
      }
      setSyraProcessingRequests(collectActiveSyraRequests((data || []) as SyraInvocationStatusRow[], user.id, chatId));
      setLiveNotice(current => current === "Syra's response status could not be loaded." ? null : current);
    };

    const channel = supabase
      .channel(`syra-response-status-${chatId}-${user.id}`)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'syra_invocations', filter: `chat_id=eq.${chatId}` }, payload => {
        if (!active) return;
        const row = (payload.eventType === 'DELETE' ? payload.old : payload.new) as Partial<SyraInvocationStatusRow>;
        if (!row.request_id || row.user_id !== user.id || row.chat_id !== chatId) return;
        setSyraProcessingRequests(current => {
          if (payload.eventType !== 'DELETE' && isFreshSyraInvocation(row, user.id, chatId)) {
            return { ...current, [row.request_id!]: row.created_at! };
          }
          const next = { ...current };
          delete next[row.request_id!];
          return next;
        });
      })
      .subscribe(status => {
        if (status === 'SUBSCRIBED') void loadProcessingRequests();
        if ((status === 'CHANNEL_ERROR' || status === 'TIMED_OUT') && active) {
          setSyraProcessingRequests({});
          setLiveNotice("Syra's live response status is unavailable.");
        }
      });

    const staleStatusTimer = window.setInterval(() => {
      setSyraProcessingRequests(current => Object.fromEntries(
        Object.entries(current).filter(([, createdAt]) => isRecentSyraProcessingTimestamp(createdAt)),
      ));
    }, 10_000);

    return () => {
      active = false;
      window.clearInterval(staleStatusTimer);
      void supabase.removeChannel(channel);
    };
  }, [chatId, user.id]);

  useEffect(() => {
    const pendingIds = Array.from(new Set(messages
      .map(message => message.reply_to_id)
      .filter((id): id is string => typeof id === 'string' && id.length > 0 && !Object.prototype.hasOwnProperty.call(replyPreviews, id))));
    if (!pendingIds.length) return;
    let active = true;
    void supabase.from('messages').select(MESSAGE_FIELDS).eq('chat_id', chatId).in('id', pendingIds).then(({ data, error: queryError }) => {
      if (!active) return;
      if (queryError) setLiveNotice('Some reply context could not be loaded.');
      const fetched = new Map(((data || []) as ChatMessage[]).map(message => [message.id, message]));
      setReplyPreviews(current => {
        const next = { ...current };
        pendingIds.forEach(id => { next[id] = fetched.get(id) || null; });
        return next;
      });
    });
    return () => { active = false; };
  }, [chatId, messages, replyPreviews]);

  useEffect(() => {
    let active = true;
    void supabase.from('chat_participants').select('is_muted').eq('chat_id', chatId).eq('user_id', user.id).maybeSingle().then(({ data, error: muteError }) => {
      if (!active) return;
      if (muteError) setLiveNotice('Notification preferences could not be loaded.');
      else setIsMuted(Boolean(data?.is_muted));
    });
    return () => { active = false; };
  }, [chatId, user.id]);

  useEffect(() => {
    if (!isRecording) return;
    const timer = window.setInterval(() => {
      const elapsed = Math.floor((Date.now() - recordingStartRef.current) / 1000);
      setRecordingSeconds(elapsed);
      if (elapsed >= 300 && recorderRef.current?.state === 'recording') recorderRef.current.stop();
    }, 500);
    return () => window.clearInterval(timer);
  }, [isRecording]);

  useEffect(() => () => {
    streamRef.current?.getTracks().forEach(track => track.stop());
    if (voiceDraft?.url) URL.revokeObjectURL(voiceDraft.url);
  }, [voiceDraft?.url]);

  const refreshMediaUrl = async (message: ChatMessage, attachmentIndex?: number) => {
    const items = getMessageMediaAttachments(message);
    const signedUrl = typeof attachmentIndex === 'number' ? items[attachmentIndex]?.url : message.media_url || items[0]?.url;
    const path = getObjectPathFromSignedUrl(signedUrl || null);
    if (!path) return;
    const { data, error: signingError } = await supabase.storage.from(MEDIA_BUCKET).createSignedUrl(path, SIGNED_URL_SECONDS);
    if (signingError || !data?.signedUrl) {
      setError('This media link could not be refreshed. It may no longer be available.');
      return;
    }
    const cacheKey = typeof attachmentIndex === 'number' ? `${message.id}:${attachmentIndex}` : message.id;
    setMediaUrls(current => ({ ...current, [cacheKey]: data.signedUrl }));
  };

  const sendText = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const text = draft.trim();
    if (!text || isSending || isUploadingMedia) return;
    setIsSending(true);
    setError(null);
    const localId = `local-${crypto.randomUUID()}`;
    const localMessage: ChatMessage = {
      id: localId,
      chat_id: chatId,
      sender_id: user.id,
      reply_to_id: null,
      content: text,
      message_type: 'text',
      content_state: 'active',
      created_at: new Date().toISOString(),
      is_deleted: false,
      expires_at: null,
      deleted_for_users: [],
      media_url: null,
      attachments: null,
      delivery_status: 'sending',
      message_state: 'sending',
      localStatus: 'sending',
      animateIn: true,
    };
    shouldStickToBottomRef.current = true;
    setDraft('');
    setMessages(current => [...current, localMessage]);
    try {
      const { data, error: insertError } = await supabase
        .from('messages')
        .insert({ chat_id: chatId, sender_id: user.id, content: text, message_type: 'text', content_state: 'active' })
        .select(MESSAGE_FIELDS)
        .single();
      if (insertError) throw insertError;
      if (!data) throw new Error('The message was not returned by the server.');
      const sent = { ...(data as ChatMessage), localStatus: 'sent' as const, animateIn: true };
      setMessages(current => [...current.filter(message => message.id !== localId && message.id !== sent.id), sent].sort((a, b) => Date.parse(a.created_at || '') - Date.parse(b.created_at || '')));
    } catch (error) {
      setMessages(current => current.map(message => message.id === localId ? { ...message, localStatus: 'failed', delivery_status: 'failed', message_state: 'failed' } : message));
      setDraft(current => current || text);
      setError(error instanceof Error ? error.message : 'Could not send this message. Please try again.');
    } finally {
      setIsSending(false);
    }
  };

  const sendTypedMessage = async (draftMessage: ChatTypedMessageDraft): Promise<boolean> => {
    if (isSending || isUploadingMedia) return false;
    setIsSending(true);
    setError(null);
    try {
      const { data, error: insertError } = await supabase
        .from('messages')
        .insert({
          chat_id: chatId,
          sender_id: user.id,
          content: draftMessage.content,
          message_type: draftMessage.type,
          metadata: draftMessage.metadata,
          content_state: 'active',
        })
        .select(MESSAGE_FIELDS)
        .single();
      if (insertError) throw insertError;
      if (!data) throw new Error('The message was not returned by the server.');
      const sent = { ...(data as ChatMessage), localStatus: 'sent' as const, animateIn: true };
      shouldStickToBottomRef.current = true;
      setMessages(current => current.some(message => message.id === sent.id) ? current : [...current, sent].sort((a, b) => Date.parse(a.created_at || '') - Date.parse(b.created_at || '')));
      return true;
    } catch (sendError) {
      setError(sendError instanceof Error ? sendError.message : 'Could not send this message. Please try again.');
      return false;
    } finally {
      setIsSending(false);
    }
  };

  const castPollVote = async (message: ChatMessage, optionIndex: number) => {
    if (votingPollMessageIds[message.id]) return;
    const metadata = isRecord(message.metadata) ? message.metadata : {};
    const poll = isRecord(metadata.poll) ? metadata.poll : metadata;
    const options = Array.isArray(poll.options) ? poll.options : [];
    if (!Number.isInteger(optionIndex) || optionIndex < 0 || optionIndex >= options.length) return;
    const endsAt = typeof poll.ends_at === 'string' ? Date.parse(poll.ends_at) : Number.NaN;
    if (Number.isFinite(endsAt) && endsAt <= Date.now()) {
      setError('This poll is closed.');
      return;
    }
    if (pollVotes.some(vote => vote.message_id === message.id && vote.user_id === user.id)) return;

    const optimisticVote: MessagePollVote = { message_id: message.id, chat_id: message.chat_id, user_id: user.id, option_index: optionIndex };
    setVotingPollMessageIds(current => ({ ...current, [message.id]: true }));
    setPollVotes(current => [...current.filter(vote => !(vote.message_id === message.id && vote.user_id === user.id)), optimisticVote]);
    setError(null);
    try {
      const { error: voteError } = await supabase.from('message_poll_votes').upsert({
        message_id: message.id,
        chat_id: message.chat_id,
        user_id: user.id,
        option_index: optionIndex,
      }, { onConflict: 'message_id,user_id' });
      if (voteError) throw voteError;
    } catch (voteError) {
      setPollVotes(current => current.filter(vote => !(vote.message_id === message.id && vote.user_id === user.id)));
      setError(voteError instanceof Error ? voteError.message : 'Your poll vote could not be saved. Please try again.');
    } finally {
      setVotingPollMessageIds(current => {
        const next = { ...current };
        delete next[message.id];
        return next;
      });
    }
  };

  const sendMedia = async (file: File, durationSeconds?: number): Promise<boolean> => {
    if (isUploadingMedia || file.size <= 0) return false;
    if (file.size > MAX_MEDIA_BYTES) {
      setError('That file is over the 25 MB chat limit. Choose a smaller file.');
      return false;
    }
    const type = mediaTypeFor(file);
    if (!isSupportedMediaFile(file)) {
      setError('Choose an image, video, audio recording, or PDF.');
      return false;
    }
    setIsUploadingMedia(true);
    setError(null);
    const objectPath = `${chatId}/${user.id}/${crypto.randomUUID()}.${extensionFor(file, type)}`;
    let uploaded = false;
    try {
      const videoMetadata = type === 'video' ? await readVideoMetadata(file) : {};
      const { error: uploadError } = await supabase.storage.from(MEDIA_BUCKET).upload(objectPath, file, {
        contentType: file.type || 'application/octet-stream',
        cacheControl: '3600',
        upsert: false,
      });
      if (uploadError) throw uploadError;
      uploaded = true;
      const { data: signed, error: signedError } = await supabase.storage.from(MEDIA_BUCKET).createSignedUrl(objectPath, SIGNED_URL_SECONDS);
      if (signedError || !signed?.signedUrl) throw signedError || new Error('Could not create a media link.');
      const attachment = { url: signed.signedUrl, type, size: file.size, name: file.name, mimeType: file.type, ...videoMetadata, ...(durationSeconds ? { duration: durationSeconds } : {}) };
      const content = type === 'audio' ? 'Voice message' : type === 'file' ? file.name : '';
      const { data, error: insertError } = await supabase.from('messages').insert({
        chat_id: chatId,
        sender_id: user.id,
        content,
        message_type: type,
        media_url: signed.signedUrl,
        attachments: [attachment],
        content_state: 'active',
      }).select(MESSAGE_FIELDS).single();
      if (insertError) throw insertError;
      if (data) {
        shouldStickToBottomRef.current = true;
        const sent = data as ChatMessage;
        setMediaUrls(current => ({ ...current, [sent.id]: signed.signedUrl }));
        setMessages(current => current.some(message => message.id === sent.id) ? current : [...current, sent].sort((a, b) => Date.parse(a.created_at || '') - Date.parse(b.created_at || '')));
      }
      return true;
    } catch (error) {
      if (uploaded) await supabase.storage.from(MEDIA_BUCKET).remove([objectPath]);
      setError(error instanceof Error ? error.message : 'Could not send this attachment. Please try again.');
      return false;
    } finally {
      setIsUploadingMedia(false);
    }
  };

  const sendMediaBatch = async (files: File[]): Promise<boolean> => {
    if (files.length <= 1) return files[0] ? sendMedia(files[0]) : false;
    if (isUploadingMedia) return false;
    if (files.length > MAX_BATCH_MEDIA_ITEMS) {
      setError(`Choose no more than ${MAX_BATCH_MEDIA_ITEMS} items in one media batch.`);
      return false;
    }
    if (files.some(file => file.size <= 0 || !isSupportedMediaFile(file))) {
      setError('Choose non-empty images, videos, audio files, or PDFs.');
      return false;
    }
    if (files.some(file => file.size > MAX_MEDIA_BYTES)) {
      setError('Each attachment must be 25 MB or smaller.');
      return false;
    }
    const totalBytes = files.reduce((total, file) => total + file.size, 0);
    if (totalBytes > MAX_BATCH_MEDIA_BYTES) {
      setError('A media batch must be 100 MB or smaller in total.');
      return false;
    }

    setIsUploadingMedia(true);
    setError(null);
    const uploadedPaths: string[] = [];
    try {
      const metadata = await Promise.all(files.map(file => mediaTypeFor(file) === 'video' ? readVideoMetadata(file) : Promise.resolve({})));
      const attachments: MediaAttachment[] = [];
      for (let index = 0; index < files.length; index += 1) {
        const file = files[index];
        const type = mediaTypeFor(file);
        const objectPath = `${chatId}/${user.id}/${crypto.randomUUID()}.${extensionFor(file, type)}`;
        const { error: uploadError } = await supabase.storage.from(MEDIA_BUCKET).upload(objectPath, file, {
          contentType: file.type || 'application/octet-stream',
          cacheControl: '3600',
          upsert: false,
        });
        if (uploadError) throw uploadError;
        uploadedPaths.push(objectPath);

        const { data: signed, error: signedError } = await supabase.storage.from(MEDIA_BUCKET).createSignedUrl(objectPath, SIGNED_URL_SECONDS);
        if (signedError || !signed?.signedUrl) throw signedError || new Error('Could not create a media link.');
        attachments.push({
          url: signed.signedUrl,
          type,
          size: file.size,
          name: file.name,
          mimeType: file.type,
          ...metadata[index],
        });
      }

      const primary = attachments[0];
      const content = primary.type === 'audio' ? 'Audio message' : primary.type === 'file' ? primary.name || 'Attachment' : '';
      const { data, error: insertError } = await supabase.from('messages').insert({
        chat_id: chatId,
        sender_id: user.id,
        content,
        message_type: primary.type,
        media_url: primary.url,
        attachments,
        content_state: 'active',
      }).select(MESSAGE_FIELDS).single();
      if (insertError) throw insertError;
      if (!data) throw new Error('The media batch was not returned by the server.');

      const sent = data as ChatMessage;
      shouldStickToBottomRef.current = true;
      setMediaUrls(current => {
        const next = { ...current, [sent.id]: primary.url };
        attachments.forEach((attachment, index) => { next[`${sent.id}:${index}`] = attachment.url; });
        return next;
      });
      setMessages(current => current.some(message => message.id === sent.id) ? current : [...current, sent].sort((a, b) => Date.parse(a.created_at || '') - Date.parse(b.created_at || '')));
      return true;
    } catch (error) {
      if (uploadedPaths.length) {
        try {
          await supabase.storage.from(MEDIA_BUCKET).remove(uploadedPaths);
        } catch {
          // Leave the send error visible even if storage cleanup also fails.
        }
      }
      setError(error instanceof Error ? error.message : 'Could not send this media batch. Please try again.');
      return false;
    } finally {
      setIsUploadingMedia(false);
    }
  };

  const startRecording = async () => {
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
      setError('Voice recording is not supported in this browser.');
      return;
    }
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      chunksRef.current = [];
      discardRecordingRef.current = false;
      const mimeType = ['audio/webm;codecs=opus', 'audio/ogg;codecs=opus', 'audio/mp4'].find(candidate => MediaRecorder.isTypeSupported(candidate));
      const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
      recorderRef.current = recorder;
      recordingStartRef.current = Date.now();
      recorder.ondataavailable = event => { if (event.data.size > 0) chunksRef.current.push(event.data); };
      recorder.onerror = () => setError('The voice recording stopped unexpectedly. Please try again.');
      recorder.onstop = () => {
        stream.getTracks().forEach(track => track.stop());
        streamRef.current = null;
        setIsRecording(false);
        if (discardRecordingRef.current) {
          discardRecordingRef.current = false;
          chunksRef.current = [];
          setRecordingSeconds(0);
          return;
        }
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || 'audio/webm' });
        chunksRef.current = [];
        if (blob.size === 0) {
          setError('No audio was captured. Please try recording again.');
          setRecordingSeconds(0);
          return;
        }
        const url = URL.createObjectURL(blob);
        setVoiceDraft({ blob, url, durationSeconds: Math.max(1, Math.round((Date.now() - recordingStartRef.current) / 1000)) });
      };
      recorder.start(500);
      setRecordingSeconds(0);
      setIsRecording(true);
    } catch (error) {
      streamRef.current?.getTracks().forEach(track => track.stop());
      streamRef.current = null;
      setError(error instanceof Error ? error.message : 'Microphone permission is required to record a voice message.');
    }
  };

  const stopRecording = () => {
    if (recorderRef.current?.state === 'recording') recorderRef.current.stop();
  };

  const discardRecording = () => {
    discardRecordingRef.current = true;
    if (recorderRef.current?.state === 'recording') recorderRef.current.stop();
    else {
      streamRef.current?.getTracks().forEach(track => track.stop());
      streamRef.current = null;
      setIsRecording(false);
      setRecordingSeconds(0);
    }
  };

  const sendVoiceDraft = async () => {
    if (!voiceDraft || isUploadingMedia) return;
    const mimeType = voiceDraft.blob.type || 'audio/webm';
    const extension = mimeType.includes('ogg') ? 'ogg' : mimeType.includes('mp4') ? 'm4a' : 'webm';
    const file = new File([voiceDraft.blob], `voice-note.${extension}`, { type: mimeType });
    const sent = await sendMedia(file, voiceDraft.durationSeconds);
    if (sent) {
      URL.revokeObjectURL(voiceDraft.url);
      setVoiceDraft(null);
      setRecordingSeconds(0);
    }
  };

  const copyConversationId = async () => {
    try {
      await navigator.clipboard.writeText(chatId);
      setShowDetails(false);
    } catch {
      setError('Could not copy the conversation id in this browser.');
    }
  };

  const toggleMute = async () => {
    const nextMuted = !isMuted;
    const { error: muteError } = await supabase.from('chat_participants').update({ is_muted: nextMuted }).eq('chat_id', chatId).eq('user_id', user.id);
    if (muteError) {
      setError('Could not update notification settings for this conversation.');
      return;
    }
    setIsMuted(nextMuted);
    setShowDetails(false);
  };

  const handleMediaError = (message: ChatMessage) => { void refreshMediaUrl(message); };

  const renderMessage = (message: ChatMessage, index: number) => {
    const previous = messages[index - 1];
    const next = messages[index + 1];
    const sameAsPrevious = Boolean(previous && previous.sender_id === message.sender_id);
    const sameAsNext = Boolean(next && next.sender_id === message.sender_id);
    const fromMe = message.sender_id === user.id;
    const deliveryState = getMessageDeliveryState(message);
    const quotedMessageLoaded = Boolean(message.reply_to_id && Object.prototype.hasOwnProperty.call(replyPreviews, message.reply_to_id));
    const quotedMessage = message.reply_to_id ? replyPreviews[message.reply_to_id] : undefined;
    const quotedEncrypted = Boolean(quotedMessage && (quotedMessage.content_state === 'encrypted' || isEncryptedMessageContent(quotedMessage.content)));
    const quotedType = (quotedMessage?.message_type || 'text').toLowerCase();
    const quotedText = !quotedMessageLoaded ? 'Loading quoted message…' : !quotedMessage || !visibleToUser(quotedMessage, user.id) ? 'Original message unavailable' : quotedEncrypted ? 'End-to-end encrypted message' : quotedType === 'image' ? 'Photo' : quotedType === 'video' ? 'Video' : quotedType === 'audio' ? 'Voice message' : quotedType === 'file' ? 'Attachment' : getMessageDisplayText(quotedMessage.content).trim() || 'Message';
    const encrypted = message.content_state === 'encrypted' || isEncryptedMessageContent(message.content);
    const text = getMessageDisplayText(message.content);
    const type = (message.message_type || 'text').toLowerCase();
    const mediaItems = getMessageMediaAttachments(message);
    const isBatchMedia = !encrypted && (mediaItems.length > 1 || type === 'media_group');
    const isTypedCard = ['contact', 'location', 'link_preview', 'poll', 'code_snippet'].includes(type);
    const attachment = mediaItems[0] || null;
    const mediaUrl = mediaUrls[message.id] || message.media_url;
    const displayUrl = mediaUrl || attachment?.url || null;
    const fileName = typeof attachment?.name === 'string' && attachment.name.trim() ? attachment.name : text || 'Shared file';
    const fileMetadata = [formatFileSize(attachment?.size), fileTypeLabel(fileName, attachment?.mimeType)].filter(Boolean).join(' · ');
    let body: React.ReactNode = null;
    if (!encrypted) {
      if (isBatchMedia && mediaItems.length > 0) {
        body = <BatchMediaDeck
          key={message.id}
          attachments={mediaItems}
          getUrl={(item, mediaIndex) => mediaUrls[`${message.id}:${mediaIndex}`] || (mediaIndex === 0 ? mediaUrls[message.id] : undefined) || item.url}
          onMediaError={mediaIndex => { void refreshMediaUrl(message, mediaIndex); }}
          onOpenGallery={() => setMediaGallery({ message, attachments: mediaItems, caption: text })}
        />;
      } else if (isTypedCard) {
        body = <ChatMessageCards
          messageId={message.id}
          messageType={type}
          metadata={message.metadata}
          pollVotes={pollVotes.filter(vote => vote.message_id === message.id)}
          userId={user.id}
          isVoting={Boolean(votingPollMessageIds[message.id])}
          onVote={optionIndex => { void castPollVote(message, optionIndex); }}
        />;
      } else if (type === 'audio' && displayUrl) {
        body = <VoiceMessagePlayer src={displayUrl} durationSeconds={typeof attachment?.duration === 'number' ? attachment.duration : undefined} fromMe={fromMe} onError={() => handleMediaError(message)} />;
      } else if (type === 'image' && displayUrl) {
        body = <img src={displayUrl} alt={attachment?.name || 'Chat attachment'} onError={() => handleMediaError(message)} className="max-h-80 max-w-full rounded-xl object-cover" />;
      } else if (type === 'video' && displayUrl) {
        body = <video controls playsInline preload="metadata" src={displayUrl} onError={() => handleMediaError(message)} className="max-h-80 max-w-full rounded-xl" />;
      } else if (type === 'file' && displayUrl) {
        body = (
          <div className={`flex w-[min(70vw,24rem)] max-w-full items-center gap-3 rounded-2xl border p-3 ${fromMe ? 'border-white/15 bg-white/10' : 'border-zinc-200 bg-zinc-50 dark:border-zinc-700 dark:bg-zinc-800/70'}`}>
            <span className={`grid h-12 w-12 shrink-0 place-items-center rounded-xl border ${fromMe ? 'border-white/20 bg-white/10' : 'border-zinc-200 bg-white dark:border-zinc-700 dark:bg-zinc-900'}`}>
              <FileText className="h-6 w-6 opacity-75" />
            </span>
            <div className="min-w-0 flex-1">
              <p title={fileName} className="truncate text-sm font-semibold">{fileName}</p>
              <p className={`mt-1 text-xs ${fromMe ? 'text-zinc-300 dark:text-zinc-500' : 'text-zinc-500 dark:text-zinc-400'}`}>{fileMetadata || 'Shared file'}</p>
            </div>
            <a href={displayUrl} download={fileName} target="_blank" rel="noreferrer" aria-label={`Open or download ${fileName}`} title="Open or download file" className={`grid h-11 w-11 shrink-0 place-items-center rounded-full border transition-colors ${fromMe ? 'border-white/20 hover:bg-white/10' : 'border-zinc-300 hover:bg-white dark:border-zinc-600 dark:hover:bg-zinc-700'}`}>
              <Download className="h-5 w-5" />
            </a>
          </div>
        );
      } else if (type !== 'text') {
        body = <div className="flex items-center gap-2 text-sm"><Image className="h-4 w-4" />This attachment isn’t available in this web view.</div>;
      } else {
        body = <p className="whitespace-pre-wrap break-words text-sm leading-6">{text || ' '}</p>;
      }
    }
    const bubbleClasses = [
      'max-w-[min(86vw,34rem)] rounded-2xl',
      isBatchMedia ? 'chat-batch-media-message overflow-visible bg-transparent px-0 py-0 shadow-none text-zinc-900 dark:text-zinc-100' : 'overflow-hidden px-3 py-3 shadow-sm',
      isBatchMedia ? '' : fromMe ? 'rounded-br-md bg-zinc-950 text-white dark:bg-zinc-100 dark:text-zinc-950' : 'rounded-bl-md border border-zinc-200 bg-white text-zinc-900 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100',
      !isBatchMedia && sameAsPrevious ? (fromMe ? 'rounded-tr-md' : 'rounded-tl-md') : '',
    ].filter(Boolean).join(' ');

    const encryptedClasses = [
      'max-w-[85%] rounded-2xl border border-zinc-300 bg-white px-4 py-3 text-zinc-700 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-300',
      sameAsPrevious ? (fromMe ? 'rounded-tr-md' : 'rounded-tl-md') : '',
    ].filter(Boolean).join(' ');
    const replyPreview = message.reply_to_id ? (
      <div className={`mb-2 flex max-w-full items-stretch gap-2 rounded-xl px-2.5 py-2 ${isBatchMedia ? 'border border-zinc-200 bg-white dark:border-zinc-700 dark:bg-zinc-900' : fromMe ? 'bg-white/10' : 'bg-black/[0.04] dark:bg-white/[0.06]'}`}>
        <span aria-hidden="true" className={`w-1 shrink-0 rounded-full ${fromMe ? 'bg-white/55' : 'bg-zinc-500 dark:bg-zinc-400'}`} />
        <div className="min-w-0">
          <p className="text-[11px] font-semibold opacity-85">Replying to {quotedMessage?.sender_id === user.id ? 'You' : title}</p>
          <p className="mt-0.5 max-h-9 overflow-hidden break-words text-xs leading-4 opacity-80">{quotedText}</p>
        </div>
      </div>
    ) : null;
    return (
      <div key={message.id} className={['flex', message.animateIn ? 'chat-message-send-enter' : '', index > 0 ? (sameAsPrevious ? 'mt-1' : 'mt-4') : '', fromMe ? 'justify-end' : 'justify-start'].filter(Boolean).join(' ')}>
        {encrypted ? (
          <div className={encryptedClasses}>
            {replyPreview}
            <div className="flex items-start gap-3"><LockKeyhole className="mt-0.5 h-4 w-4 shrink-0 text-zinc-500" /><div><p className="text-sm font-medium">End-to-end encrypted message</p><p className="mt-1 text-xs leading-5 text-zinc-500 dark:text-zinc-400">This message can’t be read in the web app. Open Synapse on Android to view it.</p></div></div>
            {fromMe && !sameAsNext && <div className="mt-2 flex justify-end text-[10px] text-zinc-500 dark:text-zinc-400"><DeliveryIndicator state={deliveryState} /></div>}
          </div>
        ) : (
          <div className={bubbleClasses}>
            {replyPreview}
            {body}
            {!sameAsNext && <div className={['mt-2 flex items-center justify-end gap-2 text-[10px]', isBatchMedia ? 'text-zinc-500 dark:text-zinc-400' : fromMe ? 'text-zinc-300 dark:text-zinc-500' : 'text-zinc-500 dark:text-zinc-400'].join(' ')}>{messageTime(message.created_at) && <time>{messageTime(message.created_at)}</time>}{fromMe && <DeliveryIndicator state={deliveryState} />}</div>}
          </div>
        )}
      </div>
    );
  };

  return (
    <div style={{ top: `${viewport.offsetTop}px`, height: `${viewport.height}px` }} className="fixed inset-x-0 z-40 flex h-[100dvh] min-h-0 w-full flex-col overflow-hidden overscroll-none bg-zinc-50 dark:bg-zinc-950">
      <header className="relative z-20 flex h-16 shrink-0 items-center gap-3 border-b border-zinc-200 bg-white px-4 dark:border-zinc-800 dark:bg-zinc-900 sm:px-6">
        <Button type="button" variant="ghost" onClick={onBack} aria-label="Back to inbox" className="h-10 w-10 shrink-0 rounded-full p-0 text-zinc-600 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800"><ArrowLeft className="h-5 w-5" /></Button>
        <ChatAvatar title={title} url={avatarUrl} />
        <div className="min-w-0 flex-1"><h3 className="truncate text-sm font-semibold text-zinc-950 dark:text-zinc-100">{title}</h3><p className="truncate text-xs text-zinc-500 dark:text-zinc-400">{subtitle}</p></div>
        <span className="hidden shrink-0 items-center gap-1 rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wide text-amber-900 sm:inline-flex"><LockKeyhole className="h-3 w-3" />Not E2EE</span>
        <Button type="button" variant="ghost" onClick={() => setShowDetails(value => !value)} aria-label="Conversation options" aria-expanded={showDetails} className="h-10 w-10 shrink-0 rounded-full p-0 text-zinc-600 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800"><MoreVertical className="h-5 w-5" /></Button>
        {showDetails && <div className="absolute right-4 top-14 z-30 w-72 rounded-2xl border border-zinc-200 bg-white p-4 shadow-xl dark:border-zinc-700 dark:bg-zinc-900 sm:right-6"><p className="text-sm font-semibold text-zinc-950 dark:text-zinc-100">Conversation options</p><p className="mt-2 text-xs leading-5 text-zinc-600 dark:text-zinc-300">Web messages and attachments are not end-to-end encrypted. E2EE messages from Android can’t be read here.</p><Button type="button" variant="outline" onClick={() => void toggleMute()} className="mt-3 h-9 w-full justify-start rounded-full border-zinc-300 px-3 text-xs dark:border-zinc-700">{isMuted ? <Bell className="mr-2 h-4 w-4" /> : <BellOff className="mr-2 h-4 w-4" />}{isMuted ? 'Unmute notifications' : 'Mute notifications'}</Button><Button type="button" variant="ghost" onClick={() => void copyConversationId()} className="mt-1 h-9 w-full rounded-full px-3 text-xs text-zinc-600 dark:text-zinc-300">Copy conversation ID</Button></div>}
      </header>

      <div role="note" className="flex shrink-0 items-start gap-3 border-b border-amber-200 bg-amber-50 px-4 py-2.5 text-amber-950 sm:px-6">
        <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
        <p className="text-[11px] leading-4"><strong>Web chat is not end-to-end encrypted.</strong> Synapse’s server can access web text and media. Signed media links expire and can be opened by anyone who has the link. Avoid sensitive information. Android E2EE messages appear unreadable here.</p>
      </div>
      {liveNotice && <p role="status" className="shrink-0 border-b border-zinc-200 bg-white px-5 py-2 text-xs text-zinc-600 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300">{liveNotice}</p>}
      {error && <p role="alert" className="shrink-0 border-b border-zinc-200 bg-white px-5 py-2 text-xs text-rose-700 dark:border-zinc-800 dark:bg-zinc-900 dark:text-rose-300">{error}</p>}

      <div className="relative min-h-0 flex-1 overflow-hidden">
      <div ref={messagesScrollRef} onScroll={event => { const el = event.currentTarget; shouldStickToBottomRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < 96; }} role="region" className={`absolute inset-0 overflow-y-auto overscroll-contain px-4 pt-5 ${isRecording || voiceDraft ? 'pb-56' : 'pb-32'} sm:px-6`} aria-live="polite" aria-label="Conversation messages">
        {isLoading ? <div className="space-y-3" aria-label="Loading messages"><div className="h-14 w-2/3 animate-pulse rounded-2xl bg-zinc-200 dark:bg-zinc-800" /><div className="ml-auto h-14 w-1/2 animate-pulse rounded-2xl bg-zinc-200 dark:bg-zinc-800" /><div className="h-14 w-3/5 animate-pulse rounded-2xl bg-zinc-200 dark:bg-zinc-800" /></div> : messages.length === 0 ? (
          <div className="flex h-full min-h-64 flex-col items-center justify-center text-center"><span className="grid h-12 w-12 place-items-center rounded-2xl border border-zinc-200 bg-white text-zinc-600 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-300"><Send className="h-5 w-5" /></span><h4 className="mt-4 font-semibold text-zinc-900 dark:text-zinc-100">Start the conversation</h4><p className="mt-1 max-w-xs text-sm leading-6 text-zinc-500 dark:text-zinc-400">Send a text or voice message. Web messages are not E2EE.</p></div>
        ) : messages.map(renderMessage)}
        {isSyraTyping && <div role="status" aria-label="Syra is typing" className="mt-4 flex justify-start">
          <div className="inline-flex items-center gap-3 rounded-2xl rounded-bl-md border border-zinc-200 bg-white px-4 py-3 shadow-sm dark:border-zinc-700 dark:bg-zinc-900">
            <span aria-hidden="true" className="inline-flex items-center gap-1.5">
              <span className="h-2 w-2 animate-bounce rounded-full bg-zinc-500 motion-reduce:animate-none" style={{ animationDelay: '-0.3s' }} />
              <span className="h-2 w-2 animate-bounce rounded-full bg-zinc-500 motion-reduce:animate-none" style={{ animationDelay: '-0.15s' }} />
              <span className="h-2 w-2 animate-bounce rounded-full bg-zinc-500 motion-reduce:animate-none" />
            </span>
            <span className="text-xs text-zinc-500 dark:text-zinc-400">Syra is typing</span>
          </div>
        </div>}
      </div>
      <div className="pointer-events-none absolute inset-x-0 bottom-0 z-20 px-3 pb-[max(env(safe-area-inset-bottom),0.75rem)] pt-4 sm:px-5">
      <form onSubmit={sendText} className={`pointer-events-auto mx-auto max-w-3xl transition-[width] duration-300 ease-out ${isComposerFocused || draft.trim() ? 'w-full' : 'w-[88%]'}`}>
        <input ref={fileInputRef} type="file" multiple accept="image/*,video/*,audio/*" className="hidden" onChange={event => { const files = Array.from(event.currentTarget.files || []); event.currentTarget.value = ''; if (files.length > 1) void sendMediaBatch(files); else if (files[0]) void sendMedia(files[0]); }} />
        <input ref={documentInputRef} type="file" accept=".pdf,application/pdf" className="hidden" onChange={event => { const file = event.currentTarget.files?.[0]; event.currentTarget.value = ''; if (file) void sendMedia(file); }} />
        {isRecording ? <div className="mb-3 flex items-center justify-between rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-rose-950 dark:border-rose-900 dark:bg-rose-950/40 dark:text-rose-200"><span className="inline-flex items-center gap-2 text-sm font-medium"><span className="h-2.5 w-2.5 animate-pulse rounded-full bg-rose-600" />Recording · {Math.floor(recordingSeconds / 60).toString().padStart(2, '0')}:{(recordingSeconds % 60).toString().padStart(2, '0')}</span><div className="flex items-center gap-2"><Button type="button" variant="ghost" onClick={discardRecording} aria-label="Discard recording" className="h-9 w-9 rounded-full p-0 text-rose-800 hover:bg-rose-100 dark:text-rose-200"><X className="h-4 w-4" /></Button><Button type="button" onClick={stopRecording} aria-label="Stop recording" className="h-9 w-9 rounded-full bg-rose-700 p-0 text-white hover:bg-rose-800"><Square className="h-3.5 w-3.5 fill-current" /></Button></div></div> : null}
        {voiceDraft && !isRecording ? <div className="mb-3 flex items-center gap-2 rounded-2xl border border-zinc-200 bg-zinc-50 p-2 dark:border-zinc-700 dark:bg-zinc-950"><audio controls src={voiceDraft.url} className="h-9 min-w-0 flex-1" /><span className="shrink-0 text-xs text-zinc-500">{voiceDraft.durationSeconds}s</span><Button type="button" variant="ghost" onClick={() => { URL.revokeObjectURL(voiceDraft.url); setVoiceDraft(null); }} aria-label="Remove recording" className="h-9 w-9 rounded-full p-0 text-zinc-600 dark:text-zinc-300"><Trash2 className="h-4 w-4" /></Button><Button type="button" disabled={isUploadingMedia} onClick={() => void sendVoiceDraft()} aria-label="Send voice recording" className="h-10 w-10 rounded-full bg-zinc-950 p-0 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-950">{isUploadingMedia ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}</Button></div> : null}
        <div className="flex w-full items-end gap-2 rounded-full border border-zinc-200 bg-zinc-50 p-1.5 shadow-[0_5px_20px_-16px_rgba(0,0,0,0.4)] focus-within:border-zinc-400 dark:border-zinc-700 dark:bg-zinc-950 dark:focus-within:border-zinc-500">
          <Button type="button" variant="ghost" onClick={() => setIsComposerSheetOpen(true)} disabled={isRecording || isUploadingMedia || Boolean(voiceDraft)} aria-label="Open message types" className="h-10 w-10 shrink-0 rounded-full p-0 text-zinc-600 hover:bg-zinc-200 dark:text-zinc-300 dark:hover:bg-zinc-800"><Plus className="h-5 w-5" /></Button>
          <textarea value={draft} onChange={event => setDraft(event.target.value)} onFocus={() => { setIsComposerFocused(true); shouldStickToBottomRef.current = true; scrollToLatest('smooth'); }} onBlur={() => setIsComposerFocused(false)} maxLength={5000} rows={1} disabled={isRecording || Boolean(voiceDraft) || isUploadingMedia} placeholder={isUploadingMedia ? 'Uploading attachment…' : 'Write a message…'} aria-label="Write a message (not end-to-end encrypted)" className="max-h-28 min-h-10 flex-1 resize-none self-center bg-transparent px-2 py-2.5 text-sm leading-5 text-zinc-900 outline-none placeholder:text-zinc-400 disabled:opacity-60 dark:text-zinc-100" />
          {draft.trim() ? <Button type="submit" disabled={isSending || isUploadingMedia || isRecording} aria-label="Send message" className="h-10 w-10 shrink-0 rounded-full bg-zinc-950 p-0 text-white shadow-none transition-transform duration-100 active:scale-95 hover:bg-zinc-800 disabled:bg-zinc-300 dark:bg-zinc-100 dark:text-zinc-950 dark:hover:bg-zinc-300">{isSending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}</Button> : <Button type="button" disabled={isUploadingMedia || isRecording || Boolean(voiceDraft)} onClick={() => void startRecording()} aria-label="Record voice message" className="h-10 w-10 shrink-0 rounded-full bg-zinc-950 p-0 text-white shadow-none hover:bg-zinc-800 disabled:bg-zinc-300 dark:bg-zinc-100 dark:text-zinc-950"><Mic className="h-5 w-5" /></Button>}
        </div>
      </form>
      </div>
      </div>
      {mediaGallery && <BatchMediaGallery
        attachments={mediaGallery.attachments}
        caption={mediaGallery.caption}
        getUrl={(item, mediaIndex) => mediaUrls[`${mediaGallery.message.id}:${mediaIndex}`] || (mediaIndex === 0 ? mediaUrls[mediaGallery.message.id] : undefined) || item.url}
        onMediaError={mediaIndex => { void refreshMediaUrl(mediaGallery.message, mediaIndex); }}
        onDismiss={() => setMediaGallery(null)}
      />}
      {isComposerSheetOpen && <ChatComposerSheet
        isSending={isSending}
        onClose={() => setIsComposerSheetOpen(false)}
        onChooseMedia={(kind: ChatPickerKind) => {
          setIsComposerSheetOpen(false);
          (kind === 'file' ? documentInputRef : fileInputRef).current?.click();
        }}
        onStartVoice={() => { setIsComposerSheetOpen(false); void startRecording(); }}
        onSend={sendTypedMessage}
      />}
    </div>
  );
};
