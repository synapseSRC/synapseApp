import type { Metadata } from "next";

import { AuthCard, SignUpForm } from "@/components/synapse/auth-forms";
import { safeNextPath } from "@/lib/synapse/auth";
import { generateMetadata } from "@/utils";

export const metadata: Metadata = {
    ...generateMetadata({ title: "Create a Synapse account" }),
    robots: "noindex, nofollow",
};

const SignUpPage = ({ searchParams }: { searchParams: Record<string, string | string[] | undefined> }) => (
    <AuthCard title="Create your account" subtitle="One Synapse identity across Android, iOS, Windows, Linux and the CLI.">
        <SignUpForm next={safeNextPath(searchParams.next)} />
    </AuthCard>
);

export default SignUpPage;
