import React, { useEffect, useState } from 'react';
import { ArrowLeft, BarChart3, ChevronRight, Code2, Contact, FileText, Image, Link2, Loader2, MapPin, Mic, Plus, Send, X } from 'lucide-react';

export type ChatTypedMessageType = 'contact' | 'location' | 'link_preview' | 'poll' | 'code_snippet';
export type ChatPickerKind = 'media' | 'file';

export interface ChatTypedMessageDraft {
  type: ChatTypedMessageType;
  content: string;
  metadata: Record<string, unknown>;
}

interface ChatComposerSheetProps {
  isSending: boolean;
  onClose: () => void;
  onChooseMedia: (kind: ChatPickerKind) => void;
  onStartVoice: () => void;
  onSend: (draft: ChatTypedMessageDraft) => Promise<boolean>;
}

type Panel = 'menu' | ChatTypedMessageType;
const inputClass = 'mt-1.5 h-11 w-full rounded-xl border border-zinc-200 bg-white px-3 text-sm text-zinc-900 outline-none placeholder:text-zinc-400 focus:border-zinc-500 focus:ring-2 focus:ring-zinc-200 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-100 dark:focus:border-zinc-500 dark:focus:ring-zinc-800';
const textareaClass = 'mt-1.5 min-h-24 w-full resize-y rounded-xl border border-zinc-200 bg-white px-3 py-2.5 text-sm leading-5 text-zinc-900 outline-none placeholder:text-zinc-400 focus:border-zinc-500 focus:ring-2 focus:ring-zinc-200 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-100 dark:focus:border-zinc-500 dark:focus:ring-zinc-800';

const normalizeWebUrl = (raw: string): URL | null => {
  const value = raw.trim();
  if (!value) return null;
  try {
    const url = new URL(/^https?:\/\//i.test(value) ? value : `https://${value}`);
    return url.protocol === 'https:' || url.protocol === 'http:' ? url : null;
  } catch {
    return null;
  }
};

const ActionButton: React.FC<{ children: React.ReactNode; disabled?: boolean; onClick: () => void; busy?: boolean }> = ({ children, disabled, onClick, busy }) => (
  <button type="button" disabled={disabled || busy} onClick={onClick} className="mt-4 inline-flex h-11 w-full items-center justify-center gap-2 rounded-full bg-zinc-950 px-5 text-sm font-semibold text-white transition hover:bg-zinc-800 disabled:cursor-not-allowed disabled:bg-zinc-300 dark:bg-zinc-100 dark:text-zinc-950 dark:hover:bg-zinc-300 dark:disabled:bg-zinc-700">
    {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}{children}
  </button>
);

export const ChatComposerSheet: React.FC<ChatComposerSheetProps> = ({ isSending, onClose, onChooseMedia, onStartVoice, onSend }) => {
  const [panel, setPanel] = useState<Panel>('menu');
  const [error, setError] = useState<string | null>(null);
  const [contactName, setContactName] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [placeName, setPlaceName] = useState('');
  const [placeAddress, setPlaceAddress] = useState('');
  const [coords, setCoords] = useState<{ latitude: number; longitude: number } | null>(null);
  const [locationBusy, setLocationBusy] = useState(false);
  const [linkUrl, setLinkUrl] = useState('');
  const [linkTitle, setLinkTitle] = useState('');
  const [linkDescription, setLinkDescription] = useState('');
  const [linkImageUrl, setLinkImageUrl] = useState('');
  const [pollQuestion, setPollQuestion] = useState('');
  const [pollOptions, setPollOptions] = useState(['', '']);
  const [pollDurationHours, setPollDurationHours] = useState(24);
  const [codeLanguage, setCodeLanguage] = useState('Kotlin');
  const [codeFilename, setCodeFilename] = useState('');
  const [codeDescription, setCodeDescription] = useState('');
  const [code, setCode] = useState('');

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => { if (event.key === 'Escape' && !isSending) onClose(); };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isSending, onClose]);

  const send = async (draft: ChatTypedMessageDraft) => {
    setError(null);
    if (await onSend(draft)) onClose();
  };

  const pickCurrentLocation = () => {
    if (!navigator.geolocation) {
      setError('Location sharing is not supported in this browser. You can enter a place manually.');
      return;
    }
    setLocationBusy(true);
    setError(null);
    navigator.geolocation.getCurrentPosition(
      position => {
        setCoords({ latitude: position.coords.latitude, longitude: position.coords.longitude });
        setPlaceName(current => current || 'Current location');
        setLocationBusy(false);
      },
      geolocationError => {
        setError(geolocationError.code === geolocationError.PERMISSION_DENIED ? 'Location access was denied. Enter a place manually instead.' : 'Could not get your location. Try again or enter a place manually.');
        setLocationBusy(false);
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 },
    );
  };

  const sendContact = () => {
    const name = contactName.trim();
    const phone = contactPhone.trim();
    if (!name || !phone) { setError('Add a contact name and phone number.'); return; }
    void send({ type: 'contact', content: `Contact · ${name}`, metadata: { type: 'contact', contact: { name, phone, email: contactEmail.trim() || null } } });
  };

  const sendLocation = () => {
    const name = placeName.trim();
    const address = placeAddress.trim();
    if (!name && !address && !coords) { setError('Enter a place or share your current location.'); return; }
    void send({ type: 'location', content: `Location · ${name || address || 'Shared location'}`, metadata: { type: 'location', location: { name: name || 'Shared location', address: address || null, latitude: coords?.latitude ?? null, longitude: coords?.longitude ?? null } } });
  };

  const sendLink = () => {
    const normalized = normalizeWebUrl(linkUrl);
    if (!normalized) { setError('Enter a valid http or https link.'); return; }
    const imageUrl = linkImageUrl.trim() ? normalizeWebUrl(linkImageUrl) : null;
    if (linkImageUrl.trim() && !imageUrl) { setError('The preview image must use a valid http or https URL.'); return; }
    const title = linkTitle.trim() || normalized.hostname;
    void send({ type: 'link_preview', content: `Link · ${title}`, metadata: { type: 'link_preview', link_preview: { url: normalized.toString(), title, description: linkDescription.trim() || null, image_url: imageUrl?.toString() || null, domain: normalized.hostname } } });
  };

  const sendPoll = () => {
    const question = pollQuestion.trim();
    const options = pollOptions.map(option => option.trim()).filter(Boolean);
    if (!question || options.length < 2) { setError('A poll needs a question and at least two options.'); return; }
    if (options.length > 4) { setError('A poll can have no more than four options.'); return; }
    if (new Set(options.map(option => option.toLowerCase())).size !== options.length) { setError('Poll options must be different.'); return; }
    const endsAt = new Date(Date.now() + pollDurationHours * 60 * 60 * 1000).toISOString();
    void send({ type: 'poll', content: `Poll · ${question}`, metadata: { type: 'poll', poll: { question, options, ends_at: endsAt, allow_multiple: false } } });
  };

  const sendCode = () => {
    const source = code.trim();
    if (!source) { setError('Add the code you want to share.'); return; }
    if (source.length > 20000) { setError('Code snippets are limited to 20,000 characters.'); return; }
    const filename = codeFilename.trim();
    void send({ type: 'code_snippet', content: `Code · ${filename || codeLanguage}`, metadata: { type: 'code_snippet', code_snippet: { language: codeLanguage, filename: filename || null, description: codeDescription.trim() || null, code: source } } });
  };

  const backToMenu = () => { setPanel('menu'); setError(null); };
  const title = panel === 'menu' ? 'Share something' : ({ contact: 'Share a contact', location: 'Share a location', link_preview: 'Share a link', poll: 'Create a poll', code_snippet: 'Share code' } as const)[panel];
  const headingId = 'chat-composer-sheet-title';

  const rows: Array<{ label: string; description: string; icon: React.ReactNode; action: () => void }> = [
    { label: 'Photos & video', description: 'Images, videos, audio · up to 10 items', icon: <Image className="h-5 w-5" />, action: () => onChooseMedia('media') },
    { label: 'Voice note', description: 'Record a short audio message', icon: <Mic className="h-5 w-5" />, action: onStartVoice },
    { label: 'File', description: 'Share a PDF document', icon: <FileText className="h-5 w-5" />, action: () => onChooseMedia('file') },
    { label: 'Contact', description: 'Share a name and phone number', icon: <Contact className="h-5 w-5" />, action: () => { setPanel('contact'); setError(null); } },
    { label: 'Location', description: 'Share a place or current location', icon: <MapPin className="h-5 w-5" />, action: () => { setPanel('location'); setError(null); } },
    { label: 'Link preview', description: 'Send a link with a preview card', icon: <Link2 className="h-5 w-5" />, action: () => { setPanel('link_preview'); setError(null); } },
    { label: 'Poll', description: 'Ask a question and collect votes', icon: <BarChart3 className="h-5 w-5" />, action: () => { setPanel('poll'); setError(null); } },
    { label: 'Code snippet', description: 'Share formatted code and a language', icon: <Code2 className="h-5 w-5" />, action: () => { setPanel('code_snippet'); setError(null); } },
  ];

  return (
    <div className="fixed inset-0 z-[100] flex items-end justify-center bg-black/45 sm:items-center sm:p-5" onMouseDown={event => { if (event.target === event.currentTarget && !isSending) onClose(); }}>
      <section role="dialog" aria-modal="true" aria-labelledby={headingId} className="flex max-h-[min(88dvh,48rem)] w-full max-w-lg flex-col overflow-hidden rounded-t-[1.75rem] border border-zinc-200 bg-zinc-50 shadow-2xl dark:border-zinc-700 dark:bg-zinc-950 sm:rounded-[1.75rem]">
        <div className="flex shrink-0 items-center gap-2 border-b border-zinc-200 bg-white px-4 py-3 dark:border-zinc-800 dark:bg-zinc-900">
          {panel !== 'menu' ? <button type="button" onClick={backToMenu} aria-label="Back to message types" className="grid h-9 w-9 shrink-0 place-items-center rounded-full text-zinc-600 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800"><ArrowLeft className="h-4 w-4" /></button> : <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-zinc-950 text-white dark:bg-zinc-100 dark:text-zinc-950"><Plus className="h-4 w-4" /></span>}
          <h2 id={headingId} className="min-w-0 flex-1 text-sm font-semibold text-zinc-950 dark:text-zinc-100">{title}</h2>
          <button type="button" onClick={onClose} aria-label="Close share sheet" className="grid h-9 w-9 shrink-0 place-items-center rounded-full text-zinc-600 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800"><X className="h-4 w-4" /></button>
        </div>

        {panel === 'menu' ? <div className="min-h-0 overflow-y-auto p-3 sm:p-4">
          <p className="px-1 pb-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-zinc-500">Message types</p>
          <div className="space-y-1">
            {rows.map(row => <button key={row.label} type="button" onClick={row.action} className="flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left transition-colors hover:bg-zinc-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-500 dark:hover:bg-zinc-900">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl border border-zinc-200 bg-white text-zinc-800 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100">{row.icon}</span>
              <span className="min-w-0 flex-1"><span className="block text-sm font-medium text-zinc-900 dark:text-zinc-100">{row.label}</span><span className="mt-0.5 block text-xs text-zinc-500 dark:text-zinc-400">{row.description}</span></span>
              <ChevronRight className="h-4 w-4 shrink-0 text-zinc-400" />
            </button>)}
          </div>
          <p className="mt-3 rounded-xl border border-zinc-200 bg-white px-3 py-2.5 text-[11px] leading-5 text-zinc-500 dark:border-zinc-800 dark:bg-zinc-900 dark:text-zinc-400">Web chats are not end-to-end encrypted. Only share information you are comfortable sending through Synapse.</p>
        </div> : <div className="min-h-0 overflow-y-auto p-4 sm:p-5">
          {panel === 'contact' && <div className="space-y-3">
            <p className="text-xs leading-5 text-zinc-500 dark:text-zinc-400">Share a contact card. Recipients can call or save the details.</p>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Contact name<input autoFocus value={contactName} onChange={event => setContactName(event.target.value)} maxLength={100} placeholder="Alex Johnson" className={inputClass} /></label>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Phone number<input value={contactPhone} onChange={event => setContactPhone(event.target.value)} maxLength={40} type="tel" placeholder="+1 234 567 8900" className={inputClass} /></label>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Email <span className="font-normal text-zinc-400">(optional)</span><input value={contactEmail} onChange={event => setContactEmail(event.target.value)} maxLength={160} type="email" placeholder="alex@example.com" className={inputClass} /></label>
            <ActionButton onClick={sendContact} disabled={isSending || !contactName.trim() || !contactPhone.trim()} busy={isSending}>Share contact</ActionButton>
          </div>}

          {panel === 'location' && <div className="space-y-3">
            <p className="text-xs leading-5 text-zinc-500 dark:text-zinc-400">Share your current coordinates or enter a place manually. Your location is sent only after you confirm.</p>
            <button type="button" onClick={pickCurrentLocation} disabled={locationBusy || isSending} className="inline-flex h-10 items-center gap-2 rounded-full border border-zinc-300 bg-white px-4 text-xs font-semibold text-zinc-800 hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100 dark:hover:bg-zinc-800">{locationBusy ? <Loader2 className="h-4 w-4 animate-spin" /> : <MapPin className="h-4 w-4" />}{coords ? 'Location added' : 'Use current location'}</button>
            {coords && <p className="rounded-xl bg-zinc-100 px-3 py-2 text-xs text-zinc-600 dark:bg-zinc-900 dark:text-zinc-300">{coords.latitude.toFixed(5)}, {coords.longitude.toFixed(5)}</p>}
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Place name<input autoFocus value={placeName} onChange={event => setPlaceName(event.target.value)} maxLength={100} placeholder="Central Park" className={inputClass} /></label>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Address <span className="font-normal text-zinc-400">(optional)</span><input value={placeAddress} onChange={event => setPlaceAddress(event.target.value)} maxLength={200} placeholder="New York, NY" className={inputClass} /></label>
            <ActionButton onClick={sendLocation} disabled={isSending || (!placeName.trim() && !placeAddress.trim() && !coords)} busy={isSending}>Share location</ActionButton>
          </div>}

          {panel === 'link_preview' && <div className="space-y-3">
            <p className="text-xs leading-5 text-zinc-500 dark:text-zinc-400">Add link details for the preview card. Synapse does not fetch third-party page metadata automatically.</p>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Link URL<input autoFocus value={linkUrl} onChange={event => setLinkUrl(event.target.value)} maxLength={2000} type="url" placeholder="https://example.com/article" className={inputClass} /></label>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Title <span className="font-normal text-zinc-400">(optional)</span><input value={linkTitle} onChange={event => setLinkTitle(event.target.value)} maxLength={160} placeholder="Page title" className={inputClass} /></label>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Description <span className="font-normal text-zinc-400">(optional)</span><textarea value={linkDescription} onChange={event => setLinkDescription(event.target.value)} maxLength={400} rows={2} placeholder="A short summary" className={`${textareaClass} min-h-20`} /></label>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Preview image URL <span className="font-normal text-zinc-400">(optional)</span><input value={linkImageUrl} onChange={event => setLinkImageUrl(event.target.value)} maxLength={2000} type="url" placeholder="https://example.com/image.jpg" className={inputClass} /></label>
            <ActionButton onClick={sendLink} disabled={isSending || !linkUrl.trim()} busy={isSending}>Share link</ActionButton>
          </div>}

          {panel === 'poll' && <div className="space-y-3">
            <p className="text-xs leading-5 text-zinc-500 dark:text-zinc-400">Recipients can cast one vote. Poll votes are visible to everyone in this chat.</p>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Question<input autoFocus value={pollQuestion} onChange={event => setPollQuestion(event.target.value)} maxLength={240} placeholder="Where should we go next?" className={inputClass} /></label>
            <div className="space-y-2"><span className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Options</span>{pollOptions.map((option, index) => <div key={index} className="flex items-center gap-2"><input value={option} onChange={event => setPollOptions(current => current.map((value, itemIndex) => itemIndex === index ? event.target.value : value))} maxLength={100} placeholder={`Option ${index + 1}`} className="h-10 min-w-0 flex-1 rounded-xl border border-zinc-200 bg-white px-3 text-sm outline-none focus:border-zinc-500 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-100" />{pollOptions.length > 2 && <button type="button" aria-label={`Remove option ${index + 1}`} onClick={() => setPollOptions(current => current.filter((_, itemIndex) => itemIndex !== index))} className="grid h-9 w-9 shrink-0 place-items-center rounded-full text-zinc-500 hover:bg-zinc-200 dark:hover:bg-zinc-800"><X className="h-4 w-4" /></button>}</div>)}</div>
            {pollOptions.length < 4 && <button type="button" onClick={() => setPollOptions(current => [...current, ''])} className="inline-flex items-center gap-1.5 rounded-full px-2 py-1 text-xs font-semibold text-zinc-700 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-900"><Plus className="h-3.5 w-3.5" />Add option</button>}
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Poll duration<select value={pollDurationHours} onChange={event => setPollDurationHours(Number(event.target.value))} className={inputClass}><option value={24}>1 day</option><option value={48}>2 days</option><option value={72}>3 days</option><option value={168}>7 days</option></select></label>
            <ActionButton onClick={sendPoll} disabled={isSending || !pollQuestion.trim() || pollOptions.filter(option => option.trim()).length < 2} busy={isSending}>Create poll</ActionButton>
          </div>}

          {panel === 'code_snippet' && <div className="space-y-3">
            <p className="text-xs leading-5 text-zinc-500 dark:text-zinc-400">Share code as a formatted card. Avoid secrets such as passwords, tokens, or private keys.</p>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Language<select autoFocus value={codeLanguage} onChange={event => setCodeLanguage(event.target.value)} className={inputClass}>{['Kotlin', 'Java', 'JavaScript', 'TypeScript', 'Python', 'Swift', 'SQL', 'JSON', 'HTML', 'CSS', 'Bash', 'Plain text'].map(language => <option key={language}>{language}</option>)}</select></label>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">File name <span className="font-normal text-zinc-400">(optional)</span><input value={codeFilename} onChange={event => setCodeFilename(event.target.value)} maxLength={120} placeholder="greeting.kt" className={inputClass} /></label>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Description <span className="font-normal text-zinc-400">(optional)</span><input value={codeDescription} onChange={event => setCodeDescription(event.target.value)} maxLength={200} placeholder="A short explanation" className={inputClass} /></label>
            <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">Code<textarea value={code} onChange={event => setCode(event.target.value)} maxLength={20000} rows={8} placeholder={'fun greet(name: String) = "Hello, $name!"'} className={`${textareaClass} font-mono text-xs`} /></label>
            <ActionButton onClick={sendCode} disabled={isSending || !code.trim()} busy={isSending}>Share code</ActionButton>
          </div>}

          {error && <p role="alert" className="mt-3 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-xs leading-5 text-rose-800 dark:border-rose-900 dark:bg-rose-950/40 dark:text-rose-200">{error}</p>}
        </div>}
      </section>
    </div>
  );
};
