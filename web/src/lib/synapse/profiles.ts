import type { User } from "@supabase/supabase-js";

import { createSupabaseServerClient } from "@/lib/supabase/server";

import { ensureProfileRow } from "./auth";
import type { OwnProfileRow, ProfileRow } from "./types";

const OWN_PROFILE_FIELDS =
    "uid,username,nickname,display_name,bio,avatar,verify,is_private,followers_count,following_count,posts_count,profile_cover_image,join_date,created_at,website,location,pronouns,work,education,relationship_status";

export const getProfileByUsername = async (username: string): Promise<ProfileRow | null> => {
    const { data, error } = await createSupabaseServerClient()
        .from("public_profiles")
        .select("*")
        .eq("username", username.toLowerCase())
        .maybeSingle();

    if (error) throw new Error(`Profile lookup failed: ${error.message}`);

    return (data as ProfileRow | null) ?? null;
};

export const getProfileByUid = async (uid: string): Promise<ProfileRow | null> => {
    const { data, error } = await createSupabaseServerClient()
        .from("public_profiles")
        .select("*")
        .eq("uid", uid)
        .maybeSingle();

    if (error) throw new Error(`Profile lookup failed: ${error.message}`);

    return (data as ProfileRow | null) ?? null;
};

export const getOwnProfile = async (uid: string): Promise<OwnProfileRow | null> => {
    const { data, error } = await createSupabaseServerClient()
        .from("users")
        .select(OWN_PROFILE_FIELDS)
        .eq("uid", uid)
        .maybeSingle();

    if (error) throw new Error(`Profile lookup failed: ${error.message}`);

    return (data as OwnProfileRow | null) ?? null;
};

export const getOrCreateOwnProfile = async (user: User): Promise<OwnProfileRow | null> => {
    const existing = await getOwnProfile(user.id);

    if (existing) return existing;

    const ensured = await ensureProfileRow(createSupabaseServerClient(), user);

    return ensured.ok ? getOwnProfile(user.id) : null;
};
