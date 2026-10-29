import { cn } from "@/utils";
import {
    ArrowDownIcon,
    RefreshCwIcon,
    CheckIcon,
    HashIcon,
    KeyRoundIcon,
    LockIcon,
    ScrollTextIcon,
    ShieldCheckIcon,
    SmartphoneIcon,
    TerminalIcon,
    UsersIcon,
    WebhookIcon,
    WrenchIcon,
} from "lucide-react";

const panel = "rounded-xl border border-border/70 bg-neutral-950/70";

const Row = ({ children, className }: { children: React.ReactNode; className?: string }) => (
    <div className={cn("flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm", className)}>
        {children}
    </div>
);

export const MessagingPreview = () => (
    <div className={cn(panel, "p-4 md:p-6")}>
        <div className="flex items-center gap-2 border-b border-border/60 pb-3 text-xs text-muted-foreground">
            <LockIcon className="size-3.5 text-cyan-300" />
            End-to-end encrypted session
            <span className="ml-auto flex items-center gap-1.5 text-[11px] text-cyan-300/80">
                <KeyRoundIcon className="size-3.5" /> keys on device
            </span>
        </div>
        <div className="mt-4 grid gap-4 sm:grid-cols-[170px_minmax(0,1fr)]">
            <div className="flex flex-col gap-1 text-sm">
                {[
                    { name: "Nadia", active: true, hint: "typing…" },
                    { name: "Kwame", hint: "2 unread" },
                    { name: "Ines", hint: "read" },
                ].map((dm) => (
                    <Row
                        key={dm.name}
                        className={dm.active ? "bg-foreground/[0.07] text-foreground" : "text-muted-foreground"}
                    >
                        <span className="flex size-6 items-center justify-center rounded-full bg-gradient-to-br from-violet-500/30 to-cyan-400/30 text-[10px] font-semibold text-foreground">
                            {dm.name.slice(0, 2).toUpperCase()}
                        </span>
                        <span className="truncate">{dm.name}</span>
                        <span className="ml-auto shrink-0 text-[10px] text-muted-foreground">{dm.hint}</span>
                    </Row>
                ))}
            </div>
            <div className="flex min-w-0 flex-col gap-2.5">
                <p className="w-fit max-w-[90%] rounded-lg rounded-bl-sm bg-foreground/[0.07] px-3 py-2 text-sm text-foreground">
                    Can you check the node logs? The webhook retried twice.
                </p>
                <p className="ml-auto w-fit max-w-[90%] rounded-lg rounded-br-sm bg-violet-500/25 px-3 py-2 text-sm text-foreground">
                    Looking now — it was a clock skew on our side, fixed in 1.0.1.
                </p>
                <div className="ml-auto flex items-center gap-2 text-[11px] text-muted-foreground">
                    <CheckIcon className="size-3.5 text-cyan-300" /> delivered to 3 devices
                </div>
                <div className="mt-1 flex flex-wrap gap-1.5">
                    {["👍 3", "🔧 1"].map((reaction) => (
                        <span key={reaction} className="rounded-full border border-border/60 px-2 py-0.5 text-[11px] text-muted-foreground">
                            {reaction}
                        </span>
                    ))}
                </div>
            </div>
        </div>
    </div>
);

export const CommunitiesPreview = () => (
    <div className={cn(panel, "p-4 md:p-6")}>
        <div className="flex items-center gap-2 border-b border-border/60 pb-3 text-xs text-muted-foreground">
            <UsersIcon className="size-3.5 text-violet-300" />
            hackerspace-belgium
            <span className="ml-auto text-[11px]">573 members · 24 online</span>
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
            <div className="flex flex-col gap-1">
                {[
                    { name: "announcements", meta: "read-only", locked: true },
                    { name: "projects", meta: "81 members" },
                    { name: "machine-shop", meta: "44 members" },
                    { name: "moderators", meta: "6 members", locked: true },
                ].map((channel) => (
                    <Row key={channel.name} className="text-muted-foreground">
                        <HashIcon className="size-4 shrink-0 text-violet-300" />
                        <span className="truncate text-sm text-foreground/85">{channel.name}</span>
                        <span className="ml-auto shrink-0 text-[11px]">{channel.meta}</span>
                        {channel.locked && <LockIcon className="size-3.5 shrink-0 text-muted-foreground" />}
                    </Row>
                ))}
            </div>
            <div className="flex flex-col gap-3">
                <div className="rounded-lg border border-border/60 p-3">
                    <p className="flex items-center gap-2 text-xs text-muted-foreground">
                        <ShieldCheckIcon className="size-3.5 text-cyan-300" /> Audit log
                    </p>
                    <ul className="mt-2 space-y-1.5 text-[11px] text-muted-foreground">
                        <li className="truncate">Ines changed role for @kwame → Moderator</li>
                        <li className="truncate">Marta pinned an announcement</li>
                        <li className="truncate">Auto-moderation held 1 report for review</li>
                    </ul>
                </div>
                <div className="flex flex-wrap gap-2 text-[11px]">
                    {["Admin", "Moderator", "Member", "Guest"].map((role, id) => (
                        <span
                            key={role}
                            className={cn(
                                "rounded-full border px-2.5 py-1",
                                id === 0 ? "border-violet-400/40 text-violet-200" : "border-border/60 text-muted-foreground",
                            )}
                        >
                            {role}
                        </span>
                    ))}
                </div>
            </div>
        </div>
    </div>
);

export const SyncPreview = () => (
    <div className={cn(panel, "p-4 md:p-6")}>
        <div className="flex items-center gap-2 border-b border-border/60 pb-3 text-xs text-muted-foreground">
            <RefreshCwIcon className="size-3.5 text-cyan-300" />
            Offline-first store
            <span className="ml-auto text-[11px] text-cyan-300/80">merged 2s ago</span>
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
            {[
                { icon: SmartphoneIcon, device: "Pixel 8", state: "Draft written offline", pending: true },
                { icon: TerminalIcon, device: "archlinux", state: "Replies read while away", pending: false },
                { icon: ArrowDownIcon, device: "Windows", state: "History fetched on demand", pending: false },
            ].map((row) => (
                <div key={row.device} className="flex flex-col gap-2 rounded-lg border border-border/60 p-3">
                    <span className="flex items-center gap-2 text-xs text-muted-foreground">
                        <row.icon className="size-4 text-foreground/70" /> {row.device}
                    </span>
                    <span className="text-sm text-foreground/85">{row.state}</span>
                    <span
                        className={cn(
                            "mt-auto flex items-center gap-1.5 text-[11px]",
                            row.pending ? "text-amber-300" : "text-cyan-300",
                        )}
                    >
                        {row.pending ? <RefreshCwIcon className="size-3.5 animate-spin" /> : <CheckIcon className="size-3.5" />}
                        {row.pending ? "queued" : "settled"}
                    </span>
                </div>
            ))}
        </div>
        <p className="mt-4 flex items-center gap-2 text-[11px] text-muted-foreground">
            <ScrollTextIcon className="size-3.5" /> Causal order decides the merge — a reply never lands above its message.
        </p>
    </div>
);

export const ApiPreview = () => (
    <div className={cn(panel, "p-4 md:p-6")}>
        <div className="flex items-center gap-2 border-b border-border/60 pb-3 text-xs text-muted-foreground">
            <WebhookIcon className="size-3.5 text-cyan-300" /> api.synapse.app/v1
            <span className="ml-auto flex items-center gap-1.5 text-[11px]">
                <WrenchIcon className="size-3.5" /> OpenAPI published per release
            </span>
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
            <div className="space-y-2 font-mono text-[11px]">
                {[
                    { verb: "POST", path: "/messages" },
                    { verb: "GET", path: "/spaces/:id/thread" },
                    { verb: "POST", path: "/webhooks" },
                ].map((endpoint) => (
                    <div key={endpoint.path} className="flex items-center gap-2 rounded-md border border-border/60 px-2.5 py-1.5">
                        <span
                            className={cn(
                                "rounded px-1.5 py-0.5 text-[10px] font-semibold",
                                endpoint.verb === "POST" ? "bg-violet-500/20 text-violet-200" : "bg-cyan-500/15 text-cyan-200",
                            )}
                        >
                            {endpoint.verb}
                        </span>
                        <span className="truncate text-foreground/80">{endpoint.path}</span>
                    </div>
                ))}
            </div>
            <div className="overflow-hidden rounded-lg border border-border/60 bg-black/60 p-3">
                <p className="mb-2 flex items-center gap-1.5 text-[11px] text-muted-foreground">
                    <ShieldCheckIcon className="size-3.5 text-cyan-300" /> signed webhook
                </p>
                <pre className="overflow-x-auto text-[11px] leading-relaxed text-foreground/85">
{`{
  "type": "message.created",
  "space": "engineering",
  "signature": "v1=9f2c…"
}`}
                </pre>
            </div>
        </div>
    </div>
);
