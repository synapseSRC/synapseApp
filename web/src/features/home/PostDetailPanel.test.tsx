import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const fixtures = vi.hoisted(() => ({
  root: null as Record<string, unknown> | null,
  replies: [] as Record<string, unknown>[],
  people: [] as Record<string, unknown>[],
}));

vi.mock('@/lib/supabase', () => ({
  supabase: {
    from: vi.fn(() => {
      const chain: Record<string, unknown> = {};
      for (const method of ['select', 'eq', 'or', 'order']) chain[method] = vi.fn(() => chain);
      chain.maybeSingle = vi.fn(async () => ({ data: fixtures.root, error: null }));
      chain.limit = vi.fn(async () => ({ data: fixtures.replies, error: null }));
      chain.in = vi.fn(async () => ({ data: fixtures.people, error: null }));
      return chain;
    }),
    storage: { from: vi.fn() },
  },
}));

import { PostDetailPanel } from './HomeScreen';
import type { UserProfile } from '@/types';

const user: UserProfile = {
  id: 'viewer-1',
  username: 'viewer',
  fullName: 'Viewer',
  followersCount: 0,
  followingCount: 0,
  postsCount: 0,
  createdAt: '2026-01-01T00:00:00.000Z',
};

const post = (id: string, overrides: Record<string, unknown> = {}) => ({
  id,
  author_uid: 'author-1',
  post_text: id === 'root-1' ? 'Original post' : `Reply ${id.replace('reply-', '')}`,
  post_image: null,
  media_items: [],
  post_type: 'TEXT',
  timestamp: Date.now(),
  publish_date: new Date().toISOString(),
  created_at: new Date().toISOString(),
  likes_count: 0,
  comments_count: 20,
  reshares_count: 0,
  is_deleted: false,
  has_poll: false,
  poll_question: null,
  poll_options: [],
  poll_end_time: null,
  youtube_url: null,
  location_name: null,
  metadata: null,
  post_hide_like_count: null,
  post_hide_comments_count: null,
  post_disable_comments: null,
  post_visibility: 'public',
  in_reply_to_post_id: id === 'root-1' ? null : 'root-1',
  root_post_id: 'root-1',
  reply_to_usernames: [],
  quoted_post_id: null,
  is_quote: false,
  is_edited: false,
  edited_at: null,
  ...overrides,
});

beforeEach(() => {
  fixtures.root = post('root-1');
  fixtures.replies = Array.from({ length: 20 }, (_, index) => post(`reply-${index + 1}`));
  fixtures.people = [{ uid: 'author-1', username: 'author', display_name: 'Author', avatar: null, verify: false }];
});

describe('PostDetailPanel reply composer', () => {
  it('keeps the reply composer docked outside the scrollable thread when there are many comments', async () => {
    const props = {
      postId: 'root-1',
      initialPost: fixtures.root as never,
      user,
      interactions: {},
      busyPostActions: {},
      actionError: null,
      onBack: vi.fn(),
      onLike: vi.fn(),
      onReshare: vi.fn(),
      onSave: vi.fn(),
      onShare: vi.fn(),
      onLoadInteractions: vi.fn(async () => undefined),
      onReplyCreated: vi.fn(),
    };

    const { container } = render(<PostDetailPanel {...props} />);
    await waitFor(() => expect(screen.getByText('Reply 20')).toBeInTheDocument());

    const shell = container.firstElementChild;
    const thread = screen.getByRole('main');
    const dock = screen.getByTestId('post-reply-dock');
    const composer = screen.getByRole('textbox', { name: 'Write a reply' });

    expect(shell).toHaveClass('h-[100dvh]', 'overflow-hidden', 'flex-col');
    expect(thread).toHaveClass('flex-1', 'min-h-0', 'overflow-y-auto');
    expect(thread.nextElementSibling).toBe(dock);
    expect(dock).toHaveClass('shrink-0');
    expect(composer).toBeVisible();
  });
});
