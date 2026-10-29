import { Icons } from "@/components";
import AnimationContainer from "@/components/global/animation-container";
import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import AppPreview from "@/components/marketing/app-preview";
import FinalCta from "@/components/marketing/final-cta";
import { SectionHeader } from "@/components/marketing/page-header";
import PlatformCard from "@/components/marketing/platform-card";
import PlatformStrip from "@/components/marketing/platform-strip";
import { FEATURE_CARDS } from "@/components/marketing/feature-cards";
import { BentoCard, BentoGrid } from "@/components/ui/bento-grid";
import { BorderBeam } from "@/components/ui/border-beam";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import MagicCard from "@/components/ui/magic-card";
import { PLATFORMS, PROCESS, PROJECT_LINKS, REVIEWS } from "@/utils";
import { ArrowRightIcon, DownloadIcon, StarIcon } from "lucide-react";
import Link from "next/link";

const HomePage = () => {
    return (
        <div className="size-full overflow-x-hidden scrollbar-hide">
            {/* Hero */}
            <MaxWidthWrapper>
                <div className="flex w-full flex-col items-center justify-center bg-gradient-to-t from-background text-center">
                    <AnimationContainer className="flex w-full flex-col items-center justify-center text-center">
                        <Link
                            href="/changelog"
                            className="group relative grid overflow-hidden rounded-full px-4 py-1 shadow-[0_1000px_0_0_hsl(0_0%_20%)_inset] transition-colors duration-200"
                        >
                            <span>
                                <span className="spark mask-gradient absolute inset-0 h-[100%] w-[100%] animate-flip overflow-hidden rounded-full [mask:linear-gradient(white,_transparent_50%)] before:absolute before:aspect-square before:w-[200%] before:rotate-[-90deg] before:animate-rotate before:bg-[conic-gradient(from_0deg,transparent_0_340deg,white_360deg)] before:content-[''] before:[inset:0_auto_auto_50%] before:[translate:-50%_-15%]" />
                            </span>
                            <span className="backdrop absolute inset-[1px] rounded-full bg-neutral-950 transition-colors duration-200 group-hover:bg-neutral-900" />
                            <span className="h-full w-full blur-md absolute bottom-0 inset-x-0 bg-gradient-to-tr from-primary/20"></span>
                            <span className="z-10 flex h-full w-full items-center justify-center gap-1 py-0.5 text-sm text-neutral-100">
                                ✨ Synapse v1.0 is out
                                <ArrowRightIcon className="ml-1 size-3 transition-transform duration-300 ease-in-out group-hover:translate-x-0.5" />
                            </span>
                        </Link>

                        <h1 className="w-full py-6 text-5xl font-medium tracking-normal text-balance !leading-[1.15] text-foreground sm:text-6xl md:text-7xl lg:text-8xl font-heading">
                            Mean to be{" "}
                            <span className="inline-block bg-gradient-to-r from-violet-500 to-cyan-400 bg-clip-text text-transparent">
                                connected
                            </span>
                        </h1>

                        <p className="mb-4 max-w-3xl text-lg tracking-tight text-muted-foreground text-balance md:text-xl">
                            The open-source social app for DMs, communities and feeds — with an API,
                            a CLI and self-hosting built in.
                            <br className="hidden md:block" />
                            <span className="hidden md:block">
                                Android, iOS, Windows, Linux, and the terminal.
                            </span>
                        </p>

                        <p className="mb-10 flex flex-wrap items-center justify-center gap-x-2 gap-y-1 text-xs uppercase tracking-wide text-muted-foreground/80">
                            <span>MIT licensed</span>
                            <span aria-hidden>·</span>
                            <span>No ads</span>
                            <span aria-hidden>·</span>
                            <span>No tracking</span>
                        </p>

                        <div className="z-50 flex w-full flex-wrap items-center justify-center gap-4 sm:w-auto">
                            <Link
                                href="/download"
                                className={buttonVariants({ variant: "accent", size: "lg", className: "gap-2" })}
                            >
                                <DownloadIcon className="size-4" />
                                Download Synapse
                            </Link>
                            <Link
                                href={PROJECT_LINKS.github}
                                className={buttonVariants({ variant: "outline", size: "lg", className: "gap-2" })}
                            >
                                <Icons.github className="size-4" />
                                Star on GitHub
                            </Link>
                        </div>
                    </AnimationContainer>

                    <AnimationContainer delay={0.2} className="relative w-full px-2 pb-20 pt-20 md:py-32">
                        <div className="gradient absolute left-1/2 top-[10%] z-0 h-1/4 w-3/4 -translate-x-1/2 animate-image-glow blur-[5rem] md:h-1/3"></div>
                        <div className="relative -m-2 rounded-xl bg-opacity-50 p-2 ring-1 ring-inset ring-foreground/20 backdrop-blur-3xl lg:-m-4 lg:rounded-2xl">
                            <BorderBeam
                                size={250}
                                duration={12}
                                delay={9}
                                colorFrom="#7C3AED"
                                colorTo="#22D3EE"
                            />
                            <AppPreview className="ring-1 ring-border" />
                            <div className="absolute -bottom-4 inset-x-0 z-40 h-1/2 w-full bg-gradient-to-t from-background"></div>
                            <div className="absolute bottom-0 inset-x-0 z-50 h-1/4 w-full bg-gradient-to-t from-background md:-bottom-8"></div>
                        </div>
                    </AnimationContainer>
                </div>
            </MaxWidthWrapper>

            {/* Platform strip */}
            <MaxWidthWrapper>
                <AnimationContainer delay={0.4}>
                    <PlatformStrip />
                </AnimationContainer>
            </MaxWidthWrapper>

            {/* Features */}
            <MaxWidthWrapper className="pt-10">
                <SectionHeader
                    badge="Features"
                    title="Social, with the hood open."
                    description="Encrypted messaging, organized communities and real-time sync — plus the API and CLI to make it yours."
                />
                <AnimationContainer delay={0.2}>
                    <div id="features" className="scroll-mt-24">
                        <BentoGrid className="py-8">
                            {FEATURE_CARDS.map((feature) => (
                                <BentoCard key={feature.name} {...feature} />
                            ))}
                        </BentoGrid>
                    </div>
                </AnimationContainer>
            </MaxWidthWrapper>

            {/* Process */}
            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="How it works"
                    title="Running your own social network in three steps"
                    description="Start on the hosted network, or bring your own node. Nothing about Synapse requires permission from us."
                />
                <div className="grid w-full grid-cols-1 gap-4 py-8 md:grid-cols-2 md:gap-8 lg:grid-cols-3">
                    {PROCESS.map((process, id) => (
                        <AnimationContainer delay={0.2 * id} key={process.title}>
                            <MagicCard className="group h-full md:py-8">
                                <div className="flex w-full flex-col items-start justify-center">
                                    <process.icon strokeWidth={1.5} className="h-10 w-10 text-foreground" />
                                    <div className="relative flex flex-col items-start">
                                        <span className="absolute -top-6 right-0 flex h-12 w-12 items-center justify-center rounded-full border-2 border-border pt-0.5 text-2xl font-medium text-foreground">
                                            {id + 1}
                                        </span>
                                        <h3 className="mt-6 text-base font-medium text-foreground">
                                            {process.title}
                                        </h3>
                                        <p className="mt-2 text-sm text-muted-foreground">
                                            {process.description}
                                        </p>
                                    </div>
                                </div>
                            </MagicCard>
                        </AnimationContainer>
                    ))}
                </div>
            </MaxWidthWrapper>

            {/* Downloads */}
            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="Downloads"
                    title="Pick your platform"
                    description="Native builds for every platform, or script the whole thing from a terminal you already use."
                />
                <AnimationContainer delay={0.2}>
                    <div className="grid grid-cols-1 gap-4 py-8 md:grid-cols-2 lg:grid-cols-3">
                        {PLATFORMS.map((platform) => (
                            <PlatformCard key={platform.id} platform={platform} size="compact" />
                        ))}
                        <Link
                            href="/download"
                            className="flex flex-col items-start justify-center gap-2 rounded-xl border border-dashed border-border/70 p-5 text-sm text-muted-foreground transition-colors duration-300 hover:border-cyan-400/40 hover:text-foreground"
                        >
                            <span className="flex items-center gap-2 font-medium text-foreground">
                                Every build, side by side
                                <ArrowRightIcon className="size-4" />
                            </span>
                            Store links, package managers, system requirements and the FAQ.
                        </Link>
                    </div>
                </AnimationContainer>
            </MaxWidthWrapper>

            {/* Community reviews */}
            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="Community"
                    title="Built in the open, used everywhere."
                    description="What people tell us after they move a real community onto Synapse."
                />
                <div className="grid grid-cols-1 place-items-start gap-4 py-10 md:grid-cols-2 md:gap-8 lg:grid-cols-3">
                    {[0, 1, 2].map((column) => (
                        <div className="flex flex-col items-start gap-6" key={column}>
                            {REVIEWS.slice(column * 3, column * 3 + 3).map((review, index) => (
                                <AnimationContainer delay={0.2 * index} key={review.username}>
                                    <MagicCard className="md:p-0">
                                        <Card className="flex h-min w-full flex-col border-none">
                                            <CardContent className="space-y-4 p-4 pb-4">
                                                <div className="flex items-center gap-3">
                                                    <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-violet-500/30 to-cyan-400/30 text-xs font-semibold text-foreground">
                                                        {review.name.split(" ")[0].slice(0, 2).toUpperCase()}
                                                    </span>
                                                    <div className="min-w-0">
                                                        <p className="truncate text-sm font-medium text-foreground">
                                                            {review.name}
                                                        </p>
                                                        <p className="truncate text-xs text-muted-foreground">
                                                            {review.username} · {review.context}
                                                        </p>
                                                    </div>
                                                </div>
                                                <p className="text-sm text-muted-foreground">{review.review}</p>
                                                <div className="flex w-full items-center gap-1">
                                                    {Array.from({ length: review.rating }, (_, star) => (
                                                        <StarIcon
                                                            key={star}
                                                            className="size-4 fill-yellow-500 text-yellow-500"
                                                        />
                                                    ))}
                                                </div>
                                            </CardContent>
                                        </Card>
                                    </MagicCard>
                                </AnimationContainer>
                            ))}
                        </div>
                    ))}
                </div>
            </MaxWidthWrapper>

            <FinalCta
                title="Take your social graph with you"
                description="Install a client, run a node, or fork the protocol. Five platforms, one open source codebase, no lock-in."
            />
        </div>
    );
};

export default HomePage;
