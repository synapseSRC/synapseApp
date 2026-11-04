import { SparklesIcon } from "lucide-react";

import { PostCard } from "@/components/synapse/post-card";
import { getFeed } from "@/lib/synapse/posts";
import { requireCurrentUser } from "@/lib/synapse/session";
import { resolveUsername } from "@/lib/synapse/auth";

export const dynamic = "force-dynamic";

const FeedPage = async () => {
    const user = await requireCurrentUser();
    const feed = await getFeed({ limit: 20 });

    return (
        <div className="space-y-4">
            <header className="rounded-2xl border border-border bg-card/60 p-5 backdrop-blur-xl">
                <h1 className="font-heading text-2xl font-semibold tracking-tight">Feed</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Latest posts across Synapse, signed in as {resolveUsername(user)}.
                </p>
            </header>

            {feed.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-border bg-card/40 p-10 text-center">
                    <SparklesIcon className="mx-auto size-8 text-violet-600 dark:text-violet-400" />
                    <p className="mt-4 font-heading text-lg font-semibold">Nothing to show yet</p>
                    <p className="mx-auto mt-2 max-w-sm text-sm text-muted-foreground">
                        The feed reads live posts from your Synapse project. Publish the first one and it lands here.
                    </p>
                </div>
            ) : (
                feed.map((entry) => <PostCard key={entry.post.id} entry={entry} />)
            )}
        </div>
    );
};

export default FeedPage;
