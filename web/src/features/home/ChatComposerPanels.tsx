import React, { useState } from 'react';
import { Loader2, MapPin, Send } from 'lucide-react';
import type { ChatTypedMessageDraft } from './chatComposerTypes';

export interface ComposerPanelProps {
  isSending: boolean;
  onSend: (draft: ChatTypedMessageDraft) => Promise<boolean>;
}

const inputClass = 'mt-1.5 h-11 w-full rounded-xl border border-border bg-background px-3 text-sm text-foreground outline-none placeholder:text-muted-foreground focus:border-ring focus:ring-2 focus:ring-ring/20';
const textareaClass = 'mt-1.5 min-h-24 w-full resize-y rounded-xl border border-border bg-background px-3 py-2.5 text-sm leading-5 text-foreground outline-none placeholder:text-muted-foreground focus:border-ring focus:ring-2 focus:ring-ring/20';

const PanelIntro: React.FC<{ children: React.ReactNode }> = ({ children }) => <p className="text-xs leading-5 text-muted-foreground">{children}</p>;
const ErrorMessage: React.FC<{ children: string | null }> = ({ children }) => children ? <p role="alert" className="mt-3 rounded-xl border border-destructive/30 bg-destructive/5 px-3 py-2 text-xs leading-5 text-destructive">{children}</p> : null;
const PanelAction: React.FC<{ children: React.ReactNode; disabled: boolean; isSending: boolean; onClick: () => void }> = ({ children, disabled, isSending, onClick }) => (
  <button type="button" disabled={disabled || isSending} onClick={onClick} className="mt-4 inline-flex h-11 w-full items-center justify-center gap-2 rounded-full bg-foreground px-5 text-sm font-semibold text-background transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50">
    {isSending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}{children}
  </button>
);

const sendDraft = (draft: ChatTypedMessageDraft, onSend: ComposerPanelProps['onSend'], setError: (message: string | null) => void) => {
  setError(null);
  void onSend(draft).then(sent => { if (!sent) setError('Could not send this message. Try again.'); }).catch(() => setError('Could not send this message. Try again.'));
};

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

export const ContactPanel: React.FC<ComposerPanelProps> = ({ isSending, onSend }) => {
  const [contactName, setContactName] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const submit = () => {
    const name = contactName.trim();
    const phone = contactPhone.trim();
    if (!name || !phone) { setError('Add a contact name and phone number.'); return; }
    sendDraft({ type: 'contact', content: `Contact · ${name}`, metadata: { type: 'contact', contact: { name, phone, email: contactEmail.trim() || null } } }, onSend, setError);
  };
  return <div className="space-y-3">
    <PanelIntro>Share a contact card. Recipients can call or save the details.</PanelIntro>
    <label className="block text-xs font-medium text-foreground">Contact name<input value={contactName} onChange={event => setContactName(event.target.value)} maxLength={100} placeholder="Alex Johnson" className={inputClass} /></label>
    <label className="block text-xs font-medium text-foreground">Phone number<input value={contactPhone} onChange={event => setContactPhone(event.target.value)} maxLength={40} type="tel" placeholder="+1 234 567 8900" className={inputClass} /></label>
    <label className="block text-xs font-medium text-foreground">Email <span className="font-normal text-muted-foreground">(optional)</span><input value={contactEmail} onChange={event => setContactEmail(event.target.value)} maxLength={160} type="email" placeholder="alex@example.com" className={inputClass} /></label>
    <PanelAction onClick={submit} disabled={!contactName.trim() || !contactPhone.trim()} isSending={isSending}>Share contact</PanelAction>
    <ErrorMessage>{error}</ErrorMessage>
  </div>;
};

export const LocationPanel: React.FC<ComposerPanelProps> = ({ isSending, onSend }) => {
  const [placeName, setPlaceName] = useState('');
  const [placeAddress, setPlaceAddress] = useState('');
  const [coords, setCoords] = useState<{ latitude: number; longitude: number } | null>(null);
  const [locationBusy, setLocationBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const pickCurrentLocation = () => {
    if (!navigator.geolocation) { setError('Location sharing is not supported in this browser. You can enter a place manually.'); return; }
    setLocationBusy(true);
    setError(null);
    navigator.geolocation.getCurrentPosition(
      position => { setCoords({ latitude: position.coords.latitude, longitude: position.coords.longitude }); setPlaceName(current => current || 'Current location'); setLocationBusy(false); },
      geoError => { setError(geoError.code === geoError.PERMISSION_DENIED ? 'Location access was denied. Enter a place manually instead.' : 'Could not get your location. Try again or enter a place manually.'); setLocationBusy(false); },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 },
    );
  };
  const submit = () => {
    const name = placeName.trim();
    const address = placeAddress.trim();
    if (!name && !address && !coords) { setError('Enter a place or share your current location.'); return; }
    sendDraft({ type: 'location', content: `Location · ${name || address || 'Shared location'}`, metadata: { type: 'location', location: { name: name || 'Shared location', address: address || null, latitude: coords?.latitude ?? null, longitude: coords?.longitude ?? null } } }, onSend, setError);
  };
  return <div className="space-y-3">
    <PanelIntro>Share your current coordinates or enter a place manually. Your location is sent only after you confirm.</PanelIntro>
    <button type="button" onClick={pickCurrentLocation} disabled={locationBusy || isSending} className="inline-flex h-10 items-center gap-2 rounded-full border border-border bg-background px-4 text-xs font-semibold text-foreground hover:bg-muted disabled:opacity-50">{locationBusy ? <Loader2 className="h-4 w-4 animate-spin" /> : <MapPin className="h-4 w-4" />}{coords ? 'Location added' : 'Use current location'}</button>
    {coords && <p className="rounded-xl bg-muted px-3 py-2 text-xs text-muted-foreground">{coords.latitude.toFixed(5)}, {coords.longitude.toFixed(5)}</p>}
    <label className="block text-xs font-medium text-foreground">Place name<input value={placeName} onChange={event => setPlaceName(event.target.value)} maxLength={100} placeholder="Central Park" className={inputClass} /></label>
    <label className="block text-xs font-medium text-foreground">Address <span className="font-normal text-muted-foreground">(optional)</span><input value={placeAddress} onChange={event => setPlaceAddress(event.target.value)} maxLength={200} placeholder="New York, NY" className={inputClass} /></label>
    <PanelAction onClick={submit} disabled={Boolean(!placeName.trim() && !placeAddress.trim() && !coords)} isSending={isSending}>Share location</PanelAction>
    <ErrorMessage>{error}</ErrorMessage>
  </div>;
};

export const LinkPreviewPanel: React.FC<ComposerPanelProps> = ({ isSending, onSend }) => {
  const [linkUrl, setLinkUrl] = useState('');
  const [linkTitle, setLinkTitle] = useState('');
  const [linkDescription, setLinkDescription] = useState('');
  const [linkImageUrl, setLinkImageUrl] = useState('');
  const [error, setError] = useState<string | null>(null);
  const submit = () => {
    const normalized = normalizeWebUrl(linkUrl);
    if (!normalized) { setError('Enter a valid http or https link.'); return; }
    const imageUrl = linkImageUrl.trim() ? normalizeWebUrl(linkImageUrl) : null;
    if (linkImageUrl.trim() && !imageUrl) { setError('The preview image must use a valid http or https URL.'); return; }
    const title = linkTitle.trim() || normalized.hostname;
    sendDraft({ type: 'link_preview', content: `Link · ${title}`, metadata: { type: 'link_preview', link_preview: { url: normalized.toString(), title, description: linkDescription.trim() || null, image_url: imageUrl?.toString() || null, domain: normalized.hostname } } }, onSend, setError);
  };
  return <div className="space-y-3">
    <PanelIntro>Add link details for the preview card. Synapse does not fetch third-party page metadata automatically.</PanelIntro>
    <label className="block text-xs font-medium text-foreground">Link URL<input value={linkUrl} onChange={event => setLinkUrl(event.target.value)} maxLength={2000} type="url" placeholder="https://example.com/article" className={inputClass} /></label>
    <label className="block text-xs font-medium text-foreground">Title <span className="font-normal text-muted-foreground">(optional)</span><input value={linkTitle} onChange={event => setLinkTitle(event.target.value)} maxLength={160} placeholder="Page title" className={inputClass} /></label>
    <label className="block text-xs font-medium text-foreground">Description <span className="font-normal text-muted-foreground">(optional)</span><textarea value={linkDescription} onChange={event => setLinkDescription(event.target.value)} maxLength={400} rows={2} placeholder="A short summary" className={`${textareaClass} min-h-20`} /></label>
    <label className="block text-xs font-medium text-foreground">Preview image URL <span className="font-normal text-muted-foreground">(optional)</span><input value={linkImageUrl} onChange={event => setLinkImageUrl(event.target.value)} maxLength={2000} type="url" placeholder="https://example.com/image.jpg" className={inputClass} /></label>
    <PanelAction onClick={submit} disabled={!linkUrl.trim()} isSending={isSending}>Share link</PanelAction>
    <ErrorMessage>{error}</ErrorMessage>
  </div>;
};

export const PollPanel: React.FC<ComposerPanelProps> = ({ isSending, onSend }) => {
  const [pollQuestion, setPollQuestion] = useState('');
  const [pollOptions, setPollOptions] = useState(['', '']);
  const [pollDurationHours, setPollDurationHours] = useState(24);
  const [error, setError] = useState<string | null>(null);
  const submit = () => {
    const question = pollQuestion.trim();
    const options = pollOptions.map(option => option.trim()).filter(Boolean);
    if (!question || options.length < 2) { setError('A poll needs a question and at least two options.'); return; }
    if (options.length > 4) { setError('A poll can have no more than four options.'); return; }
    if (new Set(options.map(option => option.toLowerCase())).size !== options.length) { setError('Poll options must be different.'); return; }
    const endsAt = new Date(Date.now() + pollDurationHours * 60 * 60 * 1000).toISOString();
    sendDraft({ type: 'poll', content: `Poll · ${question}`, metadata: { type: 'poll', poll: { question, options, ends_at: endsAt, allow_multiple: false } } }, onSend, setError);
  };
  return <div className="space-y-3">
    <PanelIntro>Recipients can cast one vote. Poll votes are visible to everyone in this chat.</PanelIntro>
    <label className="block text-xs font-medium text-foreground">Question<input value={pollQuestion} onChange={event => setPollQuestion(event.target.value)} maxLength={240} placeholder="Where should we go next?" className={inputClass} /></label>
    <div className="space-y-2"><span className="block text-xs font-medium text-foreground">Options</span>{pollOptions.map((option, index) => <div key={index} className="flex items-center gap-2"><input aria-label={`Poll option ${index + 1}`} value={option} onChange={event => setPollOptions(current => current.map((value, itemIndex) => itemIndex === index ? event.target.value : value))} maxLength={100} placeholder={`Option ${index + 1}`} className="h-10 min-w-0 flex-1 rounded-xl border border-border bg-background px-3 text-sm text-foreground outline-none focus:border-ring" />{pollOptions.length > 2 && <button type="button" aria-label={`Remove option ${index + 1}`} onClick={() => setPollOptions(current => current.filter((_, itemIndex) => itemIndex !== index))} className="grid h-9 w-9 shrink-0 place-items-center rounded-full text-muted-foreground hover:bg-muted"><span aria-hidden="true">×</span></button>}</div>)}</div>
    {pollOptions.length < 4 && <button type="button" onClick={() => setPollOptions(current => [...current, ''])} className="inline-flex items-center gap-1.5 rounded-full px-2 py-1 text-xs font-semibold text-foreground hover:bg-muted">+ Add option</button>}
    <label className="block text-xs font-medium text-foreground">Poll duration<select value={pollDurationHours} onChange={event => setPollDurationHours(Number(event.target.value))} className={inputClass}><option value={24}>1 day</option><option value={48}>2 days</option><option value={72}>3 days</option><option value={168}>7 days</option></select></label>
    <PanelAction onClick={submit} disabled={!pollQuestion.trim() || pollOptions.filter(option => option.trim()).length < 2} isSending={isSending}>Create poll</PanelAction>
    <ErrorMessage>{error}</ErrorMessage>
  </div>;
};

export const CodeSnippetPanel: React.FC<ComposerPanelProps> = ({ isSending, onSend }) => {
  const [codeLanguage, setCodeLanguage] = useState('Kotlin');
  const [codeFilename, setCodeFilename] = useState('');
  const [codeDescription, setCodeDescription] = useState('');
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const submit = () => {
    const source = code.trim();
    if (!source) { setError('Add the code you want to share.'); return; }
    if (source.length > 20000) { setError('Code snippets are limited to 20,000 characters.'); return; }
    const filename = codeFilename.trim();
    sendDraft({ type: 'code_snippet', content: `Code · ${filename || codeLanguage}`, metadata: { type: 'code_snippet', code_snippet: { language: codeLanguage, filename: filename || null, description: codeDescription.trim() || null, code: source } } }, onSend, setError);
  };
  return <div className="space-y-3">
    <PanelIntro>Share code as a formatted card. Avoid secrets such as passwords, tokens, or private keys.</PanelIntro>
    <label className="block text-xs font-medium text-foreground">Language<select value={codeLanguage} onChange={event => setCodeLanguage(event.target.value)} className={inputClass}>{['Kotlin', 'Java', 'JavaScript', 'TypeScript', 'Python', 'Swift', 'SQL', 'JSON', 'HTML', 'CSS', 'Bash', 'Plain text'].map(language => <option key={language}>{language}</option>)}</select></label>
    <label className="block text-xs font-medium text-foreground">File name <span className="font-normal text-muted-foreground">(optional)</span><input value={codeFilename} onChange={event => setCodeFilename(event.target.value)} maxLength={120} placeholder="greeting.kt" className={inputClass} /></label>
    <label className="block text-xs font-medium text-foreground">Description <span className="font-normal text-muted-foreground">(optional)</span><input value={codeDescription} onChange={event => setCodeDescription(event.target.value)} maxLength={200} placeholder="A short explanation" className={inputClass} /></label>
    <label className="block text-xs font-medium text-foreground">Code<textarea value={code} onChange={event => setCode(event.target.value)} maxLength={20000} rows={8} placeholder={'fun greet(name: String) = "Hello, $name!"'} className={`${textareaClass} font-mono text-xs`} /></label>
    <PanelAction onClick={submit} disabled={!code.trim()} isSending={isSending}>Share code</PanelAction>
    <ErrorMessage>{error}</ErrorMessage>
  </div>;
};
