import React, { useEffect, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, FileText, Mic, Play, X } from 'lucide-react';
import { formatMediaDuration, mediaResolutionLabel, type MediaAttachment } from './mediaAttachmentUtils';

interface BatchMediaGalleryProps {
  attachments: MediaAttachment[];
  caption?: string;
  getUrl: (attachment: MediaAttachment, index: number) => string;
  onMediaError: (index: number) => void;
  onDismiss: () => void;
}

const GalleryThumbnail: React.FC<{
  attachment: MediaAttachment;
  src: string;
  index: number;
  onClick: () => void;
  onError: () => void;
}> = ({ attachment, src, index, onClick, onError }) => (
  <button type="button" onClick={onClick} aria-label={`View media ${index + 1}`} className="group relative aspect-square overflow-hidden rounded-xl border border-zinc-200 bg-zinc-100 text-left shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
    {attachment.type === 'image' ? <img src={src} alt={attachment.name || `Media ${index + 1}`} loading="lazy" onError={onError} className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-[1.02]" /> : null}
    {attachment.type === 'video' ? <div className="relative h-full w-full bg-zinc-900">
      <video muted playsInline preload="metadata" src={src} onError={onError} className="h-full w-full object-cover" />
      <span aria-hidden="true" className="absolute inset-0 grid place-items-center bg-black/15"><span className="grid h-11 w-11 place-items-center rounded-full bg-black/60 text-white"><Play className="ml-0.5 h-5 w-5 fill-current" /></span></span>
      {attachment.duration && <span className="absolute bottom-2 left-2 rounded-md bg-black/75 px-2 py-1 text-[10px] font-medium text-white">{formatMediaDuration(attachment.duration)}</span>}
    </div> : null}
    {attachment.type === 'audio' ? <span className="flex h-full w-full flex-col items-center justify-center gap-3 bg-zinc-200 p-3 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"><Mic className="h-8 w-8" /><span className="max-w-full truncate text-xs">{attachment.name || 'Audio'}</span></span> : null}
    {attachment.type === 'file' ? <span className="flex h-full w-full flex-col items-center justify-center gap-3 bg-zinc-200 p-3 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"><FileText className="h-8 w-8" /><span className="max-w-full truncate text-xs">{attachment.name || 'File'}</span></span> : null}
    <span className="absolute left-2 top-2 grid h-7 min-w-7 place-items-center rounded-full bg-white/90 px-2 text-xs font-bold text-zinc-900 shadow-sm dark:bg-zinc-950/90 dark:text-zinc-100">{index + 1}</span>
  </button>
);

const MediaViewer: React.FC<BatchMediaGalleryProps & { initialIndex: number }> = ({ attachments, caption, getUrl, onMediaError, onDismiss, initialIndex }) => {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const touchStartX = useRef<number | null>(null);
  const item = attachments[currentIndex];
  const src = item ? getUrl(item, currentIndex) : '';

  useEffect(() => {
    setCurrentIndex(Math.min(Math.max(initialIndex, 0), Math.max(attachments.length - 1, 0)));
    closeButtonRef.current?.focus();
  }, [initialIndex, attachments.length]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onDismiss();
      if (event.key === 'ArrowLeft') setCurrentIndex(index => Math.max(0, index - 1));
      if (event.key === 'ArrowRight') setCurrentIndex(index => Math.min(attachments.length - 1, index + 1));
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [attachments.length, onDismiss]);

  if (!item) return null;
  const resolution = mediaResolutionLabel(item);

  return <div role="dialog" aria-modal="true" aria-label="Media viewer" className="fixed inset-0 z-[120] flex flex-col bg-black text-white" onClick={onDismiss}>
    <header className="z-10 flex h-16 shrink-0 items-center justify-between border-b border-white/10 bg-black/80 px-3 pt-[env(safe-area-inset-top)] sm:px-5">
      <button ref={closeButtonRef} type="button" onClick={onDismiss} aria-label="Close media viewer" className="grid h-10 w-10 place-items-center rounded-full text-white hover:bg-white/10"><X className="h-5 w-5" /></button>
      <span aria-live="polite" className="text-sm font-semibold">{currentIndex + 1} / {attachments.length}</span>
      <span className="h-10 w-10" aria-hidden="true" />
    </header>

    <main
      className="relative flex min-h-0 flex-1 items-center justify-center overflow-hidden px-3 py-5 sm:px-12"
      onClick={event => event.stopPropagation()}
      onTouchStart={event => { touchStartX.current = event.changedTouches[0]?.clientX ?? null; }}
      onTouchEnd={event => {
        const start = touchStartX.current;
        const end = event.changedTouches[0]?.clientX;
        touchStartX.current = null;
        if (start === null || end === undefined) return;
        const delta = end - start;
        if (delta > 60) setCurrentIndex(index => Math.max(0, index - 1));
        else if (delta < -60) setCurrentIndex(index => Math.min(attachments.length - 1, index + 1));
      }}
    >
      {item.type === 'image' ? <img src={src} alt={item.name || 'Shared media'} onError={() => onMediaError(currentIndex)} className="max-h-full max-w-full select-none object-contain" /> : null}
      {item.type === 'video' ? <div className="relative flex max-h-full max-w-full items-center justify-center">
        <video controls playsInline preload="metadata" src={src} onError={() => onMediaError(currentIndex)} className="max-h-[calc(100dvh-9rem)] max-w-full" aria-label={item.name || 'Shared video'} />
        {(item.duration || resolution) && <div className="pointer-events-none absolute bottom-12 left-3 right-3 flex justify-between text-xs font-semibold text-white">
          {item.duration ? <span className="rounded-md bg-black/70 px-2 py-1">{formatMediaDuration(item.duration)}</span> : <span />}
          {resolution && <span className="rounded-md bg-black/70 px-2 py-1">{resolution}</span>}
        </div>}
      </div> : null}
      {item.type === 'audio' ? <div className="flex w-full max-w-lg flex-col items-center gap-5 rounded-2xl border border-white/10 bg-zinc-900 p-6"><Mic className="h-12 w-12 text-zinc-300" /><p className="max-w-full truncate text-sm">{item.name || 'Audio attachment'}</p><audio controls src={src} onError={() => onMediaError(currentIndex)} className="w-full" /></div> : null}
      {item.type === 'file' ? <a href={src} target="_blank" rel="noreferrer" onClick={event => event.stopPropagation()} className="flex flex-col items-center gap-4 rounded-2xl border border-white/10 bg-zinc-900 p-8 text-white underline"><FileText className="h-12 w-12" /><span className="max-w-[80vw] truncate">{item.name || 'Open attachment'}</span></a> : null}

      {currentIndex > 0 && <button type="button" onClick={event => { event.stopPropagation(); setCurrentIndex(index => Math.max(0, index - 1)); }} aria-label="Previous media item" className="absolute left-3 top-1/2 grid h-11 w-11 -translate-y-1/2 place-items-center rounded-full bg-black/55 hover:bg-black/75 sm:left-6"><ChevronLeft className="h-6 w-6" /></button>}
      {currentIndex < attachments.length - 1 && <button type="button" onClick={event => { event.stopPropagation(); setCurrentIndex(index => Math.min(attachments.length - 1, index + 1)); }} aria-label="Next media item" className="absolute right-3 top-1/2 grid h-11 w-11 -translate-y-1/2 place-items-center rounded-full bg-black/55 hover:bg-black/75 sm:right-6"><ChevronRight className="h-6 w-6" /></button>}
    </main>

    {caption?.trim() && <footer className="shrink-0 border-t border-white/10 bg-black/80 px-4 py-3 text-sm leading-6 text-white sm:px-6">{caption}</footer>}
  </div>;
};

export const BatchMediaGallery: React.FC<BatchMediaGalleryProps> = ({ attachments, caption, getUrl, onMediaError, onDismiss }) => {
  const [viewerIndex, setViewerIndex] = useState<number | null>(null);

  useEffect(() => {
    if (viewerIndex !== null) return;
    const handleKeyDown = (event: KeyboardEvent) => { if (event.key === 'Escape') onDismiss(); };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [viewerIndex, onDismiss]);

  if (!attachments.length) return null;

  if (viewerIndex !== null) {
    return <MediaViewer attachments={attachments} initialIndex={viewerIndex} caption={caption} getUrl={getUrl} onMediaError={onMediaError} onDismiss={onDismiss} />;
  }

  return <div role="dialog" aria-modal="true" aria-label="Media gallery" className="fixed inset-0 z-[110] flex flex-col bg-zinc-50 text-zinc-950 dark:bg-zinc-950 dark:text-zinc-100">
    <header className="flex h-16 shrink-0 items-center gap-3 border-b border-zinc-200 bg-white px-4 dark:border-zinc-800 dark:bg-zinc-900 sm:px-6">
      <button type="button" onClick={onDismiss} aria-label="Close gallery" className="grid h-10 w-10 shrink-0 place-items-center rounded-full text-zinc-700 hover:bg-zinc-100 dark:text-zinc-200 dark:hover:bg-zinc-800"><X className="h-5 w-5" /></button>
      <div className="min-w-0 flex-1"><h2 className="truncate text-sm font-semibold">Media preview</h2><p className="text-xs text-zinc-500 dark:text-zinc-400">{attachments.length} items</p></div>
    </header>

    {caption?.trim() && <div className="mx-4 mt-4 rounded-xl border border-zinc-200 bg-white p-3 text-sm leading-6 dark:border-zinc-800 dark:bg-zinc-900 sm:mx-6">{caption}</div>}

    <div className="min-h-0 flex-1 overflow-y-auto p-3 sm:p-6">
      <div className="mx-auto grid max-w-5xl grid-cols-2 gap-2 sm:grid-cols-3 sm:gap-3 lg:grid-cols-4">
        {attachments.map((attachment, index) => <GalleryThumbnail
          key={`${attachment.url}-${index}`}
          attachment={attachment}
          src={getUrl(attachment, index)}
          index={index}
          onClick={() => setViewerIndex(index)}
          onError={() => onMediaError(index)}
        />)}
      </div>
    </div>
  </div>;
};
