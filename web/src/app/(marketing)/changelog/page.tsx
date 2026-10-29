import { Icons } from "@/components/global/icons";
import AnimationContainer from "@/components/global/animation-container";
import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import FinalCta from "@/components/marketing/final-cta";
import { PageHeader } from "@/components/marketing/page-header";
import { buttonVariants } from "@/components/ui/button";
import { ChangeKind, RELEASES } from "@/utils/constants/changelog";
import { PROJECT_LINKS } from "@/utils/constants/site";
import { cn, generateMetadata } from "@/utils";
import { DownloadIcon } from "lucide-react";
import Link from "next/link";

export const metadata = generateMetadata({
    title: "Changelog — Synapse",
    description:
        "Release notes for Synapse: protocol, server, clients and CLI. Every change is published here as it ships.",
});

const KIND_STYLES: Record<ChangeKind, { label: string; className: string }> = {
    added: { label: "Added", className: "border-cyan-400/40 bg-cyan-400/10 text-cyan-200" },
    improved: { label: "Improved", className: "border-violet-400/40 bg-violet-400/10 text-violet-200" },
    fixed: { label: "Fixed", className: "border-amber-400/40 bg-amber-400/10 text-amber-200" },
};

const ChangeLogPage = () => {
    return (
        <div className="overflow-x-hidden">
            <MaxWidthWrapper>
                <PageHeader
                    badge="Changelog"
                    title="What shipped, and when"
                    description="One release train for the protocol, the node, the CLI and all five clients. Nothing lands here that is not in the tagged source."
                    actions={
                        <>
                            <Link
                                href="/download"
                                className={buttonVariants({ variant: "accent", className: "gap-2" })}
                            >
                                <DownloadIcon className="size-4" />
                                Download Synapse
                            </Link>
                            <Link
                                href={PROJECT_LINKS.releases}
                                className={buttonVariants({ variant: "outline", className: "gap-2" })}
                            >
                                <Icons.github className="size-4" />
                                Release artifacts
                            </Link>
                        </>
                    }
                />

                <div className="mx-auto max-w-3xl pb-16">
                    {RELEASES.map((release, id) => (
                        <AnimationContainer delay={0.12 * id} key={release.version}>
                            <article className="relative border-l border-border/70 pb-14 pl-6 md:pl-8">
                                <span className="absolute -left-[7px] top-2 size-3.5 rounded-full border-2 border-background bg-gradient-to-br from-violet-500 to-cyan-400" />
                                <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                                    <h2 className="text-2xl font-medium text-foreground font-heading">
                                        {release.version}
                                    </h2>
                                    <span className="text-sm text-muted-foreground">{release.date}</span>
                                    {id === 0 && (
                                        <span className="rounded-full border border-cyan-400/40 bg-cyan-400/10 px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide text-cyan-200">
                                            Latest
                                        </span>
                                    )}
                                </div>
                                <p className="mt-3 text-sm text-muted-foreground md:text-base">
                                    {release.summary}
                                </p>
                                <ul className="mt-5 flex flex-col gap-3">
                                    {release.changes.map((change) => {
                                        const kind = KIND_STYLES[change.kind];
                                        return (
                                            <li
                                                key={change.description}
                                                className="flex flex-col gap-1.5 rounded-lg border border-border/50 bg-foreground/[0.02] p-3 sm:flex-row sm:items-start sm:gap-3"
                                            >
                                                <span
                                                    className={cn(
                                                        "w-fit shrink-0 rounded-full border px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide",
                                                        kind.className,
                                                    )}
                                                >
                                                    {kind.label}
                                                </span>
                                                <span className="text-sm text-muted-foreground">
                                                    {change.description}
                                                </span>
                                            </li>
                                        );
                                    })}
                                </ul>
                            </article>
                        </AnimationContainer>
                    ))}
                </div>
            </MaxWidthWrapper>

            <FinalCta
                title="Try what just shipped"
                description="Update a client, or pull the latest image for your node — the release notes above are the diff you get."
            />
        </div>
    );
};

export default ChangeLogPage;
