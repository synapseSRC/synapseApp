import React, { useState } from 'react';
import { BarChart3, Check, Code2, Contact, Copy, ExternalLink, Link2, MapPin, UserPlus } from 'lucide-react';

export interface MessagePollVote {
  message_id: string;
  chat_id?: string;
  user_id: string;
  option_index: number;
}

interface ChatMessageCardsProps {
  messageId: string;
  messageType: string;
  metadata: unknown;
  pollVotes?: MessagePollVote[];
  userId: string;
  isVoting?: boolean;
  onVote?: (optionIndex: number) => void;
}

const isRecord = (value: unknown): value is Record<string, unknown> => Boolean(value && typeof value === 'object' && !Array.isArray(value));
const recordAt = (value: unknown, key: string): Record<string, unknown> => {
  if (!isRecord(value)) return {};
  return isRecord(value[key]) ? value[key] as Record<string, unknown> : value;
};
const stringAt = (value: Record<string, unknown>, key: string, fallback = '') => typeof value[key] === 'string' ? (value[key] as string).trim() : fallback;
const safeHttpUrl = (value: string) => {
  try {
    const url = new URL(value);
    return url.protocol === 'https:' || url.protocol === 'http:' ? url.toString() : null;
  } catch { return null; }
};
const cardClass = 'w-[min(76vw,25rem)] max-w-full overflow-hidden rounded-2xl border border-zinc-200 bg-white text-zinc-900 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100';

const ContactCard: React.FC<{ data: Record<string, unknown> }> = ({ data }) => {
  const contact = recordAt(data, 'contact');
  const name = stringAt(contact, 'name', 'Shared contact');
  const phone = stringAt(contact, 'phone');
  const email = stringAt(contact, 'email');
  const saveContact = () => {
    const escapeVCard = (value: string) => value.replace(/[\\,;]/g, '\\$&').replace(/\r?\n/g, '\\n');
    const vcard = ['BEGIN:VCARD', 'VERSION:3.0', `FN:${escapeVCard(name)}`, phone ? `TEL;TYPE=CELL:${escapeVCard(phone)}` : '', email ? `EMAIL:${escapeVCard(email)}` : '', 'END:VCARD'].filter(Boolean).join('\r\n');
    const url = URL.createObjectURL(new Blob([vcard], { type: 'text/vcard;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `${name.replace(/[^a-z0-9_-]+/gi, '_') || 'contact'}.vcf`;
    link.click();
    URL.revokeObjectURL(url);
  };
  return <section role="group" aria-label={`Shared contact ${name}`} className={`${cardClass} p-4`}>
    <div className="flex items-center gap-3">
      <span className="grid h-14 w-14 shrink-0 place-items-center rounded-full border border-zinc-200 bg-zinc-100 text-lg font-semibold text-zinc-700 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"><Contact className="h-6 w-6" /></span>
      <div className="min-w-0 flex-1"><p className="truncate text-base font-semibold">{name}</p><p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">Contact</p></div>
    </div>
    {phone && <a href={`tel:${phone.replace(/[^+\d]/g, '')}`} className="mt-4 block truncate text-sm font-medium text-zinc-800 underline decoration-zinc-300 underline-offset-4 dark:text-zinc-200 dark:decoration-zinc-600">{phone}</a>}
    {email && <a href={`mailto:${email}`} className="mt-1 block truncate text-xs text-zinc-500 underline decoration-zinc-300 underline-offset-4 dark:text-zinc-400 dark:decoration-zinc-600">{email}</a>}
    <button type="button" onClick={saveContact} className="mt-4 inline-flex h-10 w-full items-center justify-center gap-2 rounded-full border border-zinc-300 bg-zinc-50 text-xs font-semibold hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-800 dark:hover:bg-zinc-700"><UserPlus className="h-4 w-4" />Save contact</button>
  </section>;
};

const LocationCard: React.FC<{ data: Record<string, unknown> }> = ({ data }) => {
  const location = recordAt(data, 'location');
  const name = stringAt(location, 'name', 'Shared location');
  const address = stringAt(location, 'address');
  const latitude = typeof location.latitude === 'number' ? location.latitude : null;
  const longitude = typeof location.longitude === 'number' ? location.longitude : null;
  const query = latitude !== null && longitude !== null ? `${latitude},${longitude}` : [name, address].filter(Boolean).join(', ');
  const mapUrl = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
  return <section role="group" aria-label={`Shared location ${name}`} className={cardClass}>
    <div className="relative h-36 overflow-hidden bg-zinc-100 dark:bg-zinc-800">
      <svg aria-hidden="true" viewBox="0 0 400 150" preserveAspectRatio="xMidYMid slice" className="absolute inset-0 h-full w-full text-zinc-300 dark:text-zinc-700">
        <rect width="400" height="150" fill="currentColor" opacity=".22" />
        <path d="M-10 32 95 70 144 5M56 170 142 88 242 164M150-15 203 52 412 20M95 70l109-18 58 92 148-44M240-15l18 159M-15 115l90-45 59 18 60-36 54 45 94-18" fill="none" stroke="currentColor" strokeWidth="8" strokeLinecap="round" />
        <path d="M-10 32 95 70 144 5M56 170 142 88 242 164M150-15 203 52 412 20M95 70l109-18 58 92 148-44M240-15l18 159M-15 115l90-45 59 18 60-36 54 45 94-18" fill="none" stroke="#fff" strokeWidth="3" strokeLinecap="round" opacity=".8" />
      </svg>
      <span className="absolute left-1/2 top-1/2 grid h-11 w-11 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full border-4 border-white bg-zinc-950 text-white shadow-lg dark:border-zinc-900 dark:bg-zinc-100 dark:text-zinc-950"><MapPin className="h-5 w-5 fill-current" /></span>
      <span className="absolute bottom-2 left-2 rounded-full bg-white/95 px-2.5 py-1 text-[10px] font-semibold text-zinc-700 dark:bg-zinc-950/90 dark:text-zinc-200">Map preview</span>
    </div>
    <div className="p-4"><h4 className="truncate text-base font-semibold">{name}</h4><p className="mt-1 line-clamp-2 min-h-5 text-xs leading-5 text-zinc-500 dark:text-zinc-400">{address || (latitude !== null && longitude !== null ? `${latitude.toFixed(5)}, ${longitude.toFixed(5)}` : 'Location shared')}</p>
      <a href={mapUrl} target="_blank" rel="noopener noreferrer" className="mt-3 inline-flex h-10 w-full items-center justify-center gap-2 rounded-full border border-zinc-300 bg-zinc-50 text-xs font-semibold hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-800 dark:hover:bg-zinc-700"><ExternalLink className="h-4 w-4" />Open in maps</a>
    </div>
  </section>;
};

const LinkPreviewCard: React.FC<{ data: Record<string, unknown> }> = ({ data }) => {
  const link = recordAt(data, 'link_preview');
  const url = safeHttpUrl(stringAt(link, 'url'));
  if (!url) return <div role="note" className="rounded-xl border border-zinc-200 px-3 py-2 text-xs text-zinc-500 dark:border-zinc-700 dark:text-zinc-400">This link preview is unavailable.</div>;
  const parsed = new URL(url);
  const title = stringAt(link, 'title', parsed.hostname);
  const description = stringAt(link, 'description');
  const imageUrl = safeHttpUrl(stringAt(link, 'image_url'));
  return <a href={url} target="_blank" rel="noopener noreferrer" aria-label={`Open link: ${title}`} className={`${cardClass} block transition-colors hover:border-zinc-400 dark:hover:border-zinc-500`}>
    {imageUrl ? <img src={imageUrl} alt="" loading="lazy" onError={event => { event.currentTarget.style.display = 'none'; }} className="aspect-[16/9] w-full object-cover" /> : <div className="grid aspect-[16/9] place-items-center bg-zinc-100 text-zinc-500 dark:bg-zinc-800 dark:text-zinc-300"><Link2 className="h-8 w-8" /></div>}
    <div className="p-4"><h4 className="line-clamp-2 text-sm font-semibold leading-5">{title}</h4>{description && <p className="mt-1.5 line-clamp-3 text-xs leading-5 text-zinc-500 dark:text-zinc-400">{description}</p>}<span className="mt-3 inline-flex max-w-full items-center gap-2 text-xs text-zinc-500 dark:text-zinc-400"><Link2 className="h-3.5 w-3.5 shrink-0" /><span className="truncate">{stringAt(link, 'domain', parsed.hostname)}</span><ExternalLink className="h-3.5 w-3.5 shrink-0" /></span></div>
  </a>;
};

const PollCard: React.FC<{ messageId: string; data: Record<string, unknown>; votes: MessagePollVote[]; userId: string; isVoting: boolean; onVote?: (optionIndex: number) => void }> = ({ messageId, data, votes, userId, isVoting, onVote }) => {
  const poll = recordAt(data, 'poll');
  const question = stringAt(poll, 'question', 'Poll');
  const options = Array.isArray(poll.options) ? poll.options.filter((option): option is string => typeof option === 'string').slice(0, 4) : [];
  const myVote = votes.find(vote => vote.message_id === messageId && vote.user_id === userId)?.option_index;
  const totalVotes = votes.filter(vote => vote.message_id === messageId).length;
  const endsAt = stringAt(poll, 'ends_at');
  const isClosed = Boolean(endsAt && Number.isFinite(Date.parse(endsAt)) && Date.parse(endsAt) <= Date.now());
  return <section role="group" aria-label={`Poll: ${question}`} className={`${cardClass} p-4`}>
    <div className="flex items-start gap-2"><BarChart3 className="mt-0.5 h-4 w-4 shrink-0 text-zinc-500 dark:text-zinc-400" /><h4 className="min-w-0 flex-1 text-sm font-semibold leading-5">{question}</h4></div>
    <div className="mt-4 space-y-2">
      {options.map((option, index) => {
        const optionVotes = votes.filter(vote => vote.message_id === messageId && vote.option_index === index).length;
        const percent = totalVotes ? Math.round(optionVotes / totalVotes * 100) : 0;
        const selected = myVote === index;
        const showResults = myVote !== undefined || totalVotes > 0 || isClosed;
        return <button key={`${index}-${option}`} type="button" disabled={isClosed || myVote !== undefined || isVoting || !onVote} onClick={() => onVote?.(index)} aria-pressed={selected} className={`relative min-h-12 w-full overflow-hidden rounded-xl border px-3 py-2 text-left transition-colors disabled:cursor-default ${selected ? 'border-zinc-800 bg-zinc-100 dark:border-zinc-300 dark:bg-zinc-800' : 'border-zinc-200 bg-white hover:bg-zinc-50 dark:border-zinc-700 dark:bg-zinc-900 dark:hover:bg-zinc-800'}`}>
          {showResults && <span aria-hidden="true" className={`absolute inset-y-0 left-0 ${selected ? 'bg-zinc-300/60 dark:bg-zinc-600/60' : 'bg-zinc-100 dark:bg-zinc-800/80'}`} style={{ width: `${percent}%` }} />}
          <span className="relative flex items-center gap-2"><span className={`grid h-4 w-4 shrink-0 place-items-center rounded-full border ${selected ? 'border-zinc-900 dark:border-zinc-100' : 'border-zinc-400 dark:border-zinc-500'}`}>{selected && <span className="h-2 w-2 rounded-full bg-zinc-900 dark:bg-zinc-100" />}</span><span className="min-w-0 flex-1 truncate text-xs font-medium">{option}</span>{showResults && <span className="shrink-0 text-[10px] text-zinc-500 dark:text-zinc-400">{optionVotes} · {percent}%</span>}{isVoting && selected && <LoaderIcon />}</span>
        </button>;
      })}
    </div>
    <div className="mt-3 flex items-center justify-between gap-2 border-t border-zinc-100 pt-3 text-[10px] text-zinc-500 dark:border-zinc-800 dark:text-zinc-400"><span>{totalVotes} {totalVotes === 1 ? 'vote' : 'votes'}{myVote !== undefined ? ' · Your vote is recorded' : ''}</span><span>{isClosed ? 'Closed' : endsAt ? `Ends ${new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(new Date(endsAt))}` : 'Open'}</span></div>
  </section>;
};

const LoaderIcon: React.FC = () => <span aria-label="Saving vote" className="ml-1 h-3 w-3 animate-spin rounded-full border-2 border-zinc-400 border-t-transparent" />;

const CodeSnippetCard: React.FC<{ data: Record<string, unknown> }> = ({ data }) => {
  const [copied, setCopied] = useState(false);
  const [copyError, setCopyError] = useState(false);
  const snippet = recordAt(data, 'code_snippet');
  const source = stringAt(snippet, 'code');
  const language = stringAt(snippet, 'language', 'Plain text');
  const filename = stringAt(snippet, 'filename');
  const description = stringAt(snippet, 'description');
  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(source);
      setCopied(true);
      setCopyError(false);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setCopyError(true);
    }
  };
  return <section role="group" aria-label={`Code snippet${filename ? ` ${filename}` : ''}`} className={`${cardClass} p-3`}>
    <div className="flex items-center gap-2 px-1 pb-3"><span className="inline-flex items-center gap-1.5 rounded-full bg-zinc-100 px-2.5 py-1 text-[11px] font-medium text-zinc-700 dark:bg-zinc-800 dark:text-zinc-200"><Code2 className="h-3.5 w-3.5" />{language}</span><button type="button" onClick={() => void copyCode()} aria-label={copied ? 'Code copied' : 'Copy code'} className="ml-auto grid h-9 w-9 place-items-center rounded-xl text-zinc-500 hover:bg-zinc-100 dark:text-zinc-300 dark:hover:bg-zinc-800">{copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}</button></div>
    <pre className="max-h-64 overflow-auto rounded-xl bg-zinc-950 p-3 text-[11px] leading-5 text-zinc-100 dark:bg-black"><code>{source}</code></pre>
    {(filename || description) && <div className="px-1 pt-3"><p className="truncate text-xs font-semibold">{filename || language}</p>{description && <p className="mt-1 line-clamp-2 text-xs leading-5 text-zinc-500 dark:text-zinc-400">{description}</p>}</div>}
    {copyError && <p role="status" className="px-1 pt-2 text-[10px] text-zinc-500 dark:text-zinc-400">Clipboard access is unavailable in this browser.</p>}
  </section>;
};

export const ChatMessageCards: React.FC<ChatMessageCardsProps> = ({ messageId, messageType, metadata, pollVotes = [], userId, isVoting = false, onVote }) => {
  const type = messageType.toLowerCase();
  const data = isRecord(metadata) ? metadata : {};
  if (type === 'contact') return <ContactCard data={data} />;
  if (type === 'location') return <LocationCard data={data} />;
  if (type === 'link_preview') return <LinkPreviewCard data={data} />;
  if (type === 'poll') return <PollCard messageId={messageId} data={data} votes={pollVotes} userId={userId} isVoting={isVoting} onVote={onVote} />;
  if (type === 'code_snippet') return <CodeSnippetCard data={data} />;
  return null;
};
