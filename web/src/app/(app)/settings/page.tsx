import { SlidersIcon } from "lucide-react";

import { requireCurrentUser } from "@/lib/synapse/session";
import { resolveUsername } from "@/lib/synapse/auth";

export const dynamic = "force-dynamic";

const SettingsPage = async () => {
    const user = await requireCurrentUser();

    return (
        <div className="space-y-4">
            <header className="rounded-2xl border border-border bg-card/60 p-5 backdrop-blur-xl">
                <h1 className="font-heading text-2xl font-semibold tracking-tight">Settings</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Manage your account settings. Signed in as {resolveUsername(user)}.
                </p>
            </header>

            <section className="rounded-2xl border border-border bg-card/60 p-5 backdrop-blur-xl space-y-3">
                <h2 className="font-heading text-lg font-semibold">Account</h2>
                <p className="text-sm text-muted-foreground">Account preferences come here. Profile editing lives at /profile/edit.</p>
            </section>

            <section className="rounded-2xl border border-border bg-card/60 p-5 backdrop-blur-xl space-y-3">
                <h2 className="font-heading text-lg font-semibold">Appearance</h2>
                <p className="text-sm text-muted-foreground">Theme and display preferences are toggled in the sidebar.</p>
            </section>

            <SlidersIcon className="mx-auto size-12 opacity-10" />
        </div>
    );
};

export default SettingsPage;
