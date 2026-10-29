import { cn } from "@/utils";
import {
    HashIcon,
    InboxIcon,
    MessagesSquareIcon,
    PlusIcon,
    SearchIcon,
    ShieldCheckIcon,
    SparklesIcon,
} from "lucide-react";

const SPACES = [
    { name: "design", members: "128", active: true },
    { name: "engineering", members: "342" },
    { name: "release-log", members: "86" },
    { name: "random", members: "410" },
];

const CONVERSATION = [
    {
        author: "Marta",
        initials: "MA",
        time: "09:41",
        body: "Pushed the new space icons. They still read clearly at 16px on a phone.",
        reactions: [{ emoji: "🔥", count: 4 }],
    },
    {
        author: "Ada",
        initials: "AD",
        time: "09:44",
        body: "Watching the channel from the terminal now — no browser involved.",
        code: "$ synapse watch --space design",
        reactions: [{ emoji: "⚡", count: 2 }],
    },
    {
        author: "Tom",
        initials: "TO",
        time: "09:52",
        body: "Draft written offline on the train merged cleanly when I reconnected.",
        reactions: [{ emoji: "👍", count: 6 }],
    },
];

const DIRECTS = [
    { name: "Nadia", initials: "NA", note: "keys are on the node now" },
    { name: "Kwame", initials: "KW", note: "sending the patch after lunch" },
    { name: "release-bot", initials: "RB", note: "build #2183 live", muted: true },
];

const DEVICES = [
    { name: "Pixel 8", state: "synced 2s ago" },
    { name: "MacBook Air", state: "synced 1m ago" },
    { name: "archlinux", state: "synced 4m ago" },
];

/** Static mock of the Synapse client, built in markup so it stays crisp at any size. */
const AppPreview = ({ className }: { className?: string }) => {
    return (
        <div
            role="img"
            aria-label="Synapse client showing the design space: a channel list, a message thread with a CLI command and reactions, direct messages, and three synced devices."
            className={cn(
                "relative overflow-hidden rounded-xl border border-border bg-neutral-950 text-left",
                className,
            )}
        >
            <div className="flex items-center gap-3 border-b border-border/70 bg-foreground/[0.02] px-4 py-2.5">
                <div className="flex items-center gap-1.5">
                    <span className="size-2.5 rounded-full bg-neutral-700" />
                    <span className="size-2.5 rounded-full bg-neutral-700" />
                    <span className="size-2.5 rounded-full bg-neutral-700" />
                </div>
                <span className="truncate text-xs font-medium text-muted-foreground">
                    synapse.app
                </span>
                <span className="ml-auto flex items-center gap-1.5 text-[11px] text-cyan-300/80">
                    <span className="size-1.5 rounded-full bg-cyan-400" />
                    <span className="hidden sm:inline">Synced across 3 devices</span>
                </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-[160px_minmax(0,1fr)] lg:grid-cols-[160px_minmax(0,1fr)_210px]">
                {/* Left rail */}
                <aside className="hidden flex-col gap-5 border-r border-border/60 p-3 md:flex">
                    <div className="flex flex-col gap-1 text-sm">
                        <span className="flex items-center gap-2 rounded-md bg-foreground/[0.06] px-2 py-1.5 text-foreground">
                            <InboxIcon className="size-3.5" /> Inbox
                        </span>
                        <span className="flex items-center gap-2 rounded-md px-2 py-1.5 text-muted-foreground">
                            <MessagesSquareIcon className="size-3.5" /> Threads
                        </span>
                        <span className="flex items-center gap-2 rounded-md px-2 py-1.5 text-muted-foreground">
                            <SearchIcon className="size-3.5" /> Search
                        </span>
                    </div>
                    <div className="flex flex-col gap-1">
                        <span className="px-2 text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                            Spaces
                        </span>
                        {SPACES.map((space) => (
                            <span
                                key={space.name}
                                className={cn(
                                    "flex items-center gap-2 rounded-md px-2 py-1.5 text-sm",
                                    space.active
                                        ? "bg-violet-500/15 text-foreground"
                                        : "text-muted-foreground",
                                )}
                            >
                                <HashIcon className="size-3.5" />
                                <span className="truncate">{space.name}</span>
                            </span>
                        ))}
                        <span className="mt-1 flex items-center gap-2 px-2 py-1.5 text-sm text-muted-foreground">
                            <PlusIcon className="size-3.5" /> New space
                        </span>
                    </div>
                    <span className="mt-auto flex items-center gap-2 text-[11px] text-muted-foreground">
                        <ShieldCheckIcon className="size-3.5 text-cyan-300" />
                        Encryption on
                    </span>
                </aside>

                {/* Center feed */}
                <main className="flex min-w-0 flex-col gap-4 p-4">
                    <div className="flex items-center justify-between gap-3 border-b border-border/50 pb-3">
                        <span className="flex min-w-0 items-center gap-2 text-sm font-medium text-foreground">
                            <HashIcon className="size-4 text-violet-300" />
                            <span className="truncate">design</span>
                        </span>
                        <span className="shrink-0 text-[11px] text-muted-foreground">128 members</span>
                    </div>

                    {CONVERSATION.map((message) => (
                        <article key={message.author} className="flex min-w-0 gap-3">
                            <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-violet-500/30 to-cyan-400/30 text-[10px] font-semibold text-foreground">
                                {message.initials}
                            </span>
                            <div className="min-w-0">
                                <div className="flex items-baseline gap-2">
                                    <span className="text-sm font-medium text-foreground">{message.author}</span>
                                    <span className="text-[11px] text-muted-foreground">{message.time}</span>
                                </div>
                                <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
                                    {message.body}
                                </p>
                                {message.code && (
                                    <code className="mt-2 block truncate rounded-md border border-border/60 bg-black/60 px-2.5 py-1.5 font-mono text-[11px] text-cyan-300">
                                        {message.code}
                                    </code>
                                )}
                                {!!message.reactions?.length && (
                                    <div className="mt-2 flex flex-wrap gap-1.5">
                                        {message.reactions.map((reaction) => (
                                            <span
                                                key={reaction.emoji}
                                                className="flex items-center gap-1 rounded-full border border-border/60 px-2 py-0.5 text-[11px] text-muted-foreground"
                                            >
                                                <span aria-hidden>{reaction.emoji}</span>
                                                {reaction.count}
                                            </span>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </article>
                    ))}

                    <div className="mt-auto flex items-center gap-2 rounded-lg border border-border/60 bg-foreground/[0.02] px-3 py-2">
                        <SparklesIcon className="size-3.5 shrink-0 text-violet-300" />
                        <span className="truncate text-xs text-muted-foreground">
                            Message #design — replies merge across devices
                        </span>
                    </div>
                </main>

                {/* Right rail */}
                <aside className="hidden flex-col gap-5 border-l border-border/60 p-3 lg:flex">
                    <div className="flex flex-col gap-1.5">
                        <span className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                            Direct
                        </span>
                        {DIRECTS.map((dm) => (
                            <div key={dm.name} className="flex items-center gap-2">
                                <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-foreground/[0.08] text-[9px] font-semibold text-foreground">
                                    {dm.initials}
                                </span>
                                <div className="min-w-0">
                                    <p className="truncate text-xs text-foreground">{dm.name}</p>
                                    <p className={cn("truncate text-[10px]", dm.muted ? "text-cyan-300/70" : "text-muted-foreground")}>
                                        {dm.note}
                                    </p>
                                </div>
                            </div>
                        ))}
                    </div>
                    <div className="mt-auto flex flex-col gap-2 border-t border-border/50 pt-3">
                        <span className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                            Devices
                        </span>
                        {DEVICES.map((device) => (
                            <div key={device.name} className="flex items-center justify-between gap-2">
                                <span className="truncate text-xs text-foreground/80">{device.name}</span>
                                <span className="shrink-0 text-[10px] text-muted-foreground">{device.state}</span>
                            </div>
                        ))}
                    </div>
                </aside>
            </div>
        </div>
    );
};

export default AppPreview;
