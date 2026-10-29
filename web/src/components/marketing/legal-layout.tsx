import AnimationContainer from "@/components/global/animation-container";
import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import Link from "next/link";
import React from "react";

export type LegalSection = {
    heading: string;
    body: React.ReactNode;
};

type LegalLayoutProps = {
    title: string;
    updated: string;
    intro: React.ReactNode;
    sections: LegalSection[];
    closing?: React.ReactNode;
};

/** Shared shell for the privacy policy and terms of service. */
const LegalLayout = ({ title, updated, intro, sections, closing }: LegalLayoutProps) => {
    return (
        <MaxWidthWrapper className="mx-auto max-w-3xl px-8 pb-40">
            <AnimationContainer delay={0.1} className="w-full">
                <h1 className="my-12 w-full text-center text-4xl font-bold font-heading md:text-6xl">
                    {title}
                </h1>
                <p className="mt-16 text-sm italic">{updated}</p>
                <div className="mt-4 leading-relaxed">{intro}</div>
                {sections.map((section) => (
                    <section className="mt-12" key={section.heading}>
                        <h2 className="text-xl font-medium text-foreground">{section.heading}</h2>
                        <div className="mt-4 leading-relaxed text-muted-foreground">
                            {section.body}
                        </div>
                    </section>
                ))}
                {closing && (
                    <p className="mt-12 font-medium leading-relaxed text-foreground">{closing}</p>
                )}
            </AnimationContainer>
        </MaxWidthWrapper>
    );
};

export default LegalLayout;

export const LegalList = ({ items }: { items: React.ReactNode[] }) => (
    <ul className="my-4 list-disc space-y-2 pl-8 marker:text-neutral-700">
        {items.map((item, id) => (
            <li key={id}>{item}</li>
        ))}
    </ul>
);

export const SupportLink = ({ email }: { email: string }) => (
    <Link href={`mailto:${email}`} className="text-foreground underline underline-offset-4">
        {email}
    </Link>
);
