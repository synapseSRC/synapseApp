import React, { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import type { Story } from './types';

export const StoryViewer: React.FC<{ stories: Story[]; startIndex?: number; onClose: () => void; onSeen?: (story: Story) => void }> = ({ stories, startIndex = 0, onClose, onSeen }) => {
  const [index, setIndex] = useState(startIndex);
  const story = stories[index];
  useEffect(() => {
    if (story) onSeen?.(story);
    const timer = window.setTimeout(() => {
      if (index < stories.length - 1) setIndex(i => i + 1); else onClose();
    }, 5000);
    return () => window.clearTimeout(timer);
  }, [index, stories.length, onClose, onSeen, story]);
  if (!story) return null;
  return (
    <div className="fixed inset-0 z-50 bg-black text-white flex items-center justify-center">
      <button type="button" onClick={onClose} className="absolute top-4 right-4 z-10"><X className="h-6 w-6" /></button>
      <div className="absolute top-2 left-2 right-2 flex gap-1">
        {stories.map((_, i) => <div key={i} className={`h-1 flex-1 rounded ${i <= index ? 'bg-white' : 'bg-white/30'}`} />)}
      </div>
      {story.media_type?.includes('video') ? (
        <video src={story.media_url || ''} className="max-h-full max-w-full" autoPlay muted />
      ) : (
        <img src={story.media_url || story.thumbnail_url || ''} alt="" className="max-h-full max-w-full object-contain" />
      )}
      {story.content && <p className="absolute bottom-8 left-4 right-4 text-center text-sm">{story.content}</p>}
      <button type="button" className="absolute left-0 top-0 h-full w-1/3" onClick={() => setIndex(i => Math.max(0, i - 1))} aria-label="previous" />
      <button type="button" className="absolute right-0 top-0 h-full w-1/3" onClick={() => setIndex(i => (i < stories.length - 1 ? i + 1 : i))} aria-label="next" />
    </div>
  );
};
