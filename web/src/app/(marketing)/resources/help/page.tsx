import { Icons } from "@/components/global/icons";
import AnimationContainer from "@/components/global/animation-container";
import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import FinalCta from "@/components/marketing/final-cta";
import { PageHeader, SectionHeader } from "@/components/marketing/page-header";
import { buttonVariants } from "@/components/ui/button";
import MagicCard from "@/components/ui/magic-card";
import { FAQ } from "@/utils/constants/faq";
import { PROJECT_LINKS, SUPPORT_EMAIL } from "@/utils/constants/site";
import { generateMetadata } from "@/utils";
import {
    ArrowUpRightIcon,
    DownloadIcon,
    MailIcon,
    MessageCircleIcon,
    RefreshCwIcon,
    ServerIcon,
} from "lucide-react";
import Link from "next/link";

export const metadata = generateMetadata({
    title: "Help and support — Synapse",
    description:
        "Get help with installing Synapse, syncing accounts across devices, or running your own node — plus the FAQ, support email and repository issues.",
});

const TOPICS = [
    {
        title: "Install Synapse",
        description:
            "Store links, package managers and system requirements for Android, iOS, Windows, Linux and the CLI.",
        href: "/download",
        label: "Go to downloads",
        icon: DownloadIcon,
    },
    {
        title: "Accounts and syncing",
        description:
            "Encryption defaults, offline drafts, handoff between devices and how updates reach you — covered in the download FAQ.",
        href: "/download#faq",
        label: "Read the FAQ",
        icon: RefreshCwIcon,
    },
    {
        title: "Self-hosting a node",
        description:
            "Bring your own server: the three-command setup, Postgres and media storage, version pinning and federating with the wider network.",
        href: "/open-source",
        label: "Open-source guide",
        icon: ServerIcon,
    },
];

const HelpPage = () => {
    return (
        <div className="overflow-x-hidden">
            <MaxWidthWrapper>
                <PageHeader
                    badge="Help"
                    title="How can we help?"
                    description="Start with the three things people ask about most. Everything below is documented in the open, and so is the answer if it is not here yet."
                />

                <div className="grid grid-cols-1 gap-4 pb-12 md:grid-cols-3 md:gap-6">
                    {TOPICS.map((topic, id) => (
                        <AnimationContainer delay={0.12 * id} key={topic.title}>
                            <MagicCard className="h-full md:py-8">
                                <div className="flex h-full w-full flex-col items-start justify-start">
                                    <topic.icon strokeWidth={1.5} className="h-9 w-9 text-foreground" />
                                    <h2 className="mt-5 text-base font-medium text-foreground">
                                        {topic.title}
                                    </h2>
                                    <p className="mt-2 text-sm text-muted-foreground">
                                        {topic.description}
                                    </p>
                                    <Link
                                        href={topic.href}
                                        className={buttonVariants({
                                            variant: "ghost",
                                            size: "sm",
                                            className: "mt-4 px-0 text-cyan-300 hover:text-cyan-200",
                                        })}
                                    >
                                        {topic.label}
                                        <ArrowUpRightIcon className="ml-1 size-3.5" />
                                    </Link>
                                </div>
                            </MagicCard>
                        </AnimationContainer>
                    ))}
                </div>
            </MaxWidthWrapper>

            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="Ask a human"
                    title="Two ways to reach the maintainers"
                    description="No ticket queue, no chatbot. Whichever you pick, the answer ends up somewhere other people can find."
                />
                <div className="mx-auto grid max-w-3xl grid-cols-1 gap-4 pb-12 md:grid-cols-2">
                    <AnimationContainer delay={0.15}>
                        <MagicCard className="h-full md:py-8">
                            <div className="flex h-full w-full flex-col items-start justify-start">
                                <MailIcon strokeWidth={1.5} className="h-9 w-9 text-foreground" />
                                <h3 className="mt-5 text-base font-medium text-foreground">
                                    Support email
                                </h3>
                                <p className="mt-2 text-sm text-muted-foreground">
                                    Account, privacy or self-hosting problems that need a private answer.
                                </p>
                                <Link
                                    href={`mailto:${SUPPORT_EMAIL}`}
                                    className={buttonVariants({
                                        variant: "ghost",
                                        size: "sm",
                                        className: "mt-4 px-0 text-cyan-300 hover:text-cyan-200",
                                    })}
                                >
                                    {SUPPORT_EMAIL}
                                </Link>
                            </div>
                        </MagicCard>
                    </AnimationContainer>
                    <AnimationContainer delay={0.25}>
                        <MagicCard className="h-full md:py-8">
                            <div className="flex h-full w-full flex-col items-start justify-start">
                                <MessageCircleIcon strokeWidth={1.5} className="h-9 w-9 text-foreground" />
                                <h3 className="mt-5 text-base font-medium text-foreground">
                                    Repository issues
                                </h3>
                                <p className="mt-2 text-sm text-muted-foreground">
                                    Bugs and feature requests, in public, with a version number and a trace.
                                </p>
                                <Link
                                    href={PROJECT_LINKS.issues}
                                    className={buttonVariants({
                                        variant: "ghost",
                                        size: "sm",
                                        className: "mt-4 px-0 text-cyan-300 hover:text-cyan-200",
                                    })}
                                >
                                    <Icons.github className="mr-2 size-3.5" />
                                    Open an issue
                                </Link>
                            </div>
                        </MagicCard>
                    </AnimationContainer>
                </div>
            </MaxWidthWrapper>

            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="Common questions"
                    title="Answered before you ask"
                    description="The full list lives on the download page."
                />
                <div className="mx-auto flex max-w-3xl flex-col gap-2 pb-12">
                    {FAQ.slice(0, 5).map((item, id) => (
                        <AnimationContainer delay={0.08 * id} key={item.id}>
                            <Link
                                href={`/download#${item.id}`}
                                className="flex items-center justify-between gap-4 rounded-lg border border-border/50 bg-foreground/[0.02] px-4 py-3 text-sm text-foreground/90 transition-colors duration-300 hover:border-border hover:bg-foreground/[0.05]"
                            >
                                <span>{item.question}</span>
                                <ArrowUpRightIcon className="size-4 shrink-0 text-muted-foreground" />
                            </Link>
                        </AnimationContainer>
                    ))}
                </div>
            </MaxWidthWrapper>

            <FinalCta
                title="Still want to poke around first?"
                description="Read the source, run a node on a spare machine, or install a client and join the hosted network — all three work without talking to us."
            />
        </div>
    );
};

export default HelpPage;
