import Link from "next/link";
import Image from "next/image";
import { BadgeCheckIcon, HeartIcon, MapPinIcon, MessageSquareIcon, RepeatIcon } from "lucide-react";

import type { FeedEntry } from "@/lib/synapse/posts";

const hidden = (value: string | null) => String(value).toLowerCase() === "true";

const DIVISIONS: { amount: number; unit: Intl.RelativeTimeFormatUnit }[] = [
    { amount: 60, unit: "second" },
    { amount: 60, unit: "minute" },
    { amount: 24, unit: "hour" },
    { amount: 7, unit: "day" },
    { amount: 4.34524, unit: "week" },
    { amount: 12, unit: "month" },
    { amount: Number.POSITIVE_INFINITY, unit: "year" },
];

const postTime = (post: FeedEntry["post"]): number => {
    const millis = post.timestamp > 1e12 ? post.timestamp : post.timestamp * 1000;
    if (Number.isFinite(millis) && millis > 0) return millis;

    return post.created_at ? new Date(post.created_at).getTime() : Number.NaN;
};

const timeAgo = (post: FeedEntry["post"]): string => {
    const at = postTime(post);

    if (!Number.isFinite(at)) return "";

    const format = new Intl.RelativeTimeFormat("en", { numeric: "always" });
    let duration = (at - Date.now()) / 1000;

    for (const division of DIVISIONS) {
        if (Math.abs(duration) < division.amount) return format.format(Math.round(duration), division.unit);
        duration /= division.amount;
    }

    return "";
};

export const PostCard = ({ entry }: { entry: FeedEntry }) => {
    const { post, author, mediaUrls } = entry;
    const authorHref = author?.username ? `/u/${author.username}` : "#";
    const primaryImage = mediaUrls[0];

    return (
        <article className="rounded-2xl border border-neutral-800 bg-neutral-950/60 p-5 backdrop-blur-xl transition-colors hover:border-neutral-700">
            <div className="flex items-start gap-3">
                <Link href={authorHref} className="shrink-0" aria-label={author?.username ?? "author"}>
                    {author?.avatar ? (
                        <Image
                            src={author.avatar}
                            alt=""
                            width={44}
                            height={44}
                            unoptimized
                            className="size-11 rounded-full border border-neutral-800 object-cover"
                        />
                    ) : (
                        <span className="grid size-11 place-items-center rounded-full bg-gradient-to-r from-violet-600 to-cyan-500 text-sm font-semibold text-white">
                            {(author?.display_name ?? author?.username ?? "S").slice(0, 1).toUpperCase()}
                        </span>
                    )}
                </Link>

                <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                        <Link href={authorHref} className="truncate text-sm font-semibold text-white hover:underline">
                            {author?.display_name ?? author?.username ?? "Synapse member"}
                        </Link>
                        {author?.verify ? <BadgeCheckIcon className="size-4 text-cyan-400" aria-label="Verified" /> : null}
                        {author?.username ? <span className="truncate text-sm text-neutral-500">@{author.username}</span> : null}
                        <span className="text-xs text-neutral-500">{timeAgo(post)}</span>
                    </div>

                    {post.post_text ? (
                        <p className="mt-2 whitespace-pre-wrap break-words text-[15px] leading-relaxed text-neutral-200">{post.post_text}</p>
                    ) : null}
                </div>
            </div>

            {primaryImage ? (
                <Image
                    src={primaryImage}
                    alt=""
                    width={640}
                    height={360}
                    unoptimized
                    className="mt-4 max-h-[26rem] w-full rounded-xl border border-neutral-800 object-cover"
                />
            ) : null}

            {post.location_name ? (
                <p className="mt-3 flex items-center gap-1.5 text-xs text-neutral-500">
                    <MapPinIcon className="size-3.5" />
                    {post.location_name}
                </p>
            ) : null}

            <div className="mt-4 flex items-center gap-6 text-xs text-neutral-500">
                {!hidden(post.post_hide_like_count) ? (
                    <span className="flex items-center gap-1.5">
                        <HeartIcon className="size-4" />
                        {post.likes_count ?? 0}
                    </span>
                ) : null}
                {!hidden(post.post_hide_comments_count) ? (
                    <span className="flex items-center gap-1.5">
                        <MessageSquareIcon className="size-4" />
                        {post.comments_count ?? 0}
                    </span>
                ) : null}
                <span className="flex items-center gap-1.5">
                    <RepeatIcon className="size-4" />
                    {post.reshares_count ?? 0}
                </span>
            </div>
        </article>
    );
};
