"use client";

import { Icons } from "@/components/global/icons";
import AnimationContainer from "@/components/global/animation-container";
import { buttonVariants } from "@/components/ui/button";
import { LampContainer } from "@/components/ui/lamp";
import { PROJECT_LINKS } from "@/utils/constants/site";
import { cn } from "@/utils";
import { DownloadIcon } from "lucide-react";
import Link from "next/link";
import React from "react";

type FinalCtaProps = {
    title: React.ReactNode;
    description: string;
    className?: string;
};

/** Closing call to action used at the foot of every marketing page. */
const FinalCta = ({ title, description, className }: FinalCtaProps) => {
    return (
        <div className={cn("w-full overflow-x-hidden", className)}>
            <AnimationContainer delay={0.1}>
                <LampContainer>
                    <div className="relative flex w-full flex-col items-center justify-center text-center">
                        <h2 className="mt-8 bg-gradient-to-b from-neutral-200 to-neutral-400 py-4 text-4xl font-medium !leading-[1.15] tracking-tight text-transparent bg-clip-text font-heading md:text-7xl text-balance">
                            {title}
                        </h2>
                        <p className="mx-auto mt-6 max-w-md text-muted-foreground">
                            {description}
                        </p>
                        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
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
                                View the source
                            </Link>
                        </div>
                    </div>
                </LampContainer>
            </AnimationContainer>
        </div>
    );
};

export default FinalCta;
