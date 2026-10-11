import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { BatchMediaDeck } from './BatchMediaDeck';
import type { MediaAttachment } from './mediaAttachmentUtils';

describe('BatchMediaDeck', () => {
  it('shows a stacked primary card and lets the user browse the other media items', () => {
    const attachments: MediaAttachment[] = [
      { url: 'https://media.example/one.gif', type: 'image', name: 'Animated GIF' },
      { url: 'https://media.example/two.mp4', type: 'video', name: 'Lake clip', duration: 24, width: 1280, height: 720 },
      { url: 'https://media.example/three.jpg', type: 'image', name: 'Mountains' },
    ];
    const onOpenGallery = vi.fn();
    render(<BatchMediaDeck attachments={attachments} getUrl={attachment => attachment.url} onMediaError={() => undefined} onOpenGallery={onOpenGallery} />);

    expect(screen.getByRole('group', { name: '3 media items' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Animated GIF' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show media 2 of 3' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Open media gallery' }));
    expect(onOpenGallery).toHaveBeenCalledOnce();

    fireEvent.click(screen.getByRole('button', { name: 'Next media item' }));

    expect(screen.getByRole('button', { name: 'Play video' })).toBeInTheDocument();
    expect(screen.getByText('0:24')).toBeInTheDocument();
    expect(screen.getByText('HD')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Previous media item' }));
    expect(screen.getByRole('img', { name: 'Animated GIF' })).toBeInTheDocument();
  });
});
