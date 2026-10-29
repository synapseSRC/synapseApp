import { buttonVariants } from "@/components/ui/button";
import CodeBlock, { CodeLine } from "@/components/marketing/code-block";
import { PlatformGlyph } from "@/components/marketing/platform-icons";
import { Platform } from "@/utils/constants/platforms";
import { cn } from "@/utils";
import { ArrowUpRightIcon, DownloadIcon } from "lucide-react";
import Link from "next/link";

type PlatformCardProps = {
    platform: Platform;
    size?: "compact" | "full";
    command?: CodeLine[];
    className?: string;
};

const PlatformCard = ({ platform, size = "full", command, className }: PlatformCardProps) => {
    const isCompact = size === "compact";

    return (
        <div
            className={cn(
                "flex flex-col justify-between gap-5 rounded-xl border border-border/60 bg-foreground/[0.02] p-5 transition-colors duration-300 hover:border-border hover:bg-foreground/[0.04] md:p-6",
                className,
            )}
        >
            <div className="flex items-start gap-4">
                <PlatformGlyph id={platform.id} />
                <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-x-2">
                        <h3 className="text-lg font-medium text-foreground">{platform.name}</h3>
                        <span className="rounded-md border border-border/70 px-1.5 py-0.5 text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
                            {platform.category}
                        </span>
                    </div>
                    <p className={cn("mt-2 text-sm text-muted-foreground", isCompact && "line-clamp-2")}>
                        {platform.blurb}
                    </p>
                </div>
            </div>

            <div className="flex flex-col gap-4">
                {!isCompact && platform.alternates.length > 0 && (
                    <ul className="flex flex-col gap-1.5 border-t border-border/50 pt-4 text-sm">
                        {platform.alternates.map((channel) => (
                            <li key={channel.label} className="flex items-center justify-between gap-4">
                                <span className="text-muted-foreground">
                                    {channel.label}
                                    {channel.detail && (
                                        <span className="ml-2 text-xs text-muted-foreground/70">
                                            · {channel.detail}
                                        </span>
                                    )}
                                </span>
                                <Link
                                    href={channel.href}
                                    className="flex items-center text-foreground/80 transition-colors hover:text-cyan-300"
                                >
                                    <span className="sr-only">
                                        {channel.label} download for {platform.name}
                                    </span>
                                    <ArrowUpRightIcon className="size-4" />
                                </Link>
                            </li>
                        ))}
                    </ul>
                )}

                {!isCompact && command && <CodeBlock title={`${platform.name.toLowerCase()} install`} lines={command} />}

                <div className="flex flex-wrap items-center justify-between gap-3">
                    <Link
                        href={platform.primary.href}
                        className={buttonVariants({
                            variant: isCompact ? "outline" : "accent",
                            size: "sm",
                            className: "gap-2",
                        })}
                    >
                        <DownloadIcon className="size-4" />
                        {platform.primary.label}
                    </Link>
                    <span className="text-xs text-muted-foreground">{platform.requirement}</span>
                </div>
            </div>
        </div>
    );
};

export default PlatformCard;
