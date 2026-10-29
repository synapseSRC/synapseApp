"use client";

import CopyButton from "@/components/ui/copy-button";
import { cn } from "@/utils";
import React from "react";

export type CodeLine = {
    /** Shell comment printed above the command, e.g. "npm". */
    label?: string;
    command: string;
};

type CodeBlockProps = {
    title?: string;
    lines: (CodeLine | string)[];
    className?: string;
};

/** Terminal window used for install, self-hosting and API snippets. */
const CodeBlock = ({ title = "terminal", lines, className }: CodeBlockProps) => {
    const raw = lines
        .map((line) => (typeof line === "string" ? line.replace(/^#\s?/, "") : line.command))
        .join("\n");

    return (
        <div
            className={cn(
                "relative overflow-hidden rounded-xl border border-border/70 bg-neutral-950/80",
                className,
            )}
        >
            <div className="flex items-center justify-between border-b border-border/60 px-4 py-2.5">
                <div className="flex items-center gap-2">
                    <span className="size-2.5 rounded-full bg-neutral-700" />
                    <span className="size-2.5 rounded-full bg-neutral-700" />
                    <span className="size-2.5 rounded-full bg-neutral-700" />
                </div>
                <span className="text-xs font-medium text-muted-foreground">{title}</span>
                <CopyButton
                    text={raw}
                    className="rounded p-1 bg-transparent hover:bg-foreground/10"
                />
            </div>
            <pre className="overflow-x-auto px-4 py-4 text-[13px] leading-relaxed">
                <code className="font-mono">
                    {lines.map((line, id) =>
                        typeof line === "string" ? (
                            <span key={id} className="block text-muted-foreground">
                                {line}
                            </span>
                        ) : (
                            <React.Fragment key={id}>
                                {line.label && (
                                    <span className="block select-none text-muted-foreground">
                                        # {line.label}
                                    </span>
                                )}
                                <span className="block text-foreground">
                                    <span className="select-none text-cyan-400">$ </span>
                                    {line.command}
                                </span>
                            </React.Fragment>
                        ),
                    )}
                </code>
            </pre>
        </div>
    );
};

export default CodeBlock;
