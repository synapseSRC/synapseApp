import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ChatMessageCards } from './ChatMessageCards';

describe('ChatMessageCards', () => {
  it('renders a contact card with a save-contact action', () => {
    render(<ChatMessageCards messageId="message-1" messageType="contact" userId="user-1" metadata={{ contact: { name: 'Alex Johnson', phone: '+12345678900' } }} />);
    expect(screen.getByRole('group', { name: 'Shared contact Alex Johnson' })).toBeVisible();
    expect(screen.getByRole('link', { name: '+12345678900' })).toHaveAttribute('href', 'tel:+12345678900');
    expect(screen.getByRole('button', { name: 'Save contact' })).toBeVisible();
  });

  it('submits a poll option and shows persisted results', () => {
    const onVote = vi.fn();
    const { rerender } = render(<ChatMessageCards
      messageId="poll-1"
      messageType="poll"
      userId="user-1"
      metadata={{ poll: { question: 'Where should we go?', options: ['Beach', 'Mountains'] } }}
      onVote={onVote}
    />);
    fireEvent.click(screen.getByRole('button', { name: /Beach/ }));
    expect(onVote).toHaveBeenCalledWith(0);

    rerender(<ChatMessageCards
      messageId="poll-1"
      messageType="poll"
      userId="user-1"
      metadata={{ poll: { question: 'Where should we go?', options: ['Beach', 'Mountains'] } }}
      pollVotes={[{ message_id: 'poll-1', user_id: 'user-1', option_index: 0 }, { message_id: 'poll-1', user_id: 'user-2', option_index: 1 }]}
      onVote={onVote}
    />);
    expect(screen.getByText('2 votes · Your vote is recorded')).toBeVisible();
    expect(screen.getByRole('button', { name: /Beach/ })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: /Beach/ })).toBeDisabled();
  });
});
