export type MessageDeliveryState = 'sending' | 'sent' | 'delivered' | 'read' | 'failed';

export interface MessageDeliveryFields {
  delivery_status?: string | null;
  message_state?: string | null;
  delivered_at?: string | null;
  read_at?: string | null;
  localStatus?: 'sending' | 'sent' | 'failed';
}

export const getMessageDeliveryState = (message: MessageDeliveryFields): MessageDeliveryState => {
  if (message.localStatus) return message.localStatus;
  const states = [message.message_state, message.delivery_status].map(value => value?.toLowerCase());
  if (states.includes('failed')) return 'failed';
  if (message.read_at || states.includes('read')) return 'read';
  if (message.delivered_at || states.includes('delivered')) return 'delivered';
  if (states.includes('sending')) return 'sending';
  return 'sent';
};

export const deliveryLabel: Record<MessageDeliveryState, string> = {
  sending: 'Sending',
  sent: 'Sent',
  delivered: 'Delivered',
  read: 'Read',
  failed: 'Failed',
};
