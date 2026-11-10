"use client";

import { useEffect } from "react";

import { buttonVariants } from "@/components/ui/button";

/**
 * Route-level error boundary for every page under app/.
 *
 * Without it a thrown error (a transient database failure inside (app)/layout.tsx, for example)
 * falls through to Next's built-in overlay, and any diagnostic text the framework renders is
 * shown to the visitor. The message stays generic here and the detail is logged instead.
 */
const ErrorBoundary = ({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) => {
    useEffect(() => {
        console.error(error);
    }, [error]);

    return (
        <div className="mx-auto flex min-h-[60vh] max-w-md flex-col items-center justify-center gap-4 px-4 text-center">
            <h1 className="font-heading text-2xl font-semibold tracking-tight">Something went wrong</h1>
            <p className="text-sm text-muted-foreground">
                This page could not be loaded. The problem was reported and we are looking into it.
            </p>
            <button
                type="button"
                onClick={reset}
                className={buttonVariants({ variant: "accent", size: "lg", className: "rounded-lg" })}
            >
                Try again
            </button>
        </div>
    );
};

export default ErrorBoundary;
