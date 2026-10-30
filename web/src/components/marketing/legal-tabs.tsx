"use client";

import { LEGAL_DOCS } from "@/utils/constants/legal";
import { cn } from "@/utils";
import { MotionConfig, motion } from "framer-motion";
import Link from "next/link";
import { usePathname } from "next/navigation";

export const LegalTabs = () => {
    const pathname = usePathname();

    return (
        <MotionConfig reducedMotion="user">
            <nav aria-label="Legal documents" className="flex flex-wrap items-center justify-center gap-2 pt-10">
                {LEGAL_DOCS.map((doc) => {
                    const active = pathname === doc.href;

                    return (
                        <Link
                            key={doc.id}
                            href={doc.href}
                            aria-current={active ? "page" : undefined}
                            className={cn(
                                "relative rounded-full border px-4 py-1.5 text-sm transition-colors duration-200",
                                active
                                    ? "border-foreground/30 text-foreground"
                                    : "border-border/70 text-muted-foreground hover:border-border hover:text-foreground",
                            )}
                        >
                            {active && (
                                <motion.span
                                    layoutId="legal-doc-indicator"
                                    className="absolute inset-0 rounded-full bg-foreground/10"
                                    transition={{ type: "spring", stiffness: 380, damping: 32 }}
                                />
                            )}
                            <span className="relative">{doc.label}</span>
                        </Link>
                    );
                })}
            </nav>
        </MotionConfig>
    );
};
