import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const chatFixtures = vi.hoisted(() => ({
  history: [] as Record<string, unknown>[],
  replies: [] as Record<string, unknown>[],
  inserted: null as Record<string, unknown> | null,
  syraInvocations: [] as Record<string, unknown>[],
  messageInserts: [] as Record<string, unknown>[],
}));

vi.mock('@/lib/supabase', () => {
  const makeChain = (table = '') => {
    const chain: Record<string, unknown> = {};
    for (const method of ['select', 'eq', 'or', 'order']) chain[method] = vi.fn(() => chain);
    chain.limit = vi.fn(async () => ({ data: chatFixtures.history, error: null }));
    chain.gte = vi.fn(async () => ({ data: chatFixtures.syraInvocations, error: null }));
    chain.in = vi.fn(async () => ({ data: chatFixtures.replies, error: null }));
    chain.maybeSingle = vi.fn(async () => ({ data: { is_muted: false }, error: null }));
    chain.insert = vi.fn((values?: Record<string, unknown>) => { if (table === 'messages' && values) chatFixtures.messageInserts.push(values); return chain; });
    chain.upsert = vi.fn(() => chain);
    chain.single = vi.fn(async () => ({ data: chatFixtures.inserted, error: null }));
    return chain;
  };
  const participantQuery = makeChain('chat_participants');
  const channel: { on: (...args: unknown[]) => unknown; subscribe: (callback?: (status: string) => void) => unknown } = {
    on: () => channel,
    subscribe: callback => { callback?.('SUBSCRIBED'); return channel; },
  };
  channel.on = vi.fn(() => channel);
  channel.subscribe = vi.fn(callback => { callback?.('SUBSCRIBED'); return channel; });
  return {
    supabase: {
      from: vi.fn((table: string) => table === 'chat_participants' ? participantQuery : makeChain(table)),
      channel: vi.fn(() => channel),
      removeChannel: vi.fn(async () => undefined),
      rpc: vi.fn(async () => ({ error: null, data: null })),
      storage: { from: vi.fn(() => ({
        upload: vi.fn(async () => ({ error: null })),
        createSignedUrl: vi.fn(async (path: string) => ({ data: { signedUrl: `https://supabase.test/storage/v1/object/sign/web-chat-media/${path}?token=test` }, error: null })),
        remove: vi.fn(async () => ({ data: [], error: null })),
      })) },
    },
  };
});

import { ChatThread } from './ChatThread';
import type { UserProfile } from '@/types';

afterEach(() => vi.clearAllMocks());
beforeEach(() => {
  chatFixtures.history = [];
  chatFixtures.replies = [];
  chatFixtures.inserted = null;
  chatFixtures.syraInvocations = [];
  chatFixtures.messageInserts = [];
});

const user: UserProfile = { id: 'user-1', username: 'ashik', fullName: 'Ashik Ahmed', avatarUrl: undefined, followersCount: 0, followingCount: 0, postsCount: 0, createdAt: '2025-01-01T00:00:00.000Z' };

const message = (overrides: Record<string, unknown>) => ({
  id: 'message-1',
  chat_id: 'chat-1',
  sender_id: 'user-1',
  reply_to_id: null,
  content: 'Hello',
  message_type: 'text',
  content_state: 'active',
  created_at: '2026-10-11T17:00:00.000Z',
  is_deleted: false,
  expires_at: null,
  deleted_for_users: [],
  media_url: null,
  attachments: null,
  delivery_status: 'sent',
  message_state: 'sent',
  ...overrides,
});

describe('ChatThread layout and privacy controls', () => {
  it('keeps the composer in a fixed viewport-height chat shell while messages scroll independently', async () => {
    const { container } = render(<ChatThread chatId="chat-1" title="Taylor" subtitle="@taylor" avatarUrl={null} user={user} onBack={vi.fn()} />);
    await waitFor(() => expect(screen.queryByLabelText('Loading messages')).not.toBeInTheDocument());
    const shell = container.firstElementChild;
    const messageList = screen.getByRole('region', { name: 'Conversation messages' });
    const composer = screen.getByLabelText('Write a message (not end-to-end encrypted)').closest('form');

    expect(shell).toHaveClass('fixed', 'h-[100dvh]', 'overflow-hidden');
    expect((shell as HTMLElement).style.height).toMatch(/^\d+(?:\.\d+)?px$/);
    expect(messageList).toHaveClass('absolute', 'inset-0', 'overflow-y-auto', 'pb-32');
    expect(messageList).not.toHaveClass('scroll-smooth');
    expect(composer).toHaveClass('pointer-events-auto', 'max-w-3xl');
    expect(composer?.parentElement).toHaveClass('absolute', 'bottom-0');
  });

  it('jumps directly to the latest loaded message on initial open', async () => {
    const originalScrollTo = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'scrollTo');
    const scrollTo = vi.fn();
    Object.defineProperty(HTMLElement.prototype, 'scrollTo', { configurable: true, value: scrollTo });
    chatFixtures.history = [
      message({ id: 'older-message', created_at: '2026-10-11T16:00:00.000Z' }),
      message({ id: 'latest-message', created_at: '2026-10-11T17:00:00.000Z' }),
    ];

    try {
      render(<ChatThread chatId="chat-1" title="Taylor" subtitle="@taylor" avatarUrl={null} user={user} onBack={vi.fn()} />);
      await waitFor(() => expect(screen.queryByLabelText('Loading messages')).not.toBeInTheDocument());
      await waitFor(() => expect(scrollTo).toHaveBeenCalledWith(expect.objectContaining({ behavior: 'auto' })));
    } finally {
      if (originalScrollTo) Object.defineProperty(HTMLElement.prototype, 'scrollTo', originalScrollTo);
      else delete (HTMLElement.prototype as Partial<HTMLElement>).scrollTo;
    }
  });

  it('expands the floating input when focused and relaxes when idle', async () => {
    render(<ChatThread chatId="chat-1" title="Taylor" subtitle="@taylor" avatarUrl={null} user={user} onBack={vi.fn()} />);
    await waitFor(() => expect(screen.queryByLabelText('Loading messages')).not.toBeInTheDocument());
    const input = screen.getByLabelText('Write a message (not end-to-end encrypted)');
    const bar = input.closest('form');
    expect(bar).toHaveClass('w-[88%]');
    fireEvent.focus(input);
    expect(bar).toHaveClass('w-full');
    fireEvent.blur(input);
    expect(bar).toHaveClass('w-[88%]');
  });

  it('shows a quoted original inside a reply bubble and keeps delivery state icon-only', async () => {
    chatFixtures.history = [
      message({ id: 'reply-1', sender_id: 'syra-user', reply_to_id: 'original-1', content: 'I can help with that.' }),
      message({ id: 'sent-1', sender_id: 'user-1', content: 'Thanks!' }),
    ];
    chatFixtures.replies = [message({ id: 'original-1', sender_id: 'user-1', content: 'Can you take a look at this?' })];

    render(<ChatThread chatId="chat-1" title="Syra" subtitle="@syra" avatarUrl={null} user={user} onBack={vi.fn()} />);

    expect(await screen.findByText('Replying to You')).toBeVisible();
    expect(screen.getByText('Can you take a look at this?')).toBeVisible();
    expect(screen.getByText('I can help with that.')).toBeVisible();
    expect(screen.getByLabelText('Sent')).toBeInTheDocument();
    expect(screen.queryByText('Sent')).not.toBeInTheDocument();
  });

  it('renders file messages with filename, size, type, and an open/download action', async () => {
    chatFixtures.history = [message({
      id: 'file-1',
      content: 'Project_Proposal.pdf',
      message_type: 'file',
      media_url: 'https://files.example/project.pdf',
      attachments: [{ url: 'https://files.example/project.pdf', type: 'file', name: 'Project_Proposal.pdf', mimeType: 'application/pdf', size: 2516582 }],
    })];

    render(<ChatThread chatId="chat-1" title="Taylor" subtitle="@taylor" avatarUrl={null} user={user} onBack={vi.fn()} />);

    expect(await screen.findByText('Project_Proposal.pdf')).toBeVisible();
    expect(screen.getByText('2.4 MB · PDF')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Open or download Project_Proposal.pdf' })).toHaveAttribute('download', 'Project_Proposal.pdf');
  });

  it('renders a custom voice player with seek progress and speed controls', async () => {
    chatFixtures.history = [message({
      id: 'voice-1',
      content: 'Voice message',
      message_type: 'audio',
      media_url: 'https://files.example/voice.m4a',
      attachments: [{ url: 'https://files.example/voice.m4a', type: 'audio', name: 'voice-note.m4a', size: 24576, duration: 2 }],
    })];

    render(<ChatThread chatId="chat-1" title="Taylor" subtitle="@taylor" avatarUrl={null} user={user} onBack={vi.fn()} />);

    expect(await screen.findByRole('button', { name: 'Play voice message' })).toBeVisible();
    expect(screen.getByRole('slider', { name: 'Audio progress' })).toHaveAttribute('aria-valuetext', '00:00 of 00:02');
    expect(screen.getByRole('button', { name: 'Playback speed 1x' })).toBeVisible();
  });

  it('renders a fast optimistic send with the message arrival animation', async () => {
    chatFixtures.inserted = message({ id: 'server-message-1', content: 'Quick hello' });
    render(<ChatThread chatId="chat-1" title="Taylor" subtitle="@taylor" avatarUrl={null} user={user} onBack={vi.fn()} />);
    await waitFor(() => expect(screen.queryByLabelText('Loading messages')).not.toBeInTheDocument());

    const input = screen.getByLabelText('Write a message (not end-to-end encrypted)');
    fireEvent.change(input, { target: { value: 'Quick hello' } });
    fireEvent.submit(input.closest('form')!);

    await waitFor(() => {
      expect(screen.getAllByText('Quick hello')).toHaveLength(1);
      expect(screen.getByText('Quick hello').closest('.chat-message-send-enter')).not.toBeNull();
    });
  });

  it('shows three animated dots only while a recent Syra invocation is processing', async () => {
    chatFixtures.syraInvocations = [{
      request_id: 'syra-dm-message-1',
      status: 'processing',
      user_id: 'user-1',
      chat_id: 'chat-1',
      created_at: new Date().toISOString(),
    }];

    render(<ChatThread chatId="chat-1" title="Syra" subtitle="@syra" avatarUrl={null} user={user} onBack={vi.fn()} />);

    const indicator = await screen.findByRole('status', { name: 'Syra is typing' });
    expect(indicator).toBeVisible();
    expect(indicator.querySelectorAll('.h-2.w-2')).toHaveLength(3);
  });

  it('does not show a typing indicator for a completed Syra invocation', async () => {
    chatFixtures.syraInvocations = [{
      request_id: 'syra-dm-message-1',
      status: 'completed',
      user_id: 'user-1',
      chat_id: 'chat-1',
      created_at: new Date().toISOString(),
    }];

    render(<ChatThread chatId="chat-1" title="Syra" subtitle="@syra" avatarUrl={null} user={user} onBack={vi.fn()} />);

    await waitFor(() => expect(screen.queryByLabelText('Loading messages')).not.toBeInTheDocument());
    expect(screen.queryByRole('status', { name: 'Syra is typing' })).not.toBeInTheDocument();
  });

  it('sends selected media files as one message with a stacked attachment list', async () => {
    const firstUrl = 'https://supabase.test/storage/v1/object/sign/web-chat-media/chat-1/user-1/one.png?token=test';
    const secondUrl = 'https://supabase.test/storage/v1/object/sign/web-chat-media/chat-1/user-1/two.gif?token=test';
    chatFixtures.inserted = message({
      id: 'batch-message',
      content: '',
      message_type: 'image',
      media_url: firstUrl,
      attachments: [
        { url: firstUrl, type: 'image', name: 'one.png', mimeType: 'image/png', size: 1 },
        { url: secondUrl, type: 'image', name: 'two.gif', mimeType: 'image/gif', size: 1 },
      ],
    });
    const { container } = render(<ChatThread chatId="chat-1" title="Taylor" subtitle="@taylor" avatarUrl={null} user={user} onBack={vi.fn()} />);
    await waitFor(() => expect(screen.queryByLabelText('Loading messages')).not.toBeInTheDocument());

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;
    expect(fileInput.multiple).toBe(true);
    fireEvent.change(fileInput, { target: { files: [
      new File(['first'], 'one.png', { type: 'image/png' }),
      new File(['second'], 'two.gif', { type: 'image/gif' }),
    ] } });

    const batchDeck = await screen.findByRole('group', { name: '2 media items' });
    expect(batchDeck.closest('.chat-batch-media-message')).toHaveClass('bg-transparent', 'px-0', 'py-0', 'shadow-none', 'overflow-visible');
    await waitFor(() => expect(chatFixtures.messageInserts).toHaveLength(1));
    expect(chatFixtures.messageInserts[0].message_type).toBe('image');
    expect(chatFixtures.messageInserts[0].attachments).toHaveLength(2);

    fireEvent.click(screen.getByRole('button', { name: 'Open media gallery' }));
    expect(await screen.findByRole('dialog', { name: 'Media gallery' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'View media 2' }));
    expect(await screen.findByRole('dialog', { name: 'Media viewer' })).toBeInTheDocument();
    expect(screen.getByText('2 / 2')).toBeInTheDocument();
  });

  it('opens the share sheet from the composer plus button', async () => {
    render(<ChatThread chatId="chat-1" title="Taylor" subtitle="@taylor" avatarUrl={null} user={user} onBack={vi.fn()} />);
    await waitFor(() => expect(screen.queryByLabelText('Loading messages')).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Open message types' }));
    expect(screen.getByRole('dialog', { name: 'Share something' })).toBeVisible();
    expect(screen.getByRole('button', { name: /Photos & video/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Voice note/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Contact/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Location/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Link preview/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /^Poll/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Code snippet/ })).toBeVisible();
  });

  it('sends a contact card from the message-type sheet', async () => {
    chatFixtures.inserted = message({
      id: 'contact-card-1',
      content: 'Contact · Alex Johnson',
      message_type: 'contact',
      metadata: { type: 'contact', contact: { name: 'Alex Johnson', phone: '+12345678900', email: null } },
    });
    render(<ChatThread chatId="chat-1" title="Taylor" subtitle="@taylor" avatarUrl={null} user={user} onBack={vi.fn()} />);
    await waitFor(() => expect(screen.queryByLabelText('Loading messages')).not.toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Open message types' }));
    fireEvent.click(screen.getByRole('button', { name: /^Contact/ }));
    fireEvent.change(screen.getByLabelText('Contact name'), { target: { value: 'Alex Johnson' } });
    fireEvent.change(screen.getByLabelText('Phone number'), { target: { value: '+12345678900' } });
    fireEvent.click(screen.getByRole('button', { name: 'Share contact' }));

    await waitFor(() => expect(chatFixtures.messageInserts).toEqual([expect.objectContaining({ message_type: 'contact', metadata: expect.objectContaining({ type: 'contact' }) })]));
    expect(await screen.findByText('Alex Johnson')).toBeVisible();
    expect(screen.queryByRole('dialog', { name: 'Share a contact' })).not.toBeInTheDocument();
  });

  it('shows the profile area, privacy disclosure, share sheet trigger, and recording control', async () => {
    render(<ChatThread chatId="chat-1" title="Taylor" subtitle="@taylor" avatarUrl={null} user={user} onBack={vi.fn()} />);
    await waitFor(() => expect(screen.queryByLabelText('Loading messages')).not.toBeInTheDocument());

    expect(screen.getByRole('heading', { name: 'Taylor' })).toBeInTheDocument();
    expect(screen.getByText('Web chat is not end-to-end encrypted.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Open message types' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Record voice message' })).toBeVisible();
  });
});
