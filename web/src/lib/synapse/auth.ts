import type { SupabaseClient, User } from "@supabase/supabase-js";

export const FALLBACK_USERNAME = "member";

export const describeAuthError = (error: unknown): string => {
    const message = error instanceof Error ? error.message : "Something went wrong. Please try again.";
    const normalized = message.toLowerCase();

    if (normalized.includes("invalid login credentials") || normalized.includes("invalid email or password")) {
        return "Email or password is incorrect. Please try again.";
    }
    if (normalized.includes("user already registered") || normalized.includes("already been registered")) {
        return "An account with this email already exists. Try signing in instead.";
    }
    if (normalized.includes("database error saving new user") || normalized.includes("users_username_lower_key")) {
        return "That username may already be taken. Try another username.";
    }
    if (normalized.includes("email not confirmed")) {
        return "Please confirm your email using the link we sent before signing in.";
    }
    if (normalized.includes("too many requests") || normalized.includes("rate limit")) {
        return "Too many attempts. Please wait a moment and try again.";
    }
    if (normalized.includes("timeout") || normalized.includes("timed out")) {
        return "The request took too long. Check your connection and try again.";
    }
    if (normalized.includes("password") && (normalized.includes("weak") || normalized.includes("characters"))) {
        return "Choose a stronger password that meets the security requirements.";
    }

    return message;
};

export const safeNextPath = (value: string | string[] | undefined): string => {
    const path = Array.isArray(value) ? value[0] : value;
    if (!path || !path.startsWith("/") || path.startsWith("//")) return "/home";
    return path;
};

export const resolveUsername = (user: User): string => {
    const metadata = user.user_metadata as { username?: string };
    return metadata.username || user.email?.split("@")[0] || FALLBACK_USERNAME;
};

export const initialsAvatarUrl = (name: string): string => {
    const seed = name.trim().toLowerCase() || FALLBACK_USERNAME;
    return `https://api.dicebear.com/9.x/initials/svg?seed=${encodeURIComponent(seed)}`;
};

export const normalizeUsername = (value: string): string => value.trim().toLowerCase();

// Mirrors ensureProfileExists in the Android client (SupabaseAuthRepository.kt:121), so web
// accounts appear in the same directory as native ones. A pre-existing row is left untouched.
export const ensureProfileRow = async (
    client: SupabaseClient,
    user: User,
): Promise<{ ok: true } | { ok: false; message: string }> => {
    try {
        const { data, error: selectError } = await client
            .from("users")
            .select("uid")
            .eq("uid", user.id)
            .maybeSingle();

        if (selectError) return { ok: false, message: selectError.message };
        if (data) return { ok: true };

        const { error: insertError } = await client
            .from("users")
            .insert({ uid: user.id, username: resolveUsername(user) });

        if (insertError) return { ok: false, message: insertError.message };

        return { ok: true };
    } catch (error) {
        return { ok: false, message: error instanceof Error ? error.message : "Unknown error" };
    }
};
