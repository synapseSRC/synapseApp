import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ChatComposerSheet } from './ChatComposerSheet';

describe('ChatComposerSheet', () => {
  it('creates a poll draft with options and a closing time', async () => {
    const onClose = vi.fn();
    const onSend = vi.fn(async () => true);
    render(<ChatComposerSheet isSending={false} onClose={onClose} onChooseMedia={vi.fn()} onStartVoice={vi.fn()} onSend={onSend} />);

    fireEvent.click(screen.getByRole('button', { name: /^Poll/ }));
    fireEvent.change(screen.getByLabelText('Question'), { target: { value: 'Where should we go?' } });
    fireEvent.change(screen.getByPlaceholderText('Option 1'), { target: { value: 'Beach' } });
    fireEvent.change(screen.getByPlaceholderText('Option 2'), { target: { value: 'Mountains' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create poll' }));

    await waitFor(() => expect(onSend).toHaveBeenCalledWith(expect.objectContaining({
      type: 'poll',
      content: 'Poll · Where should we go?',
      metadata: expect.objectContaining({
        type: 'poll',
        poll: expect.objectContaining({ question: 'Where should we go?', options: ['Beach', 'Mountains'], allow_multiple: false }),
      }),
    })));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('routes media and file choices to their existing pickers', () => {
    const onChooseMedia = vi.fn();
    render(<ChatComposerSheet isSending={false} onClose={vi.fn()} onChooseMedia={onChooseMedia} onStartVoice={vi.fn()} onSend={vi.fn(async () => true)} />);
    fireEvent.click(screen.getByRole('button', { name: /^Photos & video/ }));
    fireEvent.click(screen.getByRole('button', { name: /^File/ }));
    expect(onChooseMedia).toHaveBeenNthCalledWith(1, 'media');
    expect(onChooseMedia).toHaveBeenNthCalledWith(2, 'file');
  });
});
