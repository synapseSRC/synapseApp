import { LEGAL_DOCS } from "@/utils/constants/legal";
import { SUPPORT_EMAIL } from "@/utils/constants/site";
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

const slugify = (value: string) =>
    value
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "");

/** Document body shared by the terms, privacy and cookie pages. */
const LegalLayout = ({ title, updated, intro, sections, closing }: LegalLayoutProps) => {
    return (
        <>
            <h1 className="mt-10 w-full text-center text-4xl font-bold font-heading md:text-6xl">{title}</h1>
            <p className="mt-6 text-center text-sm italic text-muted-foreground">{updated}</p>

            <div className="mt-10 leading-relaxed">{intro}</div>

            <nav aria-label="On this page" className="mt-12 rounded-2xl border border-border/70 p-6">
                <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">On this page</p>
                <ol className="mt-4 grid gap-x-8 gap-y-2 text-sm sm:grid-cols-2">
                    {sections.map((section, index) => (
                        <li key={section.heading}>
                            <Link
                                href={`#${slugify(section.heading)}`}
                                className="text-muted-foreground transition-colors hover:text-foreground"
                            >
                                <span className="mr-2 text-xs tabular-nums text-muted-foreground/60">
                                    {String(index + 1).padStart(2, "0")}
                                </span>
                                {section.heading}
                            </Link>
                        </li>
                    ))}
                </ol>
            </nav>

            {sections.map((section) => (
                <section className="mt-12 scroll-mt-24" key={section.heading} id={slugify(section.heading)}>
                    <h2 className="text-xl font-medium text-foreground">{section.heading}</h2>
                    <div className="mt-4 leading-relaxed text-muted-foreground">{section.body}</div>
                </section>
            ))}

            {closing && <p className="mt-12 font-medium leading-relaxed text-foreground">{closing}</p>}

            <div className="mt-16 flex flex-wrap items-center gap-x-6 gap-y-2 border-t border-border/70 pt-6 text-sm text-muted-foreground">
                {LEGAL_DOCS.map((doc) => (
                    <Link key={doc.id} href={doc.href} className="transition-colors hover:text-foreground">
                        {doc.label}
                    </Link>
                ))}
                <SupportLink email={SUPPORT_EMAIL} />
            </div>
        </>
    );
};

export default LegalLayout;

export const LegalLink = ({ href, children }: { href: string; children: React.ReactNode }) => {
    const external = /^https?:/.test(href);
    return (
        <Link
            href={href}
            target={external ? "_blank" : undefined}
            rel={external ? "noreferrer noopener" : undefined}
            className="text-foreground underline underline-offset-4 transition-colors hover:text-cyan-300"
        >
            {children}
        </Link>
    );
};

export const LegalList = ({ items }: { items: React.ReactNode[] }) => (
    <ul className="my-4 list-disc space-y-2 pl-8 marker:text-neutral-700">
        {items.map((item, id) => (
            <li key={id}>{item}</li>
        ))}
    </ul>
);

export const LegalTable = ({
    head,
    rows,
}: {
    head: React.ReactNode[];
    rows: React.ReactNode[][];
}) => (
    <>
        <div className="my-6 hidden overflow-x-auto rounded-xl border border-border/70 sm:block">
            <table className="w-full min-w-[36rem] border-collapse text-left text-sm">
                <thead className="bg-muted/40 text-foreground">
                    <tr>
                        {head.map((cell, index) => (
                            <th key={index} scope="col" className="px-4 py-3 font-medium">
                                {cell}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody className="divide-y divide-border/60">
                    {rows.map((row, rowIndex) => (
                        <tr key={rowIndex} className="align-top">
                            {row.map((cell, cellIndex) => (
                                <td key={cellIndex} className="px-4 py-3">
                                    {cell}
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
        <div className="my-6 space-y-3 sm:hidden">
            {rows.map((row, rowIndex) => (
                <dl key={rowIndex} className="space-y-3 rounded-xl border border-border/70 p-4">
                    {row.map((cell, cellIndex) => (
                        <div key={cellIndex}>
                            <dt className="text-xs uppercase tracking-wide text-muted-foreground">{head[cellIndex]}</dt>
                            <dd className="mt-1 text-sm">{cell}</dd>
                        </div>
                    ))}
                </dl>
            ))}
        </div>
    </>
);

export const SupportLink = ({ email }: { email: string }) => (
    <Link href={`mailto:${email}`} className="text-foreground underline underline-offset-4 transition-colors hover:text-cyan-300">
        {email}
    </Link>
);
