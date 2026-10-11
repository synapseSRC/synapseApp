import { describe, expect, it } from 'vitest';
import { collectActiveSyraRequests, isFreshSyraInvocation } from './syraTypingStatus';

const now = Date.parse('2026-10-11T17:10:00.000Z');
const row = {
  request_id: 'syra-dm-message-1',
  status: 'processing',
  user_id: 'ashik-id',
  chat_id: 'ashik-syra-chat',
  created_at: '2026-10-11T17:09:30.000Z',
};

describe('Syra typing status', () => {
  it('shows only a recent, real processing invocation for this user and chat', () => {
    expect(isFreshSyraInvocation(row, 'ashik-id', 'ashik-syra-chat', now)).toBe(true);
    expect(isFreshSyraInvocation({ ...row, status: 'completed' }, 'ashik-id', 'ashik-syra-chat', now)).toBe(false);
    expect(isFreshSyraInvocation(row, 'someone-else', 'ashik-syra-chat', now)).toBe(false);
    expect(isFreshSyraInvocation(row, 'ashik-id', 'another-chat', now)).toBe(false);
  });

  it('hides stale processing rows rather than leaving a stuck typing indicator', () => {
    const stale = { ...row, created_at: '2026-10-11T17:05:00.000Z' };
    expect(isFreshSyraInvocation(stale, 'ashik-id', 'ashik-syra-chat', now)).toBe(false);
    expect(collectActiveSyraRequests([stale], 'ashik-id', 'ashik-syra-chat', now)).toEqual({});
  });

  it('collects only active requests for the current user and conversation', () => {
    const rows = [
      row,
      { ...row, request_id: 'done', status: 'completed' },
      { ...row, request_id: 'other-user', user_id: 'other' },
    ];
    expect(collectActiveSyraRequests(rows, 'ashik-id', 'ashik-syra-chat', now)).toEqual({
      'syra-dm-message-1': row.created_at,
    });
  });
});
