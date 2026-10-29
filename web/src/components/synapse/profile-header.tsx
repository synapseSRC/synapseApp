import Image from "next/image";
import type { ReactNode } from "react";
import {
    BadgeCheckIcon,
    BriefcaseIcon,
    GraduationCapIcon,
    HeartIcon,
    LinkIcon,
    LockIcon,
    MapPinIcon,
    UserRoundIcon,
} from "lucide-react";

import { displayUrl, safeHttpUrl } from "@/lib/synapse/profile-fields";
import type { OwnProfileRow, ProfileRow } from "@/lib/synapse/types";

type HeaderProfile = ProfileRow &
    Partial<Pick<OwnProfileRow, "website" | "location" | "pronouns" | "work" | "education" | "relationship_status">>;

const Stat = ({ value, label }: { value: number | null; label: string }) => (
    <span className="text-sm text-neutral-400">
        <strong className="font-semibold text-white">{value ?? 0}</strong> {label}
    </span>
);

const Detail = ({ icon: Icon, label, children }: { icon: typeof LinkIcon; label: string; children: ReactNode }) => (
    <div className="flex min-w-0 items-start gap-2 text-sm text-neutral-400">
        <Icon className="mt-0.5 size-4 shrink-0 text-neutral-500" aria-hidden />
        <span className="sr-only">{label}:</span>
        <span className="min-w-0">{children}</span>
    </div>
);

export const ProfileHeader = ({
    profile,
    action,
    showDetails = false,
    coverUrl = null,
}: {
    profile: HeaderProfile;
    action?: ReactNode;
    showDetails?: boolean;
    coverUrl?: string | null;
}) => {
    const website = safeHttpUrl(profile.website);

    return (
        <section className="overflow-hidden rounded-2xl border border-neutral-800 bg-neutral-950/60 backdrop-blur-xl">
            {coverUrl ? (
                <div className="relative h-32 overflow-hidden sm:h-40">
                    <Image src={coverUrl} alt="" fill unoptimized className="object-cover" />
                </div>
            ) : (
                <div className="h-32 bg-gradient-to-r from-violet-700/60 via-neutral-900 to-cyan-600/50 sm:h-40" />
            )}

            <div className="p-5 pt-0">
                <div className="-mt-12 flex items-end justify-between gap-4">
                    {profile.avatar ? (
                        <Image
                            src={profile.avatar}
                            alt=""
                            width={96}
                            height={96}
                            unoptimized
                            className="size-20 rounded-full border-4 border-neutral-950 object-cover sm:size-24"
                        />
                    ) : (
                        <span className="grid size-20 place-items-center rounded-full border-4 border-neutral-950 bg-gradient-to-r from-violet-600 to-cyan-500 text-2xl font-semibold text-white sm:size-24">
                            {(profile.display_name ?? profile.username ?? "S").slice(0, 1).toUpperCase()}
                        </span>
                    )}
                    {action ? <div className="flex flex-wrap items-center justify-end gap-2">{action}</div> : null}
                </div>

                <div className="mt-4">
                    <div className="flex flex-wrap items-center gap-2">
                        <h1 className="font-heading text-2xl font-semibold tracking-tight">
                            {profile.display_name ?? profile.username}
                        </h1>
                        {profile.verify ? <BadgeCheckIcon className="size-5 text-cyan-400" aria-label="Verified" /> : null}
                        {profile.is_private ? (
                            <span className="flex items-center gap-1 text-xs text-neutral-400">
                                <LockIcon className="size-3.5" /> Private
                            </span>
                        ) : null}
                    </div>
                    <p className="mt-1 text-sm text-neutral-500">@{profile.username}</p>
                    {profile.bio ? (
                        <p className="mt-3 max-w-prose whitespace-pre-wrap text-sm text-neutral-300">{profile.bio}</p>
                    ) : null}
                    <div className="mt-4 flex items-center gap-5">
                        <Stat value={profile.posts_count} label="posts" />
                        <Stat value={profile.followers_count} label="followers" />
                        <Stat value={profile.following_count} label="following" />
                    </div>

                    {showDetails ? (
                        <div className="mt-4 grid gap-2 sm:grid-cols-2">
                            {profile.location ? (
                                <Detail icon={MapPinIcon} label="Location">
                                    {profile.location}
                                </Detail>
                            ) : null}
                            {website ? (
                                <Detail icon={LinkIcon} label="Website">
                                    <a
                                        href={website}
                                        target="_blank"
                                        rel="me noopener noreferrer nofollow ugc"
                                        className="truncate text-violet-400 underline-offset-4 hover:text-violet-300 hover:underline"
                                    >
                                        {displayUrl(website)}
                                    </a>
                                </Detail>
                            ) : null}
                            {profile.work ? (
                                <Detail icon={BriefcaseIcon} label="Work">
                                    {profile.work}
                                </Detail>
                            ) : null}
                            {profile.education ? (
                                <Detail icon={GraduationCapIcon} label="Education">
                                    {profile.education}
                                </Detail>
                            ) : null}
                            {profile.relationship_status ? (
                                <Detail icon={HeartIcon} label="Relationship">
                                    {profile.relationship_status}
                                </Detail>
                            ) : null}
                            {profile.pronouns ? (
                                <Detail icon={UserRoundIcon} label="Pronouns">
                                    {profile.pronouns}
                                </Detail>
                            ) : null}
                        </div>
                    ) : null}
                </div>
            </div>
        </section>
    );
};
