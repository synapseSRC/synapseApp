import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { AuthCard, ResetPasswordForm } from "@/components/synapse/auth-forms";
import { createSupabaseServerClient } from "@/lib/supabase/server";
import { generateMetadata } from "@/utils";

export const metadata: Metadata = {
    ...generateMetadata({ title: "Choose a new Synapse password" }),
    robots: "noindex, nofollow",
};

const ResetPasswordPage = async () => {
    const supabase = createSupabaseServerClient();
    const { data } = await supabase.auth.getUser();

    if (!data.user) {
        redirect(`/login?error=${encodeURIComponent("Open the reset link we emailed you to continue.")}`);
    }

    return (
        <AuthCard title="Choose a new password" subtitle="Your reset link is valid — pick a password you haven't used here before.">
            <ResetPasswordForm />
        </AuthCard>
    );
};

export default ResetPasswordPage;
