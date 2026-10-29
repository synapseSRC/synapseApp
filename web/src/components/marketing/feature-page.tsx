import FinalCta from "@/components/marketing/final-cta";
import { PageHeader, SectionHeader } from "@/components/marketing/page-header";
import { Icons } from "@/components/global/icons";
import AnimationContainer from "@/components/global/animation-container";
import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import { BorderBeam } from "@/components/ui/border-beam";
import { buttonVariants } from "@/components/ui/button";
import MagicCard from "@/components/ui/magic-card";
import { PROJECT_LINKS } from "@/utils/constants/site";
import { DownloadIcon, LucideIcon } from "lucide-react";
import Link from "next/link";
import React from "react";

export type Capability = {
    title: string;
    description: string;
    icon: LucideIcon;
};

type FeaturePageProps = {
    badge: string;
    title: string;
    description: string;
    preview: React.ReactNode;
    previewLabel: string;
    capabilities: Capability[];
    cta: { title: React.ReactNode; description: string };
};

/** Shared layout for the four feature pages. */
const FeaturePage = ({
    badge,
    title,
    description,
    preview,
    previewLabel,
    capabilities,
    cta,
}: FeaturePageProps) => {
    return (
        <div className="overflow-x-hidden">
            <MaxWidthWrapper>
                <PageHeader
                    badge={badge}
                    title={title}
                    description={description}
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
                                href={PROJECT_LINKS.github}
                                className={buttonVariants({ variant: "outline", className: "gap-2" })}
                            >
                                <Icons.github className="size-4" />
                                View the source
                            </Link>
                        </>
                    }
                />

                <AnimationContainer delay={0.2} className="relative w-full pb-8">
                    <div className="relative rounded-2xl p-2 ring-1 ring-inset ring-foreground/15 bg-foreground/[0.02] backdrop-blur-sm">
                        <BorderBeam size={250} duration={12} delay={9} colorFrom="#7C3AED" colorTo="#22D3EE" />
                        {preview}
                    </div>
                    <p className="mt-3 text-center text-xs text-muted-foreground">{previewLabel}</p>
                </AnimationContainer>
            </MaxWidthWrapper>

            <MaxWidthWrapper className="py-10">
                <SectionHeader
                    badge="What you get"
                    title="Built for the way you actually work"
                    description="Every client speaks the same protocol, so nothing here is a mobile-only or desktop-only promise."
                />
                <div className="grid grid-cols-1 gap-4 pb-10 md:grid-cols-3 md:gap-6">
                    {capabilities.map((capability, id) => (
                        <AnimationContainer delay={0.15 * id} key={capability.title}>
                            <MagicCard className="h-full md:py-8">
                                <div className="flex flex-col items-start justify-center w-full">
                                    <capability.icon strokeWidth={1.5} className="h-10 w-10 text-foreground" />
                                    <h3 className="mt-6 text-base font-medium text-foreground">
                                        {capability.title}
                                    </h3>
                                    <p className="mt-2 text-sm text-muted-foreground">
                                        {capability.description}
                                    </p>
                                </div>
                            </MagicCard>
                        </AnimationContainer>
                    ))}
                </div>
            </MaxWidthWrapper>

            <FinalCta title={cta.title} description={cta.description} />
        </div>
    );
};

export default FeaturePage;
