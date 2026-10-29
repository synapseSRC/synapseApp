import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { buttonVariants } from "@/components/ui/button";
import { PostCard } from "@/components/synapse/post-card";
import { ProfileHeader } from "@/components/synapse/profile-header";
import { getPostsByAuthor, type FeedEntry } from "@/lib/synapse/posts";
import { resolveCoverUrl } from "@/lib/synapse/profile-media";
import { getProfileByUsername } from "@/lib/synapse/profiles";
import { getCurrentUser } from "@/lib/synapse/session";
import { generateMetadata as buildMetadata } from "@/utils";

interface Props {
    params: { username: string };
}

export const dynamic = "force-dynamic";

export const generateMetadata = async ({ params }: Props): Promise<Metadata> => {
    const profile = await getProfileByUsername(params.username).catch(() => null);
    const handle = profile?.username ?? params.username;

    return {
        ...buildMetadata({
            title: profile ? `${profile.display_name ?? handle} (@${handle}) on Synapse` : `@${handle} on Synapse`,
            description: profile?.bio ?? `See @${handle}'s profile on Synapse — the open-source social app for every device.`,
        }),
        robots: "index, follow",
    };
};

const ProfilePage = async ({ params }: Props) => {
    const profile = await getProfileByUsername(params.username);

    if (!profile) notFound();

    const user = await getCurrentUser();
    const isOwner = Boolean(user && user.id === profile.uid);
    const coverUrl = await resolveCoverUrl(profile.profile_cover_image);
    const posts: FeedEntry[] | { error: string } | null = user
        ? await getPostsByAuthor(profile.uid).catch((error: Error) => ({ error: error.message }))
        : null;

    return (
        <div className="space-y-4">
            <ProfileHeader
                profile={profile}
                coverUrl={coverUrl}
                action={
                    !isOwner && user ? (
                        <span className="rounded-full border border-neutral-700 px-4 py-2 text-xs text-neutral-300">
                            Follow is coming to web
                        </span>
                    ) : null
                }
            />

            {posts === null ? (
                <div className="rounded-2xl border border-dashed border-neutral-800 bg-neutral-950/40 p-8 text-center">
                    <p className="font-heading text-lg font-semibold">Posts need a Synapse account</p>
                    <p className="mx-auto mt-2 max-w-sm text-sm text-neutral-400">
                        Profiles are public, but the post list is only readable by signed-in members.
                    </p>
                    <Link
                        href={`/login?next=${encodeURIComponent(`/u/${profile.username}`)}`}
                        className={buttonVariants({ variant: "accent", size: "lg", className: "mt-5 rounded-lg" })}
                    >
                        Sign in
                    </Link>
                </div>
            ) : "error" in posts ? (
                <div className="rounded-2xl border border-red-500/30 bg-red-500/10 p-5 text-sm text-red-200">
                    Could not load this profile&apos;s posts: {posts.error}
                </div>
            ) : posts.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-neutral-800 bg-neutral-950/40 p-8 text-center text-sm text-neutral-400">
                    {profile.display_name ?? profile.username} hasn&apos;t posted yet.
                </div>
            ) : (
                posts.map((entry) => <PostCard key={entry.post.id} entry={{ ...entry, author: profile }} />)
            )}
        </div>
    );
};

export default ProfilePage;
