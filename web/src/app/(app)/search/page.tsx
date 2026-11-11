import { SearchIcon } from "lucide-react";

import { requireCurrentUser } from "@/lib/synapse/session";
import { resolveUsername } from "@/lib/synapse/auth";

export const dynamic = "force-dynamic";

const SearchPage = async () => {
    const user = await requireCurrentUser();

    return (
        <div className="space-y-4">
            <header className="rounded-2xl border border-border bg-card/60 p-5 backdrop-blur-xl">
                <h1 className="font-heading text-2xl font-semibold tracking-tight">Search</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Search users and posts. Signed in as {resolveUsername(user)}.
                </p>
            </header>

            <section className="rounded-2xl border border-border bg-card/60 p-5 backdrop-blur-xl">
                <p className="text-sm text-muted-foreground">Search results appear here.</p>
            </section>

            <SearchIcon className="mx-auto size-12 opacity-10" />
        </div>
    );
};

export default SearchPage;
