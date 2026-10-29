import { cn } from "@/utils";
import {
    RefreshCwIcon,
    CheckIcon,
    HashIcon,
    LockIcon,
    ShieldCheckIcon,
    SmartphoneIcon,
    TerminalIcon,
} from "lucide-react";

const shell =
    "absolute border border-border/70 bg-neutral-950/90 shadow-[0_20px_60px_-30px_rgba(0,0,0,0.9)] transition-all duration-300 ease-out group-hover:scale-105";

/** Open API + CLI card visual. */
export const ApiVisual = ({ className }: { className?: string }) => (
    <div className={cn(shell, "right-4 top-6 w-[85%] rounded-lg p-3 md:right-8 md:w-[70%]", className)}>
        <div className="flex items-center gap-1.5 border-b border-border/60 pb-2">
            <TerminalIcon className="size-3.5 text-cyan-300" />
            <span className="text-[11px] text-muted-foreground">synapse cli</span>
        </div>
        <div className="mt-2 space-y-1 font-mono text-[11px] leading-relaxed">
            <p className="truncate text-muted-foreground">
                <span className="text-cyan-400">$ </span>synapse send --space engineering
            </p>
            <p className="truncate text-foreground">Deploy 2183 is live on the node.</p>
            <p className="truncate text-muted-foreground">
                <span className="text-violet-400">✓ </span>delivered to 3 devices · 41ms
            </p>
        </div>
    </div>
);

/** Encrypted messaging card visual. */
export const MessagingVisual = ({ className }: { className?: string }) => (
    <div className={cn(shell, "right-6 top-6 w-[80%] rounded-lg p-3 md:right-10 md:w-[62%]", className)}>
        <div className="flex items-center gap-1.5 border-b border-border/60 pb-2 text-[11px] text-muted-foreground">
            <LockIcon className="size-3.5 text-cyan-300" />
            End-to-end encrypted
            <span className="ml-auto text-[10px] text-muted-foreground/70">@nadia</span>
        </div>
        <div className="mt-3 space-y-2">
            <p className="ml-auto w-fit max-w-[85%] rounded-lg rounded-br-sm bg-violet-500/25 px-2.5 py-1.5 text-[11px] text-foreground">
                Are the keys stored on device?
            </p>
            <p className="w-fit max-w-[85%] rounded-lg rounded-bl-sm bg-foreground/[0.07] px-2.5 py-1.5 text-[11px] text-foreground">
                Yes — sealed in the local store, backup is optional.
            </p>
            <p className="w-fit max-w-[85%] rounded-lg rounded-bl-sm bg-foreground/[0.07] px-2.5 py-1.5 text-[11px] text-foreground">
                Server only ever sees ciphertext.
            </p>
        </div>
    </div>
);

/** Cross-device sync card visual. */
export const SyncVisual = ({ className }: { className?: string }) => (
    <div className={cn(shell, "right-2 top-6 w-[88%] rounded-lg p-3", className)}>
        <div className="flex items-center justify-between border-b border-border/60 pb-2 text-[11px] text-muted-foreground">
            <span className="flex items-center gap-1.5">
                <RefreshCwIcon className="size-3.5 text-cyan-300" /> Sync
            </span>
            <span className="text-cyan-300/80">2s ago</span>
        </div>
        <div className="mt-3 space-y-2">
            {[
                { icon: SmartphoneIcon, name: "Pixel 8", note: "draft composed" },
                { icon: TerminalIcon, name: "archlinux", note: "merged cleanly" },
            ].map((row) => (
                <div key={row.name} className="flex items-center gap-2 text-[11px]">
                    <row.icon className="size-3.5 shrink-0 text-muted-foreground" />
                    <span className="truncate text-foreground/80">{row.name}</span>
                    <span className="ml-auto flex shrink-0 items-center gap-1 text-muted-foreground">
                        <CheckIcon className="size-3 text-cyan-300" />
                        {row.note}
                    </span>
                </div>
            ))}
        </div>
    </div>
);

/** Communities and spaces card visual. */
export const CommunitiesVisual = ({ className }: { className?: string }) => (
    <div className={cn(shell, "right-4 top-6 w-[85%] rounded-lg p-3 md:w-[68%]", className)}>
        <div className="space-y-1.5">
            {[
                { name: "engineering", meta: "342 members", role: "Admin" },
                { name: "release-log", meta: "86 members", role: "Mod" },
                { name: "design", meta: "128 members", role: "Member" },
            ].map((space) => (
                <div key={space.name} className="flex items-center gap-2 rounded-md px-2 py-1.5 text-[11px] hover:bg-foreground/[0.05]">
                    <HashIcon className="size-3.5 shrink-0 text-violet-300" />
                    <span className="truncate text-foreground/85">{space.name}</span>
                    <span className="ml-auto shrink-0 text-muted-foreground">{space.meta}</span>
                    <span className="shrink-0 rounded border border-border/70 px-1.5 py-0.5 text-[10px] text-muted-foreground">
                        {space.role}
                    </span>
                </div>
            ))}
        </div>
        <div className="mt-3 flex items-center gap-1.5 border-t border-border/60 pt-2 text-[10px] text-muted-foreground">
            <ShieldCheckIcon className="size-3 text-cyan-300" />
            Audit log keeps every moderation action
        </div>
    </div>
);
