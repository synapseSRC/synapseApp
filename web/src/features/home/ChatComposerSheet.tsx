import React, { useEffect, useState } from 'react';
import { ArrowLeft, BarChart3, ChevronRight, Code2, Contact, FileText, Image, Link2, MapPin, Mic, Plus, X } from 'lucide-react';
import { CodeSnippetPanel, ContactPanel, LinkPreviewPanel, LocationPanel, PollPanel, type ComposerPanelProps } from './ChatComposerPanels';
import type { ChatPickerKind, ChatTypedMessageDraft, ChatTypedMessageType } from './chatComposerTypes';

export type { ChatPickerKind, ChatTypedMessageDraft, ChatTypedMessageType } from './chatComposerTypes';

interface ChatComposerSheetProps {
  isSending: boolean;
  onClose: () => void;
  onChooseMedia: (kind: ChatPickerKind) => void;
  onStartVoice: () => void;
  onSend: (draft: ChatTypedMessageDraft) => Promise<boolean>;
}

type Panel = 'menu' | ChatTypedMessageType;

const PANEL_ORDER: ChatTypedMessageType[] = ['contact', 'location', 'link_preview', 'poll', 'code_snippet'];
const PANEL_COMPONENTS: Record<ChatTypedMessageType, React.ComponentType<ComposerPanelProps>> = {
  contact: ContactPanel,
  location: LocationPanel,
  link_preview: LinkPreviewPanel,
  poll: PollPanel,
  code_snippet: CodeSnippetPanel,
};
const PANEL_TITLES: Record<ChatTypedMessageType, string> = {
  contact: 'Share a contact',
  location: 'Share a location',
  link_preview: 'Share a link',
  poll: 'Create a poll',
  code_snippet: 'Share code',
};

export const ChatComposerSheet: React.FC<ChatComposerSheetProps> = ({ isSending, onClose, onChooseMedia, onStartVoice, onSend }) => {
  const [panel, setPanel] = useState<Panel>('menu');

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => { if (event.key === 'Escape' && !isSending) onClose(); };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isSending, onClose]);

  const send = async (draft: ChatTypedMessageDraft) => {
    const sent = await onSend(draft);
    if (sent) onClose();
    return sent;
  };
  const backToMenu = () => setPanel('menu');
  const title = panel === 'menu' ? 'Share something' : PANEL_TITLES[panel];
  const headingId = 'chat-composer-sheet-title';
  const rows: Array<{ label: string; description: string; icon: React.ReactNode; action: () => void }> = [
    { label: 'Photos & video', description: 'Images, videos, audio · up to 10 items', icon: <Image className="h-5 w-5" />, action: () => onChooseMedia('media') },
    { label: 'Voice note', description: 'Record a short audio message', icon: <Mic className="h-5 w-5" />, action: onStartVoice },
    { label: 'File', description: 'Share a PDF document', icon: <FileText className="h-5 w-5" />, action: () => onChooseMedia('file') },
    { label: 'Contact', description: 'Share a name and phone number', icon: <Contact className="h-5 w-5" />, action: () => setPanel('contact') },
    { label: 'Location', description: 'Share a place or current location', icon: <MapPin className="h-5 w-5" />, action: () => setPanel('location') },
    { label: 'Link preview', description: 'Send a link with a preview card', icon: <Link2 className="h-5 w-5" />, action: () => setPanel('link_preview') },
    { label: 'Poll', description: 'Ask a question and collect votes', icon: <BarChart3 className="h-5 w-5" />, action: () => setPanel('poll') },
    { label: 'Code snippet', description: 'Share formatted code and a language', icon: <Code2 className="h-5 w-5" />, action: () => setPanel('code_snippet') },
  ];

  return (
    <div className="fixed inset-0 z-[100] flex items-end justify-center bg-foreground/40 sm:items-center sm:p-5" onMouseDown={event => { if (event.target === event.currentTarget && !isSending) onClose(); }}>
      <section role="dialog" aria-modal="true" aria-labelledby={headingId} className="flex max-h-[min(88dvh,48rem)] w-full max-w-lg flex-col overflow-hidden rounded-t-[1.75rem] border border-border bg-background shadow-2xl sm:rounded-[1.75rem]">
        <div className="flex shrink-0 items-center gap-2 border-b border-border bg-card px-4 py-3">
          {panel !== 'menu' ? <button type="button" onClick={backToMenu} aria-label="Back to message types" className="grid h-9 w-9 shrink-0 place-items-center rounded-full text-muted-foreground hover:bg-muted"><ArrowLeft className="h-4 w-4" /></button> : <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-foreground text-background"><Plus className="h-4 w-4" /></span>}
          <h2 id={headingId} className="min-w-0 flex-1 text-sm font-semibold text-card-foreground">{title}</h2>
          <button type="button" onClick={onClose} aria-label="Close share sheet" className="grid h-9 w-9 shrink-0 place-items-center rounded-full text-muted-foreground hover:bg-muted"><X className="h-4 w-4" /></button>
        </div>

        <div className={`min-h-0 overflow-y-auto p-3 sm:p-4 ${panel === 'menu' ? '' : 'hidden'}`}>
          <p className="px-1 pb-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-muted-foreground">Message types</p>
          <div className="space-y-1">
            {rows.map(row => <button key={row.label} type="button" onClick={row.action} className="flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left text-foreground transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl border border-border bg-card text-card-foreground">{row.icon}</span>
              <span className="min-w-0 flex-1"><span className="block text-sm font-medium text-card-foreground">{row.label}</span><span className="mt-0.5 block text-xs text-muted-foreground">{row.description}</span></span>
              <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
            </button>)}
          </div>
          <p className="mt-3 rounded-xl border border-border bg-card px-3 py-2.5 text-[11px] leading-5 text-muted-foreground">Web chats are not end-to-end encrypted. Only share information you are comfortable sending through Synapse.</p>
        </div>

        {PANEL_ORDER.map(type => {
          const PanelComponent = PANEL_COMPONENTS[type];
          return <div key={type} hidden={panel !== type} className="min-h-0 overflow-y-auto p-4 sm:p-5"><PanelComponent isSending={isSending} onSend={send} /></div>;
        })}
      </section>
    </div>
  );
};

