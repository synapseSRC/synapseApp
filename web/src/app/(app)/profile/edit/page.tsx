import type { Metadata } from "next";
import Link from "next/link";
import { ArrowLeftIcon } from "lucide-react";

import { EditProfileForm } from "@/components/synapse/edit-profile-form";
import { toFormValues } from "@/lib/synapse/profile-fields";
import { resolveCoverUrl } from "@/lib/synapse/profile-media";
import { getOrCreateOwnProfile } from "@/lib/synapse/profiles";
import { requireCurrentUser } from "@/lib/synapse/session";
import { generateMetadata as buildMetadata } from "@/utils";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
    ...buildMetadata({ title: "Edit your profile on Synapse" }),
    robots: "noindex, nofollow",
};

const EditProfilePage = async () => {
    const user = await requireCurrentUser("/profile/edit");
    const profile = await getOrCreateOwnProfile(user);
    const coverUrl = profile ? await resolveCoverUrl(profile.profile_cover_image) : null;

    return (
        <div className="space-y-4">
            <div className="flex items-center gap-3">
                <Link
                    href="/profile"
                    aria-label="Back to profile"
                    className="grid size-9 shrink-0 place-items-center rounded-full border border-neutral-800 text-neutral-400 transition-colors hover:text-white"
                >
                    <ArrowLeftIcon className="size-4" />
                </Link>
                <div>
                    <h1 className="font-heading text-2xl font-semibold tracking-tight">Edit profile</h1>
                    <p className="text-sm text-neutral-400">Update how you appear across Synapse.</p>
                </div>
            </div>

            {profile ? (
                <EditProfileForm initial={toFormValues(profile)} coverUrl={coverUrl} />
            ) : (
                <div className="rounded-2xl border border-red-500/30 bg-red-500/10 p-5 text-sm text-red-200">
                    We couldn&apos;t load your profile.{" "}
                    <Link href="/profile" className="underline underline-offset-4">
                        Go back to your profile
                    </Link>
                </div>
            )}
        </div>
    );
};

export default EditProfilePage;
