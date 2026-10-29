import type { Metadata } from "next";

import { AuthCard, ForgotPasswordForm } from "@/components/synapse/auth-forms";
import { generateMetadata } from "@/utils";

export const metadata: Metadata = {
    ...generateMetadata({ title: "Reset your Synapse password" }),
    robots: "noindex, nofollow",
};

const ForgotPasswordPage = () => (
    <AuthCard title="Forgot your password?" subtitle="We'll email you a link to choose a new one.">
        <ForgotPasswordForm />
    </AuthCard>
);

export default ForgotPasswordPage;
