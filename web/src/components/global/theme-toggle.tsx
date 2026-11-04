"use client";

import { useEffect, useState } from "react";
import { useTheme } from "next-themes";
import { MoonIcon, SunIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/utils";

export const ThemeToggle = ({ className }: { className?: string }) => {
    const { resolvedTheme, setTheme } = useTheme();

    // next-themes resolves the theme synchronously from <html> on the client but not
    // during SSR, so nothing derived from it may reach the first render — labels included.
    const [mounted, setMounted] = useState(false);

    useEffect(() => setMounted(true), []);

    const isDark = mounted && resolvedTheme === "dark";
    const label = isDark ? "Switch to light theme" : "Switch to dark theme";

    return (
        <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label={label}
            title={label}
            disabled={!mounted}
            onClick={() => setTheme(isDark ? "light" : "dark")}
            className={cn("text-muted-foreground hover:text-foreground", className)}
        >
            {!mounted ? (
                <span className="size-4" aria-hidden />
            ) : isDark ? (
                <SunIcon className="size-4" />
            ) : (
                <MoonIcon className="size-4" />
            )}
        </Button>
    );
};
