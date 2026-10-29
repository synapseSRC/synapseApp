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
    SearchIcon,
    SettingsIcon,
    UserIcon,
} from "lucide-react";

import { getSupabaseBrowserClient } from "@/lib/supabase/client";
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
        <button type="button" onClick={handleSignOut} disabled={pending} className={cn("flex items-center gap-3 text-sm text-neutral-400 transition-colors hover:text-white", className)}>
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
                        pathname.startsWith(href) ? "bg-white/5 text-white" : "text-neutral-400 hover:bg-white/5 hover:text-white",
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
                    pathname.startsWith("/profile") ? "bg-white/5 text-white" : "text-neutral-400 hover:bg-white/5 hover:text-white",
                )}
            >
                <span className="grid size-5 place-items-center rounded-full bg-gradient-to-r from-violet-600 to-cyan-500 text-[10px] font-semibold text-white">
                    S
                </span>
                Profile
            </Link>
            <SignOutButton className="w-full rounded-xl px-4 py-3" />
        </nav>
    );
};

export const AppTabBar = () => {
    const pathname = usePathname();

    return (
        <div className="fixed inset-x-0 bottom-0 z-40 border-t border-neutral-800 bg-neutral-950/90 backdrop-blur-xl lg:hidden">
            <div className="mx-auto flex max-w-lg items-center justify-around px-2 py-2">
                {NAV_LINKS.slice(0, 4).map(({ href, label, icon: Icon }) => (
                    <Link
                        key={href}
                        href={href}
                        aria-label={label}
                        className={cn("flex flex-1 flex-col items-center gap-1 py-1 text-[11px]", pathname.startsWith(href) ? "text-white" : "text-neutral-500")}
                    >
                        <Icon className="size-5" />
                        {label}
                    </Link>
                ))}
                <Link
                    href="/profile"
                    aria-label="Profile"
                    className={cn("flex flex-1 flex-col items-center gap-1 py-1 text-[11px]", pathname.startsWith("/profile") ? "text-white" : "text-neutral-500")}
                >
                    <UserIcon className="size-5" />
                    Profile
                </Link>
            </div>
        </div>
    );
};

export { SignOutButton };
