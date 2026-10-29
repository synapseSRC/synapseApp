import { Icons } from "@/components/global/icons";
import AnimationContainer from "@/components/global/animation-container";
import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import CodeBlock from "@/components/marketing/code-block";
import FinalCta from "@/components/marketing/final-cta";
import { PageHeader, SectionHeader } from "@/components/marketing/page-header";
import { buttonVariants } from "@/components/ui/button";
import MagicCard from "@/components/ui/magic-card";
import { generateMetadata } from "@/utils";
import { PROJECT_LINKS, SUPPORT_EMAIL } from "@/utils/constants/site";
import { SELF_HOST_COMMANDS } from "@/utils/constants/platforms";
import {
    DownloadIcon,
    FileTextIcon,
    GitBranchIcon,
    GlobeIcon,
    HeartHandshakeIcon,
    LockIcon,
    PuzzleIcon,
    ScaleIcon,
} from "lucide-react";
import Link from "next/link";

export const metadata = generateMetadata({
    title: "Open source and self-hosting — Synapse",
    description:
        "Synapse is MIT licensed: clients, server, protocol and SDKs in the open. Run your own node with docker compose, or build on the same API the official clients use.",
});

const STATS = [
    { value: "MIT", label: "licensed, every repository" },
    { value: "5", label: "first-party clients" },
    { value: "1", label: "protocol they all speak" },
    { value: "0", label: "ads, trackers or seat limits" },
];

const PRINCIPLES = [
    {
        title: "The whole stack, not a client",
        description:
            "Clients, server, federation handshake and the three SDKs live under the same license. Nothing you depend on is a closed binary.",
        icon: ScaleIcon,
    },
    {
        title: "Readable end to end",
        description:
            "A message can be traced from the keystroke in a client, through the wire format, to the row on the node — which is how bug reports get solved with a version number instead of a guess.",
        icon: PuzzleIcon,
    },
    {
        title: "Yours to run",
        description:
            "One container, one Postgres database, one TLS certificate. A node is a deployment, not a product you have to buy.",
        icon: GlobeIcon,
    },
    {
        title: "Encryption is part of the code",
        description:
            "End-to-end encrypted DMs are the default path in the protocol, not an option a hosted operator can quietly turn off.",
        icon: LockIcon,
    },
];

const CONTRIB = [
    {
        title: "Start with an issue",
        description:
            "Reproducible bugs get a trace and a version. Good-first-issues are labelled on every repository, including the SDKs.",
        icon: GitBranchIcon,
        label: "Browse issues",
        href: PROJECT_LINKS.issues,
    },
    {
        title: "Protocol changes go through an RFC",
        description:
            "Anything that touches the wire format needs a short written proposal and a review window, so a change cannot silently break an old client.",
        icon: FileTextIcon,
        label: "Read the discussions",
        href: PROJECT_LINKS.discussions,
    },
    {
        title: "Self-host and tell us where it hurt",
        description:
            "Deployments on odd hardware are how the node gets better. Setup friction is treated as a bug and fixed in the next release.",
        icon: HeartHandshakeIcon,
        label: `Email ${SUPPORT_EMAIL}`,
        href: `mailto:${SUPPORT_EMAIL}`,
    },
];

const OpenSourcePage = () => {
    return (
        <div className="overflow-x-hidden">
            <MaxWidthWrapper>
                <PageHeader
                    badge="Open source"
                    title="Social software should be yours"
                    description="Synapse is MIT licensed from the client through the protocol to the node. Read it, build on it, run it, or fork it — none of those require asking us first."
                    actions={
                        <>
                            <Link
                                href={PROJECT_LINKS.github}
                                className={buttonVariants({ variant: "accent", className: "gap-2" })}
                            >
                                <Icons.github className="size-4" />
                                View the source
                            </Link>
                            <Link
                                href="/download"
                                className={buttonVariants({ variant: "outline", className: "gap-2" })}
                            >
                                <DownloadIcon className="size-4" />
                                Download Synapse
                            </Link>
                        </>
                    }
                />

                <AnimationContainer delay={0.2}>
                    <dl className="mx-auto grid max-w-4xl grid-cols-2 gap-px overflow-hidden rounded-xl border border-border/60 bg-border/40 pb-14 md:grid-cols-4">
                        {STATS.map((stat) => (
                            <div
                                key={stat.value}
                                className="flex flex-col items-center gap-1 bg-background/80 px-4 py-6 text-center"
                            >
                                <dt className="text-2xl font-medium text-foreground md:text-3xl font-heading">
                                    {stat.value}
                                </dt>
                                <dd className="text-xs text-muted-foreground">{stat.label}</dd>
                            </div>
                        ))}
                    </dl>
                </AnimationContainer>
            </MaxWidthWrapper>

            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="Self-hosting"
                    title="Run a node on hardware you already own"
                    description="Three commands to a node with TLS, its own domain and a first space. Clients point at it by typing the address."
                />
                <AnimationContainer delay={0.2}>
                    <div className="mx-auto grid max-w-5xl gap-4 pb-10 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]">
                        <CodeBlock title="self-host" lines={SELF_HOST_COMMANDS} />
                        <div className="flex flex-col justify-center gap-3 rounded-xl border border-border/60 bg-foreground/[0.02] p-5 text-sm text-muted-foreground">
                            <p>
                                A single small VPS is enough for a few hundred active members. The only
                                external dependency is Postgres; media can live on any S3-compatible
                                bucket.
                            </p>
                            <p>
                                Releases are pinned by version tag, so updating a node is a deliberate act
                                rather than something that happens to it overnight.
                            </p>
                            <p className="text-foreground/90">
                                A self-hosted node still federates — your community keeps talking to the
                                wider network.
                            </p>
                        </div>
                    </div>
                </AnimationContainer>
            </MaxWidthWrapper>

            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="Built to be extended"
                    title="Open in the places that matter"
                    description="Open source is only useful if the layers you actually touch are readable and changeable."
                />
                <div className="grid grid-cols-1 gap-4 pb-10 md:grid-cols-2">
                    {PRINCIPLES.map((principle, id) => (
                        <AnimationContainer delay={0.12 * id} key={principle.title}>
                            <MagicCard className="h-full md:py-8">
                                <div className="flex w-full flex-col items-start justify-center">
                                    <principle.icon strokeWidth={1.5} className="h-9 w-9 text-foreground" />
                                    <h3 className="mt-5 text-base font-medium text-foreground">
                                        {principle.title}
                                    </h3>
                                    <p className="mt-2 text-sm text-muted-foreground">
                                        {principle.description}
                                    </p>
                                </div>
                            </MagicCard>
                        </AnimationContainer>
                    ))}
                </div>
            </MaxWidthWrapper>

            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="Contribute"
                    title="Where the work happens"
                    description="No private roadmap. Issues, RFCs and release notes are the plan."
                />
                <div className="grid grid-cols-1 gap-4 pb-10 md:grid-cols-3 md:gap-6">
                    {CONTRIB.map((entry, id) => (
                        <AnimationContainer delay={0.12 * id} key={entry.title}>
                            <MagicCard className="h-full md:py-8">
                                <div className="flex h-full w-full flex-col items-start justify-start">
                                    <entry.icon strokeWidth={1.5} className="h-9 w-9 text-foreground" />
                                    <h3 className="mt-5 text-base font-medium text-foreground">
                                        {entry.title}
                                    </h3>
                                    <p className="mt-2 text-sm text-muted-foreground">
                                        {entry.description}
                                    </p>
                                    <Link
                                        href={entry.href}
                                        className={buttonVariants({
                                            variant: "ghost",
                                            size: "sm",
                                            className: "mt-4 px-0 text-cyan-300 hover:text-cyan-200",
                                        })}
                                    >
                                        {entry.label}
                                    </Link>
                                </div>
                            </MagicCard>
                        </AnimationContainer>
                    ))}
                </div>
            </MaxWidthWrapper>

            <FinalCta
                title="Fork it, run it, or just use it"
                description="Five clients, one protocol, MIT licensed. Whatever you pick, the source is the same source."
            />
        </div>
    );
};

export default OpenSourcePage;
