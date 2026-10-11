import React from 'react';
import { Bookmark, Heart, Loader2, MessageCircle, Repeat2, Share2 } from 'lucide-react';
import type { PostInteractionState } from './postActions';

interface PostActionBarProps {
  postId: string;
  hideLikes?: boolean;
  hideReplyCount?: boolean;
  repliesDisabled?: boolean;
  state: PostInteractionState;
  busy?: boolean;
  onLike: () => void;
  onReply: () => void;
  onReshare: () => void;
  onSave: () => void;
  onShare: () => void;
}

const compactCount = (value: number) => new Intl.NumberFormat(undefined, { notation: value >= 1000 ? 'compact' : 'standard', maximumFractionDigits: 1 }).format(value);

const actionClass = (active: boolean) => `inline-flex min-h-10 items-center gap-2 rounded-full px-3 text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-500 disabled:cursor-not-allowed disabled:opacity-50 ${active ? 'bg-zinc-100 text-zinc-950 dark:bg-zinc-800 dark:text-zinc-100' : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-950 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-100'}`;

export const PostActionBar: React.FC<PostActionBarProps> = ({ postId, hideLikes, hideReplyCount, repliesDisabled, state, busy = false, onLike, onReply, onReshare, onSave, onShare }) => (
  <footer className="mt-4 flex flex-wrap items-center gap-1 border-t border-zinc-100 pt-3 dark:border-zinc-800" aria-label="Post actions">
    <button type="button" onClick={onLike} disabled={busy} aria-label={`${state.isLiked ? 'Unlike' : 'Like'} post ${postId}`} aria-pressed={state.isLiked} className={actionClass(state.isLiked)}><Heart className={`h-4 w-4 ${state.isLiked ? 'fill-current' : ''}`} />{!hideLikes && <span>{compactCount(state.likesCount)}</span>}<span className="sr-only">Like</span></button>
    <button type="button" onClick={onReply} disabled={busy} aria-label={repliesDisabled ? 'View post details; replies are disabled' : 'View post details and replies'} className={actionClass(false)}><MessageCircle className="h-4 w-4" />{!hideReplyCount && <span>{compactCount(state.repliesCount)}</span>}</button>
    <button type="button" onClick={onReshare} disabled={busy} aria-label={state.isReshared ? 'Undo reshare' : 'Reshare post'} aria-pressed={state.isReshared} className={actionClass(state.isReshared)}><Repeat2 className="h-4 w-4" />{compactCount(state.resharesCount)}</button>
    <button type="button" onClick={onSave} disabled={busy} aria-label={state.isSaved ? 'Remove saved post' : 'Save post'} aria-pressed={state.isSaved} className={actionClass(state.isSaved)}><Bookmark className={`h-4 w-4 ${state.isSaved ? 'fill-current' : ''}`} /><span className="sr-only">Save</span></button>
    <button type="button" onClick={onShare} disabled={busy} aria-label="Share post" className={actionClass(false)}><Share2 className="h-4 w-4" /><span className="sr-only">Share</span></button>
    {busy && <Loader2 className="ml-auto h-4 w-4 animate-spin text-zinc-500" aria-label="Updating post" />}
  </footer>
);
