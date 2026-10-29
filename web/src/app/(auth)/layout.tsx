import Link from "next/link";
import type { ReactNode } from "react";

import { Icons } from "@/components";

const AuthLayout = ({ children }: { children: ReactNode }) => (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden px-4 py-16">
        <div
            aria-hidden
            className="absolute inset-0 bg-[linear-gradient(to_right,#161616_1px,transparent_1px),linear-gradient(to_bottom,#161616_1px,transparent_1px)] bg-[size:3rem_3rem] [mask-image:radial-gradient(ellipse_70%_60%_at_50%_40%,#000_60%,transparent_110%)]"
        />
        <Link href="/" className="relative mb-10 flex items-center gap-2 font-heading text-xl font-semibold tracking-tight">
            <Icons.mark className="size-7" />
            Synapse
        </Link>
        {children}
        <p className="relative mt-10 max-w-md text-center text-xs text-neutral-500">
            By continuing you agree to our{" "}
            <Link href="/terms" className="underline underline-offset-2 hover:text-neutral-300">
                Terms
            </Link>{" "}
            and{" "}
            <Link href="/privacy" className="underline underline-offset-2 hover:text-neutral-300">
                Privacy Policy
            </Link>
            .
        </p>
    </div>
);

export default AuthLayout;
