import type { Viewport } from "next";
import { ThemeProvider } from "next-themes";

import { Toaster } from "@/components/ui/sonner";
import "@/styles/globals.css";
import { aeonik, cn, generateMetadata, inter } from "@/utils";

export const metadata = generateMetadata();

export const viewport: Viewport = {
    colorScheme: "light dark",
    themeColor: [
        { color: "#ffffff", media: "(prefers-color-scheme: light)" },
        { color: "#0a0a0a", media: "(prefers-color-scheme: dark)" },
    ],
};

export default function RootLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <html lang="en" className="scrollbar" suppressHydrationWarning>
            <body
                className={cn(
                    "min-h-screen bg-background text-foreground antialiased !font-default overflow-x-hidden",
                    aeonik.variable,
                    inter.variable,
                )}
            >
                <ThemeProvider
                    attribute="class"
                    defaultTheme="system"
                    enableSystem
                    disableTransitionOnChange
                    storageKey="synapse-theme"
                >
                    <Toaster richColors position="top-right" />
                    {children}
                </ThemeProvider>
            </body>
        </html>
    );
};
