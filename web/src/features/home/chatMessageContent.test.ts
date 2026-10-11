import { describe, expect, it } from 'vitest';
import { getMessageDisplayText, isEncryptedMessageContent } from './chatMessageContent';

describe('chat message content handling', () => {
  it('recognizes recipient-keyed Android Signal envelopes', () => {
    expect(isEncryptedMessageContent(JSON.stringify({ recipient: { type: 3, body: 'ciphertext', registrationId: 42 } }))).toBe(true);
  });

  it('recognizes a direct encrypted envelope', () => {
    expect(isEncryptedMessageContent(JSON.stringify({ type: 1, body: 'ciphertext', registrationId: 42 }))).toBe(true);
  });

  it('does not classify regular text or ordinary JSON as encrypted', () => {
    expect(isEncryptedMessageContent('hello there')).toBe(false);
    expect(isEncryptedMessageContent(JSON.stringify({ content: 'readable text' }))).toBe(false);
    expect(isEncryptedMessageContent('{not json')).toBe(false);
  });

  it('unwraps a plaintext content wrapper and preserves regular text', () => {
    expect(getMessageDisplayText(JSON.stringify({ content: 'hello' }))).toBe('hello');
    expect(getMessageDisplayText('hello\nworld')).toBe('hello\nworld');
  });
});
