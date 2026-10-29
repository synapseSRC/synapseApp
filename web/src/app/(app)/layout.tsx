import Link from "next/link";
import type { ReactNode } from "react";

import { Icons } from "@/components";
import { AppSidebar, AppTabBar } from "@/components/synapse/app-nav";
import { getProfileByUid } from "@/lib/synapse/profiles";
import { getCurrentUser } from "@/lib/synapse/session";

const AppLayout = async ({ children }: { children: ReactNode }) => {
    const user = await getCurrentUser();
    const profile = user ? await getProfileByUid(user.id) : null;
    const profileHref = profile?.username ? "/profile" : "/login";

    return (
        <div className="relative min-h-screen">
            <div
                aria-hidden
                className="absolute inset-0 bg-[linear-gradient(to_right,#161616_1px,transparent_1px),linear-gradient(to_bottom,#161616_1px,transparent_1px)] bg-[size:3rem_3rem] [mask-image:radial-gradient(ellipse_80%_60%_at_50%_0%,#000_70%,transparent_110%)]"
            />

            <header className="relative z-30 flex items-center justify-between px-4 py-4 lg:hidden">
                <Link href="/home" className="flex items-center gap-2 font-heading text-lg font-semibold tracking-tight">
                    <Icons.mark className="size-6" />
                    Synapse
                </Link>
                <Link href="/" className="text-xs text-neutral-400 hover:text-white">
                    Back to site
                </Link>
            </header>

            <div className="relative z-10 mx-auto flex w-full max-w-6xl gap-8 px-4 pb-24 pt-2 lg:pb-12 lg:pt-10">
                <aside className="hidden w-56 shrink-0 self-start lg:sticky lg:top-10 lg:block">
                    <Link href="/home" className="mb-8 flex items-center gap-2 font-heading text-xl font-semibold tracking-tight">
                        <Icons.mark className="size-7" />
                        Synapse
                    </Link>
                    <AppSidebar profileHref={profileHref} />
                </aside>

                <main className="min-w-0 flex-1">{children}</main>

                <aside className="hidden w-72 shrink-0 self-start lg:sticky lg:top-10 lg:block">
                    <div className="rounded-2xl border border-neutral-800 bg-neutral-950/60 p-5 backdrop-blur-xl">
                        <p className="font-heading text-sm font-semibold text-white">Synapse Web</p>
                        <p className="mt-2 text-xs leading-relaxed text-neutral-400">
                            Browser client for the same account as Android, iOS, Windows, Linux and the CLI. Public profiles are
                            indexable; messages here are not end-to-end encrypted.
                        </p>
                        <Link href="/download" className="mt-4 inline-block text-xs text-violet-400 underline underline-offset-4 hover:text-violet-300">
                            Get the native app
                        </Link>
                    </div>
                    <div className="mt-4 rounded-2xl border border-neutral-800 bg-neutral-950/60 p-5 text-xs text-neutral-400 backdrop-blur-xl">
                        Signed in as{" "}
                        <Link href={profileHref} className="text-white underline underline-offset-4">
                            {profile?.username ?? "guest"}
                        </Link>
                    </div>
                </aside>
            </div>

            <AppTabBar />
        </div>
    );
};

export default AppLayout;
