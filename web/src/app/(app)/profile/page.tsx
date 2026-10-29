import type { Metadata } from "next";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { SignOutButton } from "@/components/synapse/app-nav";
import { PostCard } from "@/components/synapse/post-card";
import { ProfileHeader } from "@/components/synapse/profile-header";
import { getPostsByAuthor, type FeedEntry } from "@/lib/synapse/posts";
import { resolveCoverUrl } from "@/lib/synapse/profile-media";
import { getOrCreateOwnProfile } from "@/lib/synapse/profiles";
import { requireCurrentUser } from "@/lib/synapse/session";
import { cn, generateMetadata as buildMetadata } from "@/utils";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
    ...buildMetadata({ title: "Your profile on Synapse" }),
    robots: "noindex, nofollow",
};

const ProfileScreenPage = async () => {
    const user = await requireCurrentUser("/profile");
    const profile = await getOrCreateOwnProfile(user);

    if (!profile) {
        return (
            <div className="rounded-2xl border border-red-500/30 bg-red-500/10 p-5 text-sm text-red-200">
                We couldn&apos;t load your profile.{" "}
                <Link href="/profile" className="underline underline-offset-4">
                    Try again
                </Link>
            </div>
        );
    }

    const posts: FeedEntry[] | { error: string } = await getPostsByAuthor(user.id).catch((error: Error) => ({
        error: error.message,
    }));
    const coverUrl = await resolveCoverUrl(profile.profile_cover_image);

    return (
        <div className="space-y-4">
            <ProfileHeader
                profile={profile}
                coverUrl={coverUrl}
                showDetails
                action={
                    <>
                        {profile.username ? (
                            <Link
                                href={`/u/${profile.username}`}
                                className={buttonVariants({ variant: "ghost", size: "sm", className: "rounded-lg" })}
                            >
                                Public profile
                            </Link>
                        ) : null}
                        <Link
                            href="/profile/edit"
                            className={buttonVariants({ variant: "outline", size: "sm", className: "rounded-lg" })}
                        >
                            Edit profile
                        </Link>
                        <SignOutButton className={cn(buttonVariants({ variant: "ghost", size: "sm" }), "gap-2 rounded-lg")} />
                    </>
                }
            />

            {"error" in posts ? (
                <div className="rounded-2xl border border-red-500/30 bg-red-500/10 p-5 text-sm text-red-200">
                    Could not load your posts: {posts.error}
                </div>
            ) : posts.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-neutral-800 bg-neutral-950/40 p-8 text-center">
                    <p className="font-heading text-lg font-semibold">You haven&apos;t posted yet</p>
                    <p className="mx-auto mt-2 max-w-sm text-sm text-neutral-400">
                        Posts from the native apps and CLI will show up here.
                    </p>
                </div>
            ) : (
                posts.map((entry) => <PostCard key={entry.post.id} entry={{ ...entry, author: profile }} />)
            )}
        </div>
    );
};

export default ProfileScreenPage;
