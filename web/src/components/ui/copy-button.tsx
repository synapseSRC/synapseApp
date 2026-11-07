"use client";

import { cn } from "@/utils";
import { Check, Copy, LucideIcon } from "lucide-react";
import React, { useState } from 'react'
import { toast } from "sonner";

interface Props {
    text: string;
    className?: string;
    icon?: LucideIcon;
}

const CopyButton = ({ text, className, icon }: Props) => {

    const [isCopied, setIsCopied] = useState<boolean>(false);

    const Comp = icon || Copy;

    const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
        e.stopPropagation();
        // Resolve the visual state from the promise: writeText rejects when the page is not in a
        // secure context or the user denies clipboard permission, and claiming success there both
        // leaves an unhandled rejection and shows a checkmark for a copy that never happened.
        navigator.clipboard.writeText(text).then(() => {
            setIsCopied(true);
            toast.success("Copied to clipboard!");
            setTimeout(() => {
                setIsCopied(false);
            }, 2000);
        }).catch(() => {
            toast.error("Copy failed — clipboard access was denied.");
        });
    };

    return (
        <button
            className={cn(
                "flex items-center justify-center group bg-foreground/10 p-2 transition-all duration-100 hover:scale-105 hover:bg-foreground/20 rounded-md active:scale-95",
                className
            )}
        >
            <span className="sr-only">Copy</span>
            {isCopied ? (
                <Check className="text-neutral-400 group-hover:text-accent" />
            ) : (
                <Comp className="text-neutral-400 group-hover:text-foreground" />
            )}
        </button>
    )
};

export default CopyButton
