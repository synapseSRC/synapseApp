import React, { useState } from 'react';
import { X, Image as ImageIcon } from 'lucide-react';
import { supabase } from '@/lib/supabase';
import { createStory } from './storiesService';
import { STORAGE_BUCKETS } from '@/config/storageBuckets';

export const StoryCreator: React.FC<{ userId: string; onClose: () => void; onCreated?: () => void }> = ({ userId, onClose, onCreated }) => {
  const [file, setFile] = useState<File | null>(null);
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    if (!file) { setError('Select media first.'); return; }
    setBusy(true); setError(null);
    try {
      const path = `${userId}/${Date.now()}-${file.name}`;
      const { error: uploadError } = await supabase.storage.from(STORAGE_BUCKETS.POSTS_MEDIA).upload(path, file);
      if (uploadError) throw uploadError;
      const { data: urlData } = supabase.storage.from(STORAGE_BUCKETS.POSTS_MEDIA).getPublicUrl(path);
      await createStory({ mediaUrl: urlData.publicUrl, mediaType: file.type.startsWith('video') ? 'video' : 'image', content: text || null });
      onCreated?.();
      onClose();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not create story.');
    } finally { setBusy(false); }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4">
      <div className="w-full max-w-md rounded-2xl bg-white dark:bg-zinc-900 p-5 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold">Create story</h2>
          <button type="button" onClick={onClose}><X className="h-5 w-5" /></button>
        </div>
        <label className="block">
          <span className="text-sm">Media</span>
          <input type="file" accept="image/*,video/*" onChange={e => setFile(e.target.files?.[0] || null)} className="mt-1 block w-full" />
        </label>
        {file && <div className="text-xs text-zinc-500">{file.name}</div>}
        <label className="block">
          <span className="text-sm">Text overlay</span>
          <textarea value={text} onChange={e => setText(e.target.value)} className="mt-1 w-full rounded border p-2 text-sm" rows={3} />
        </label>
        {error && <p className="text-sm text-rose-600">{error}</p>}
        <button type="button" disabled={busy} onClick={submit} className="w-full rounded-full bg-zinc-950 text-white py-2 text-sm font-semibold disabled:opacity-50">
          {busy ? 'Publishing…' : 'Publish story'}
        </button>
        <ImageIcon className="hidden" />
      </div>
    </div>
  );
};
