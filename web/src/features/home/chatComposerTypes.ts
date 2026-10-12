export type ChatTypedMessageType = 'contact' | 'location' | 'link_preview' | 'poll' | 'code_snippet';
export type ChatPickerKind = 'media' | 'file';

export interface ChatTypedMessageDraft {
  type: ChatTypedMessageType;
  content: string;
  metadata: Record<string, unknown>;
}
