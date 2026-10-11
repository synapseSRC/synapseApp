import React, { useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, FileText, Image, Mic, Play } from 'lucide-react';
import { formatMediaDuration, mediaResolutionLabel, type MediaAttachment } from './mediaAttachmentUtils';

interface VideoPreviewProps {
  attachment: MediaAttachment;
  src: string;
  onError: () => void;
}

const VideoPreview: React.FC<VideoPreviewProps> = ({ attachment, src, onError }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const resolution = mediaResolutionLabel(attachment);
  const duration = formatMediaDuration(attachment.duration);

  const play = async () => {
    try {
      await videoRef.current?.play();
    } catch {
      // Browser playback can be blocked; native controls remain available.
    }
  };

  return (
    <div className="relative h-full w-full bg-black">
      <video
        ref={videoRef}
        controls
        playsInline
        preload="metadata"
        src={src}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onError={onError}
        className="h-full w-full object-contain"
        aria-label={attachment.name || 'Shared video'}
      />
      {!isPlaying && <button type="button" onClick={() => void play()} aria-label="Play video" className="absolute left-1/2 top-1/2 z-10 grid h-14 w-14 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full bg-zinc-950/80 text-white shadow-lg ring-1 ring-white/40">
        <Play className="ml-1 h-6 w-6 fill-current" />
      </button>}
      {(duration || resolution) && <div className="pointer-events-none absolute inset-x-3 bottom-12 z-10 flex items-center justify-between">
        {duration ? <span className="inline-flex items-center rounded-lg bg-zinc-950/85 px-2.5 py-1.5 text-xs font-medium text-white">{duration}</span> : <span />}
        {resolution && <span className="rounded-lg bg-zinc-950/85 px-2 py-1.5 text-[10px] font-bold tracking-wide text-white">{resolution}</span>}
      </div>}
    </div>
  );
};

interface BatchMediaDeckProps {
  attachments: MediaAttachment[];
  getUrl: (attachment: MediaAttachment, index: number) => string;
  onMediaError: (index: number) => void;
  onOpenGallery: () => void;
}

const StackPreview: React.FC<{ attachment: MediaAttachment; src: string }> = ({ attachment, src }) => {
  if (attachment.type === 'image') return <img src={src} alt="" className="h-full w-full object-cover" />;
  if (attachment.type === 'video') return <div className="flex h-full w-full flex-col items-center justify-center gap-2 bg-zinc-900 text-white"><Play className="h-8 w-8 fill-current" /><span className="text-xs font-medium">Video</span></div>;
  if (attachment.type === 'audio') return <div className="flex h-full w-full flex-col items-center justify-center gap-2 bg-zinc-200 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"><Mic className="h-7 w-7" /><span className="max-w-[80%] truncate text-xs">{attachment.name || 'Audio'}</span></div>;
  return <div className="flex h-full w-full flex-col items-center justify-center gap-2 bg-zinc-200 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"><FileText className="h-7 w-7" /><span className="max-w-[80%] truncate text-xs">{attachment.name || 'File'}</span></div>;
};

export const BatchMediaDeck: React.FC<BatchMediaDeckProps> = ({ attachments, getUrl, onMediaError, onOpenGallery }) => {
  const [activeIndex, setActiveIndex] = useState(0);
  if (!attachments.length) return null;

  const active = attachments[activeIndex] || attachments[0];
  const activeUrl = getUrl(active, activeIndex);
  const stackCards = [2, 1]
    .filter(offset => attachments.length > offset)
    .map(offset => ({ offset, index: (activeIndex + offset) % attachments.length }));

  const renderActive = () => {
    if (active.type === 'image') return <img src={activeUrl} alt={active.name || 'Shared photo'} onError={() => onMediaError(activeIndex)} className="h-full w-full object-cover" />;
    if (active.type === 'video') return <VideoPreview key={`${activeIndex}:${activeUrl}`} attachment={active} src={activeUrl} onError={() => onMediaError(activeIndex)} />;
    if (active.type === 'audio') return <div className="flex h-full w-full flex-col items-center justify-center gap-4 bg-zinc-100 p-5 text-zinc-800 dark:bg-zinc-900 dark:text-zinc-100"><Mic className="h-9 w-9" /><p className="max-w-full truncate text-sm font-medium">{active.name || 'Audio attachment'}</p><audio controls preload="metadata" src={activeUrl} onError={() => onMediaError(activeIndex)} className="w-full" /></div>;
    return <a href={activeUrl} target="_blank" rel="noreferrer" onClick={event => event.stopPropagation()} className="flex h-full w-full flex-col items-center justify-center gap-3 bg-zinc-100 p-5 text-zinc-800 underline dark:bg-zinc-900 dark:text-zinc-100"><FileText className="h-10 w-10" /><span className="max-w-full truncate text-sm font-medium">{active.name || 'Open attachment'}</span></a>;
  };

  return (
    <div role="group" aria-label={`${attachments.length} media items`} className="relative my-1 w-full max-w-[22rem] pb-8 pr-5 pt-2">
      {stackCards.map(({ offset, index }) => {
        const item = attachments[index];
        return <button
          key={`${index}-${item.url}`}
          type="button"
          onClick={() => setActiveIndex(index)}
          aria-label={`Show media ${index + 1} of ${attachments.length}`}
          className="absolute inset-y-5 right-0 w-[86%] overflow-hidden rounded-[1.2rem] border-4 border-white bg-zinc-100 shadow-md transition-transform dark:border-zinc-700 dark:bg-zinc-900"
          style={{ transform: `translate(${offset * 7}px, ${offset * 4}px) rotate(${offset * 3}deg)`, zIndex: 3 - offset }}
        >
          <StackPreview attachment={item} src={getUrl(item, index)} />
        </button>;
      })}
      <div className="relative z-10 mr-4 aspect-[4/3] overflow-hidden rounded-[1.2rem] border-[5px] border-white bg-zinc-100 shadow-xl dark:border-zinc-700 dark:bg-zinc-900">
        {renderActive()}
      </div>
      {attachments.length > 1 && <div className="absolute bottom-1 right-0 z-20 inline-flex items-center gap-1 rounded-full border border-zinc-700/50 bg-zinc-950/90 p-1 text-white shadow-lg">
        <button type="button" onClick={onOpenGallery} aria-label="Open media gallery" title="Open media gallery" className="grid h-7 w-7 place-items-center rounded-full hover:bg-white/15"><Image className="h-4 w-4" /></button>
        <span aria-live="polite" className="px-2 text-[10px] font-medium">{activeIndex + 1} / {attachments.length}</span>
        <button type="button" onClick={() => setActiveIndex((activeIndex - 1 + attachments.length) % attachments.length)} aria-label="Previous media item" className="grid h-7 w-7 place-items-center rounded-full hover:bg-white/15"><ChevronLeft className="h-4 w-4" /></button>
        <button type="button" onClick={() => setActiveIndex((activeIndex + 1) % attachments.length)} aria-label="Next media item" className="grid h-7 w-7 place-items-center rounded-full hover:bg-white/15"><ChevronRight className="h-4 w-4" /></button>
      </div>}
    </div>
  );
};
