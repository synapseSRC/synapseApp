import React from 'react';
import { ArrowLeft, CalendarClock, CheckCircle2, Loader2, Sparkles } from 'lucide-react';
import { CreatePostToolPanels, CreatePostToolSelector, PostComposer, PostPreview } from './CreatePostPanels';
import { useCreatePost, type CreatePostModel } from './useCreatePost';
import type { CreatePostScreenProps } from './createPostShared';

export const CreatePostShell: React.FC<{ model: CreatePostModel }> = ({ model }) => (
  <div className="min-h-screen bg-[#f7f7f8] text-zinc-950">
    <header className="sticky top-0 z-30 border-b border-zinc-200 bg-white/95 backdrop-blur"><div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-3 px-4 sm:px-6"><button type="button" onClick={model.saveDraftAndClose} className="inline-flex items-center gap-2 rounded-full px-3 py-2 text-sm font-medium text-zinc-600 transition hover:bg-zinc-100 hover:text-zinc-950"><ArrowLeft className="h-4 w-4" /><span className="hidden sm:inline">Back to home</span></button><div className="text-center"><p className="text-sm font-semibold text-zinc-950">Create a post</p><p className="hidden text-[11px] text-zinc-500 sm:block">Share a thought, a moment, or a conversation</p></div><button type="button" onClick={model.saveDraftAndClose} className="rounded-full border border-zinc-200 bg-white px-4 py-2 text-xs font-semibold text-zinc-700 transition hover:border-zinc-300 hover:bg-zinc-50">Save draft</button></div></header>
    <main className="mx-auto grid max-w-7xl gap-6 px-4 py-6 sm:px-6 sm:py-9 lg:grid-cols-[minmax(0,760px)_300px] lg:items-start lg:justify-center">
      <form onSubmit={model.handleSubmit} className="space-y-4">
        <PostComposer model={model} />
        <CreatePostToolPanels model={model} />
        <CreatePostToolSelector model={model} />
        {model.formError && <div role="alert" className="rounded-2xl border border-zinc-300 bg-white px-4 py-3 text-sm text-zinc-700">{model.formError}</div>}
        {model.successMessage && <div role="status" className="flex items-center gap-2 rounded-2xl border border-zinc-300 bg-white px-4 py-3 text-sm font-medium text-zinc-800"><CheckCircle2 className="h-4 w-4" />{model.successMessage}</div>}
        <div className="sticky bottom-3 z-20 flex items-center justify-between gap-3 rounded-2xl border border-zinc-200 bg-white/95 p-3 shadow-[0_10px_32px_-16px_rgba(0,0,0,0.25)] backdrop-blur sm:p-4"><span className="min-w-0 truncate text-xs text-zinc-500">{model.uploadMessage || (model.tools.schedule ? `Scheduled · ${new Date(model.scheduledAt).toLocaleString()}` : 'Your draft saves on this device')}</span><button type="submit" disabled={!model.isReadyToPost || model.isSaving || Boolean(model.successMessage)} className="inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-xl bg-zinc-950 px-5 text-sm font-semibold text-white transition hover:bg-zinc-800 disabled:cursor-not-allowed disabled:bg-zinc-300">{model.isSaving ? <><Loader2 className="h-4 w-4 animate-spin" />{model.uploadMessage || 'Saving…'}</> : model.tools.schedule ? <><CalendarClock className="h-4 w-4" />Schedule post</> : <><Sparkles className="h-4 w-4" />Publish post</>}</button></div>
      </form>
      <PostPreview />
    </main>
  </div>
);

export const CreatePostScreen: React.FC<CreatePostScreenProps> = props => {
  const model = useCreatePost(props);
  return <CreatePostShell model={model} />;
};
