import type { Metadata } from "next";

import { AuthCard, Failure, LoginForm } from "@/components/synapse/auth-forms";
import { safeNextPath } from "@/lib/synapse/auth";
import { generateMetadata } from "@/utils";

export const metadata: Metadata = {
    ...generateMetadata({ title: "Sign in to Synapse" }),
    robots: "noindex, nofollow",
};

const LoginPage = ({ searchParams }: { searchParams: Record<string, string | string[] | undefined> }) => (
    <AuthCard title="Welcome back" subtitle="Sign in to open your feed, DMs and communities.">
        {typeof searchParams.error === "string" ? <Failure message={searchParams.error} /> : null}
        <LoginForm next={safeNextPath(searchParams.next)} />
    </AuthCard>
);

export default LoginPage;
