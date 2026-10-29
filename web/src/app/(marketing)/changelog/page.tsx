import type { ReactNode } from "react";
import { ArrowUpRight, DownloadIcon } from "lucide-react";
import Link from "next/link";

import AnimationContainer from "@/components/global/animation-container";
import { Icons } from "@/components/global/icons";
import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import FinalCta from "@/components/marketing/final-cta";
import { PageHeader } from "@/components/marketing/page-header";
import { buttonVariants } from "@/components/ui/button";
import type { ChangeKind } from "@/lib/github/release-notes";
import { getReleaseFeed, type ChangelogRelease } from "@/lib/github/releases";
import { PROJECT_LINKS } from "@/utils/constants/site";
import { cn, generateMetadata } from "@/utils";

// The page is rendered per visit so a GitHub hiccup is never baked into a cached page; the release
// payload itself is cached by fetch for the same six hours the Android app uses.
export const dynamic = "force-dynamic";

export const metadata = generateMetadata({
    title: "Changelog — Synapse",
    description:
        "Every published Synapse release, with the notes from its GitHub tag. If it isn't tagged, it isn't here.",
});

const KIND_STYLES: Record<ChangeKind, { label: string; heading: string; className: string }> = {
    added: {
        label: "Added",
        heading: "New features",
        className: "border-cyan-400/40 bg-cyan-400/10 text-cyan-200",
    },
    improved: {
        label: "Improved",
        heading: "Improvements",
        className: "border-violet-400/40 bg-violet-400/10 text-violet-200",
    },
    fixed: {
        label: "Fixed",
        heading: "Bug fixes",
        className: "border-amber-400/40 bg-amber-400/10 text-amber-200",
    },
    changed: {
        label: "Changed",
        heading: "Changes",
        className: "border-border/70 bg-foreground/[0.04] text-muted-foreground",
    },
};

const RELEASE_DATE = new Intl.DateTimeFormat("en-US", {
    month: "long",
    day: "numeric",
    year: "numeric",
    timeZone: "UTC",
});

/** The inline subset the notes parser hands over: bold, code and http(s) links. */
const INLINE_MARKUP = /\*\*(.+?)\*\*|`([^`]*)`|\[([^\]]+)\]\(([^)]*)\)/g;

function renderInline(text: string): ReactNode {
    const nodes: ReactNode[] = [];
    const pattern = new RegExp(INLINE_MARKUP.source, "g");
    let cursor = 0;
    let match: RegExpExecArray | null;
    while ((match = pattern.exec(text)) !== null) {
        if (match.index > cursor) nodes.push(text.slice(cursor, match.index));
        const [raw, bold, code, label, href] = match;
        const key = `${match.index}-${raw}`;
        if (bold !== undefined) {
            nodes.push(
                <strong key={key} className="font-medium text-foreground">
                    {bold}
                </strong>,
            );
        } else if (code !== undefined) {
            nodes.push(
                <code
                    key={key}
                    className="rounded bg-foreground/[0.06] px-1 py-0.5 font-mono text-[0.85em] text-foreground"
                >
                    {code}
                </code>,
            );
        } else if (href !== undefined && /^https?:\/\//i.test(href)) {
            nodes.push(
                <a
                    key={key}
                    href={href}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-foreground underline decoration-border underline-offset-2 transition-colors hover:text-cyan-200"
                >
                    {label}
                </a>,
            );
        } else if (label !== undefined) {
            nodes.push(label);
        }
        cursor = match.index + raw.length;
    }
    if (cursor < text.length) nodes.push(text.slice(cursor));
    return nodes;
}

function formatReleaseDate(iso: string | null): string | null {
    return iso ? RELEASE_DATE.format(new Date(iso)) : null;
}

function GitHubButton() {
    return (
        <Link
            href={PROJECT_LINKS.releases}
            target="_blank"
            rel="noopener noreferrer"
            className={buttonVariants({ variant: "outline", className: "gap-2" })}
        >
            <Icons.github className="size-4" />
            GitHub Releases
        </Link>
    );
}

function FeedNotice({
    title,
    message,
    action,
}: {
    title: string;
    message: string;
    action?: ReactNode;
}) {
    return (
        <div className="rounded-2xl border border-border/60 bg-foreground/[0.02] px-6 py-14 text-center">
            <h2 className="font-heading text-2xl font-medium text-foreground">{title}</h2>
            <p className="mx-auto mt-3 max-w-md text-sm text-muted-foreground md:text-base">
                {message}
            </p>
            {action && <div className="mt-8 flex justify-center">{action}</div>}
        </div>
    );
}

function ReleaseEntry({ release, isLatest }: { release: ChangelogRelease; isLatest: boolean }) {
    const date = formatReleaseDate(release.publishedAt);

    return (
        <article className="relative border-l border-border/70 pb-14 pl-6 md:pl-8">
            <span className="absolute -left-[7px] top-2 size-3.5 rounded-full border-2 border-background bg-gradient-to-br from-violet-500 to-cyan-400" />
            <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <h2 className="font-heading text-2xl font-medium text-foreground">
                    {release.tag}
                </h2>
                {date && <span className="text-sm text-muted-foreground">{date}</span>}
                {isLatest && (
                    <span className="rounded-full border border-cyan-400/40 bg-cyan-400/10 px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide text-cyan-200">
                        Latest
                    </span>
                )}
            </div>
            {release.title !== release.tag && (
                <p className="mt-3 text-sm text-muted-foreground md:text-base">{release.title}</p>
            )}

            {release.groups.length > 0 && (
                <div className="mt-6 flex flex-col gap-6">
                    {release.groups.map((group, groupIndex) => {
                        const kind = KIND_STYLES[group.kind];
                        return (
                            <section key={`${group.kind}-${groupIndex}`}>
                                <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                                    <span
                                        className={cn(
                                            "w-fit rounded-full border px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide",
                                            kind.className,
                                        )}
                                    >
                                        {kind.label}
                                    </span>
                                    <h3 className="text-sm font-medium text-foreground">
                                        {group.title || kind.heading}
                                    </h3>
                                </div>
                                <ul className="mt-3 flex flex-col gap-2">
                                    {group.items.map((item, itemIndex) => (
                                        <li
                                            key={`${itemIndex}-${item}`}
                                            className="flex gap-3 rounded-lg border border-border/50 bg-foreground/[0.02] p-3"
                                        >
                                            <span
                                                aria-hidden="true"
                                                className="mt-[7px] size-1.5 shrink-0 rounded-full bg-muted-foreground/50"
                                            />
                                            <span className="text-sm leading-relaxed text-muted-foreground">
                                                {renderInline(item)}
                                            </span>
                                        </li>
                                    ))}
                                </ul>
                            </section>
                        );
                    })}
                </div>
            )}

            <Link
                href={release.url}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-6 inline-flex items-center gap-1.5 text-sm font-medium text-foreground/70 transition-colors hover:text-foreground"
            >
                View on GitHub
                <ArrowUpRight className="size-4" />
            </Link>
        </article>
    );
}

const ChangeLogPage = async () => {
    const feed = await getReleaseFeed();

    return (
        <div className="overflow-x-hidden">
            <MaxWidthWrapper>
                <PageHeader
                    badge="Changelog"
                    title="What shipped, and when"
                    description="Every published Synapse release, with its notes straight from GitHub. If it isn't tagged, it isn't here."
                    actions={
                        <>
                            <Link
                                href="/download"
                                className={buttonVariants({ variant: "accent", className: "gap-2" })}
                            >
                                <DownloadIcon className="size-4" />
                                Download Synapse
                            </Link>
                            <GitHubButton />
                        </>
                    }
                />

                <div className="mx-auto max-w-3xl pb-16">
                    {feed.status === "ok" ? (
                        feed.releases.length === 0 ? (
                            <FeedNotice
                                title="No releases yet"
                                message="Published releases will appear here as soon as they reach GitHub."
                                action={<GitHubButton />}
                            />
                        ) : (
                            feed.releases.map((release, index) => (
                                <AnimationContainer delay={0.12 * index} key={release.tag}>
                                    <ReleaseEntry release={release} isLatest={index === 0} />
                                </AnimationContainer>
                            ))
                        )
                    ) : (
                        <FeedNotice
                            title="Couldn't load releases"
                            message={
                                feed.rateLimited
                                    ? "GitHub is limiting requests right now. Try again in a little while."
                                    : "The release list could not be read right now. Check the repository directly instead."
                            }
                            action={<GitHubButton />}
                        />
                    )}
                </div>
            </MaxWidthWrapper>

            <FinalCta
                title="Try what just shipped"
                description="Update Synapse and see the difference — every note above comes from a real, tagged release."
            />
        </div>
    );
};

export default ChangeLogPage;
