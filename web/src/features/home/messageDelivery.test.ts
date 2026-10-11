import { describe, expect, it } from 'vitest';
import { getMessageDeliveryState } from './messageDelivery';

describe('message delivery state', () => {
  it('shows local send and failure states before a database row exists', () => {
    expect(getMessageDeliveryState({ localStatus: 'sending' })).toBe('sending');
    expect(getMessageDeliveryState({ localStatus: 'sent' })).toBe('sent');
    expect(getMessageDeliveryState({ localStatus: 'failed', message_state: 'sent' })).toBe('failed');
  });

  it('maps persisted sent, delivered, and read receipts', () => {
    expect(getMessageDeliveryState({ message_state: 'sent', delivery_status: 'sent' })).toBe('sent');
    expect(getMessageDeliveryState({ message_state: 'delivered' })).toBe('delivered');
    expect(getMessageDeliveryState({ delivery_status: 'read', read_at: '2026-01-01T00:00:00Z' })).toBe('read');
  });
});
