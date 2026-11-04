"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import {
    BellIcon,
    HomeIcon,
    InboxIcon,
    Loader2Icon,
    LogOutIcon,
    PaletteIcon,
    SearchIcon,
    SettingsIcon,
    UserIcon,
} from "lucide-react";

import { getSupabaseBrowserClient } from "@/lib/supabase/client";
import { ThemeToggle } from "@/components/global/theme-toggle";
import { cn } from "@/utils";

const NAV_LINKS = [
    { href: "/home", label: "Feed", icon: HomeIcon },
    { href: "/search", label: "Search", icon: SearchIcon },
    { href: "/notifications", label: "Notifications", icon: BellIcon },
    { href: "/inbox", label: "Messages", icon: InboxIcon },
    { href: "/settings", label: "Settings", icon: SettingsIcon },
];

const SignOutButton = ({ className }: { className?: string }) => {
    const router = useRouter();
    const [pending, setPending] = useState(false);

    const handleSignOut = async () => {
        setPending(true);
        await getSupabaseBrowserClient().auth.signOut();
        router.replace("/");
        router.refresh();
    };

    return (
        <button type="button" onClick={handleSignOut} disabled={pending} className={cn("flex items-center gap-3 text-sm text-muted-foreground transition-colors hover:text-foreground", className)}>
            {pending ? <Loader2Icon className="size-5 animate-spin" /> : <LogOutIcon className="size-5" />}
            Sign out
        </button>
    );
};

export const AppSidebar = ({ profileHref }: { profileHref: string }) => {
    const pathname = usePathname();

    return (
        <nav className="space-y-1">
            {NAV_LINKS.map(({ href, label, icon: Icon }) => (
                <Link
                    key={href}
                    href={href}
                    className={cn(
                        "flex items-center gap-3 rounded-xl px-4 py-3 text-sm transition-colors",
                        pathname.startsWith(href) ? "bg-accent text-accent-foreground" : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
                    )}
                >
                    <Icon className="size-5" />
                    {label}
                </Link>
            ))}
            <Link
                href={profileHref}
                className={cn(
                    "flex items-center gap-3 rounded-xl px-4 py-3 text-sm transition-colors",
                    pathname.startsWith("/profile") ? "bg-accent text-accent-foreground" : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
                )}
            >
                <span className="grid size-5 place-items-center rounded-full bg-gradient-to-r from-violet-600 to-cyan-500 text-[10px] font-semibold text-white">
                    S
                </span>
                Profile
            </Link>
            <div className="flex items-center gap-3 rounded-xl px-4 py-3 text-sm text-muted-foreground">
                <PaletteIcon className="size-5" />
                Appearance
                <ThemeToggle className="ml-auto" />
            </div>
            <SignOutButton className="w-full rounded-xl px-4 py-3" />
        </nav>
    );
};

export const AppTabBar = () => {
    const pathname = usePathname();

    return (
        <div className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-background/90 backdrop-blur-xl lg:hidden">
            <div className="mx-auto flex max-w-lg items-center justify-around px-2 py-2">
                {NAV_LINKS.slice(0, 4).map(({ href, label, icon: Icon }) => (
                    <Link
                        key={href}
                        href={href}
                        aria-label={label}
                        className={cn("flex flex-1 flex-col items-center gap-1 py-1 text-[11px]", pathname.startsWith(href) ? "text-foreground" : "text-muted-foreground")}
                    >
                        <Icon className="size-5" />
                        {label}
                    </Link>
                ))}
                <Link
                    href="/profile"
                    aria-label="Profile"
                    className={cn("flex flex-1 flex-col items-center gap-1 py-1 text-[11px]", pathname.startsWith("/profile") ? "text-foreground" : "text-muted-foreground")}
                >
                    <UserIcon className="size-5" />
                    Profile
                </Link>
            </div>
        </div>
    );
};

export { SignOutButton };
