import AnimationContainer from "@/components/global/animation-container";
import MagicBadge from "@/components/ui/magic-badge";
import { cn } from "@/utils";
import React from "react";

type PageHeaderProps = {
    badge: string;
    title: React.ReactNode;
    description: string;
    actions?: React.ReactNode;
    className?: string;
};

/** Hero block at the top of a marketing page. */
export function PageHeader({ badge, title, description, actions, className }: PageHeaderProps) {
    return (
        <div className={cn("flex flex-col items-center w-full text-center py-16 md:py-24", className)}>
            <AnimationContainer reverse delay={0.05}>
                <MagicBadge title={badge} />
            </AnimationContainer>
            <AnimationContainer reverse delay={0.1}>
                <h1 className="mt-6 max-w-4xl text-4xl font-medium !leading-[1.12] tracking-tight text-balance text-foreground md:text-6xl font-heading">
                    {title}
                </h1>
            </AnimationContainer>
            <AnimationContainer reverse delay={0.15}>
                <p className="mt-5 max-w-2xl text-lg text-muted-foreground text-balance md:text-xl">
                    {description}
                </p>
            </AnimationContainer>
            {actions && (
                <AnimationContainer reverse delay={0.2}>
                    <div className="mt-9 flex flex-wrap items-center justify-center gap-3">
                        {actions}
                    </div>
                </AnimationContainer>
            )}
        </div>
    );
}

type SectionHeaderProps = {
    badge?: string;
    title: React.ReactNode;
    description?: string;
    align?: "center" | "start";
    className?: string;
};

/** Title block for a section inside a page. */
export function SectionHeader({
    badge,
    title,
    description,
    align = "center",
    className,
}: SectionHeaderProps) {
    return (
        <AnimationContainer delay={0.1}>
            <div
                className={cn(
                    "flex flex-col w-full py-8",
                    align === "center" ? "items-center text-center" : "items-start text-start",
                    className,
                )}
            >
                {badge && <MagicBadge title={badge} />}
                <h2 className="mt-6 text-3xl font-medium !leading-[1.1] tracking-tight text-foreground text-balance md:text-5xl font-heading">
                    {title}
                </h2>
                {description && (
                    <p
                        className={cn(
                            "mt-4 text-lg text-muted-foreground text-balance md:max-w-2xl",
                            align === "center" ? "max-w-lg" : "max-w-xl",
                        )}
                    >
                        {description}
                    </p>
                )}
            </div>
        </AnimationContainer>
    );
}
