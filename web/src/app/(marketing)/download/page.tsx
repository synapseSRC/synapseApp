import AnimationContainer from "@/components/global/animation-container";
import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import CodeBlock, { CodeLine } from "@/components/marketing/code-block";
import Faq from "@/components/marketing/faq";
import FinalCta from "@/components/marketing/final-cta";
import { PageHeader, SectionHeader } from "@/components/marketing/page-header";
import PlatformCard from "@/components/marketing/platform-card";
import { buttonVariants } from "@/components/ui/button";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import { cn, generateMetadata } from "@/utils";
import { CLI_INSTALL_COMMANDS, PLATFORMS } from "@/utils/constants/platforms";
import { ShieldCheckIcon } from "lucide-react";
import Link from "next/link";

export const metadata = generateMetadata({
    title: "Download Synapse — Android, iOS, Windows, Linux and CLI",
    description:
        "Install Synapse on every device you use: native apps for Android and iOS, desktop builds for Windows and Linux, and a CLI for the terminal. Package managers supported.",
});

const TERMINAL_INSTALL: CodeLine[] = [
    { label: "Windows", command: "winget install Synapse.Synapse" },
    { label: "macOS", command: "brew install synapse" },
    { label: "Debian / Ubuntu", command: "sudo apt install ./synapse_latest_amd64.deb" },
    { label: "Fedora", command: "sudo dnf install synapse" },
    { label: "Any platform with Node.js 18+", command: "npm install -g @synapse/cli" },
    ...CLI_INSTALL_COMMANDS.slice(-1),
];

const DownloadPage = () => {
    return (
        <div className="overflow-x-hidden">
            <MaxWidthWrapper>
                <PageHeader
                    badge="Download"
                    title="Synapse, everywhere you are."
                    description="Native builds for phones, desktops and the terminal. Every one of them speaks the same protocol, so a conversation started on a phone finishes in a shell."
                />

                <div className="grid grid-cols-1 gap-4 pb-8 md:grid-cols-2">
                    {PLATFORMS.map((platform, id) => (
                        <div
                            key={platform.id}
                            id={platform.id}
                            className={cn(
                                "scroll-mt-28",
                                platform.id === "cli" && "md:col-span-2",
                            )}
                        >
                            <AnimationContainer delay={0.1 * id} className="h-full">
                                <PlatformCard platform={platform} className="h-full" />
                            </AnimationContainer>
                        </div>
                    ))}
                </div>
            </MaxWidthWrapper>

            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="From the terminal"
                    title="One command, on every platform"
                    description="No store account required. Package manager builds are the same signed binaries as the direct downloads."
                />
                <AnimationContainer delay={0.2}>
                    <div className="mx-auto grid max-w-5xl gap-4 pb-10 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)]">
                        <CodeBlock title="install" lines={TERMINAL_INSTALL} />
                        <div className="flex flex-col justify-between gap-4 rounded-xl border border-border/60 bg-foreground/[0.02] p-5">
                            <div>
                                <p className="flex items-center gap-2 text-sm font-medium text-foreground">
                                    <ShieldCheckIcon className="size-4 text-cyan-300" />
                                    Reproducible builds
                                </p>
                                <p className="mt-2 text-sm text-muted-foreground">
                                    Every release is built from a tagged commit, published with its
                                    checksum and signed with the same key across all channels. Compare a
                                    binary against the source if you want to.
                                </p>
                            </div>
                            <Link
                                href="/open-source"
                                className={buttonVariants({ variant: "outline", size: "sm" })}
                            >
                                Read the build and self-hosting guide
                            </Link>
                        </div>
                    </div>
                </AnimationContainer>
            </MaxWidthWrapper>

            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="Requirements"
                    title="What your machine needs"
                    description="Synapse is a native client on every platform — no Electron tax on mobile, and the CLI has no GUI dependency at all."
                />
                <AnimationContainer delay={0.2}>
                    <div className="pb-10">
                        <Table className="mx-auto max-w-5xl">
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Platform</TableHead>
                                    <TableHead>Minimum</TableHead>
                                    <TableHead>Available as</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {PLATFORMS.map((platform) => (
                                    <TableRow key={platform.id}>
                                        <TableCell className="font-medium text-foreground">
                                            {platform.name}
                                        </TableCell>
                                        <TableCell className="text-muted-foreground">
                                            {platform.requirement}
                                        </TableCell>
                                        <TableCell className="text-muted-foreground">
                                            {[platform.primary, ...platform.alternates]
                                                .map((channel) => channel.label)
                                                .join(" · ")}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                        <p className="mx-auto mt-4 max-w-5xl text-xs text-muted-foreground">
                            Running your own node instead? A single small VPS with Docker and Postgres is
                            enough for a few hundred active members — see the self-hosting guide.
                        </p>
                    </div>
                </AnimationContainer>
            </MaxWidthWrapper>

            <MaxWidthWrapper className="py-10">
                <div id="faq" className="scroll-mt-28">
                    <SectionHeader
                        badge="FAQ"
                        title="Questions people ask before installing"
                        description="If something is missing here, the repository issues and discussions are open to anyone."
                    />
                    <AnimationContainer delay={0.2}>
                        <div className="mx-auto max-w-3xl pb-16">
                            <Faq />
                        </div>
                    </AnimationContainer>
                </div>
            </MaxWidthWrapper>

            <FinalCta
                title="Install it once. Sync it everywhere."
                description="MIT licensed, no ads, no tracking. Pick a platform above or read the source first — both are fine."
            />
        </div>
    );
};

export default DownloadPage;
