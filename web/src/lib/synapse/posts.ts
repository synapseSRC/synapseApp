import { createSupabaseServerClient } from "@/lib/supabase/server";

import type { MediaItem, PostRow, ProfileRow } from "./types";

// Field list, filter and ordering mirror the working client in synapseApp/web (HomeScreen.tsx:49).
const POST_FIELDS =
    "id,author_uid,post_text,post_image,media_items,post_type,timestamp,publish_date,created_at,likes_count,comments_count,reshares_count,is_deleted,has_poll,poll_question,poll_options,poll_end_time,youtube_url,location_name,metadata,post_hide_like_count,post_hide_comments_count,post_disable_comments,post_visibility,in_reply_to_post_id,root_post_id,reply_to_usernames,quoted_post_id,is_quote,is_edited,edited_at";

const PROFILE_FIELDS = "uid,username,display_name,avatar,verify,bio,followers_count,following_count,posts_count";
const NOT_DELETED = "is_deleted.is.null,is_deleted.eq.false";

export interface FeedEntry {
    post: PostRow;
    author: ProfileRow | null;
    mediaUrls: string[];
}

const isAbsoluteUrl = (value: string) => /^https?:\/\//i.test(value);

const collectPaths = (posts: PostRow[]): string[] => {
    const paths: string[] = [];

    const add = (value: string | null | undefined) => {
        if (value && !isAbsoluteUrl(value) && !paths.includes(value)) paths.push(value);
    };

    for (const post of posts) {
        add(post.post_image);

        for (const item of post.media_items ?? []) {
            add(item?.url);
        }
    }

    return paths;
};

const unique = (values: string[]): string[] => values.filter((value, index, all) => all.indexOf(value) === index);

const signPaths = async (paths: string[]): Promise<Map<string, string>> => {
    if (paths.length === 0) return new Map();

    const { data, error } = await createSupabaseServerClient().storage.from("posts").createSignedUrls(paths, 3600);

    if (error) return new Map();

    const signed = new Map<string, string>();

    for (const entry of data ?? []) {
        if (entry.path && entry.signedUrl) signed.set(entry.path, entry.signedUrl);
    }

    return signed;
};

export const getProfilesByUids = async (uids: string[]): Promise<Map<string, ProfileRow>> => {
    if (uids.length === 0) return new Map();

    const { data, error } = await createSupabaseServerClient()
        .from("users")
        .select(PROFILE_FIELDS)
        .in("uid", uids);

    if (error) throw new Error(`Author lookup failed: ${error.message}`);

    return new Map(((data ?? []) as ProfileRow[]).map((profile) => [profile.uid, profile]));
};

export const getFeed = async ({
    offset = 0,
    limit = 20,
    authorIds,
}: {
    offset?: number;
    limit?: number;
    authorIds?: string[];
} = {}): Promise<FeedEntry[]> => {
    const supabase = createSupabaseServerClient();

    let query = supabase
        .from("posts")
        .select(POST_FIELDS)
        .or(NOT_DELETED)
        .order("timestamp", { ascending: false })
        .range(offset, offset + limit - 1);

    if (authorIds && authorIds.length > 0) query = query.in("author_uid", authorIds);

    const { data, error } = await query;

    if (error) throw new Error(`Feed query failed: ${error.message}`);

    const posts = (data ?? []) as PostRow[];
    const [profiles, signed] = await Promise.all([
        getProfilesByUids(unique(posts.map((post) => post.author_uid))),
        signPaths(collectPaths(posts)),
    ]);

    return posts.map((post) => {
        const mediaUrls = [post.post_image, ...(post.media_items ?? []).map((item: MediaItem) => item?.url)]
            .filter((value): value is string => Boolean(value))
            .map((value) => (isAbsoluteUrl(value) ? value : (signed.get(value) ?? "")))
            .filter(Boolean);

        return { post, author: profiles.get(post.author_uid) ?? null, mediaUrls };
    });
};

export const getPostsByAuthor = async (uid: string, limit = 60): Promise<FeedEntry[]> => {
    const { data, error } = await createSupabaseServerClient()
        .from("posts")
        .select(POST_FIELDS)
        .eq("author_uid", uid)
        .or(NOT_DELETED)
        .order("timestamp", { ascending: false })
        .limit(limit);

    if (error) throw new Error(`Profile posts query failed: ${error.message}`);

    const posts = (data ?? []) as PostRow[];
    const signed = await signPaths(collectPaths(posts));

    return posts.map((post) => ({
        post,
        author: null,
        mediaUrls: [post.post_image, ...(post.media_items ?? []).map((item: MediaItem) => item?.url)]
            .filter((value): value is string => Boolean(value))
            .map((value) => (isAbsoluteUrl(value) ? value : (signed.get(value) ?? "")))
            .filter(Boolean),
    }));
};
