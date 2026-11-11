import { BellIcon } from "lucide-react";

import { requireCurrentUser } from "@/lib/synapse/session";
import { resolveUsername } from "@/lib/synapse/auth";

export const dynamic = "force-dynamic";

const NotificationsPage = async () => {
    const user = await requireCurrentUser();

    return (
        <div className="space-y-4">
            <header className="rounded-2xl border border-border bg-card/60 p-5 backdrop-blur-xl">
                <h1 className="font-heading text-2xl font-semibold tracking-tight">Notifications</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Your activity feed. Signed in as {resolveUsername(user)}.
                </p>
            </header>

            <section className="rounded-2xl border border-dashed border-border bg-card/40 p-10 text-center">
                <BellIcon className="mx-auto size-8 text-muted-foreground" />
                <p className="mt-4 font-heading text-lg font-semibold">No notifications yet</p>
                <p className="mx-auto mt-2 max-w-sm text-sm text-muted-foreground">
                    Likes, replies, and mentions will show up here.
                </p>
            </section>
        </div>
    );
};

export default NotificationsPage;
