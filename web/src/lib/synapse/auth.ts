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

// `?error=` / `error_description` reach the sign-in page straight from the query string, so any
// text a visitor puts in a link would be rendered inside our own alert. Map the known
// Supabase/GoTrue wording onto fixed copy and fall back to a generic line instead of echoing it.
const AUTH_NOTICES: ReadonlyArray<readonly [RegExp, string]> = [
    [/expired|invalid.*(link|token|grant)|otp_expired/i, "That sign-in link has expired. Request a new one and try again."],
    [/access_denied|cancel(l)?ed/i, "Sign-in was cancelled before it completed."],
    [/email.*not.*confirmed|email_not_confirmed/i, "Confirm your email address, then try signing in again."],
    [/too many|rate.?limit/i, "Too many attempts. Please wait a moment and try again."],
    [/server error|unexpected_failure/i, "The sign-in service is unavailable right now. Please try again shortly."],
];

export const safeAuthNotice = (value: string | string[] | undefined): string | null => {
    const raw = Array.isArray(value) ? value[0] : value;
    if (!raw) return null;

    return AUTH_NOTICES.find(([pattern]) => pattern.test(raw))?.[1] ?? "We could not complete that sign-in. Please try again.";
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

// PNG, not SVG: Android decodes notification large icons through BitmapFactory, which cannot
// rasterize SVG, so an SVG avatar URL silently drops the picture from push notifications.
export const initialsAvatarUrl = (name: string): string => {
    const seed = name.trim().toLowerCase() || FALLBACK_USERNAME;
    return `https://api.dicebear.com/9.x/initials/png?seed=${encodeURIComponent(seed)}`;
};

export const normalizeUsername = (value: string): string => value.trim().toLowerCase();

const GOOGLE_AVATAR_HOST_SUFFIX = "googleusercontent.com";
const GENERATED_AVATAR_HOST = "api.dicebear.com";

const hostOf = (url: string): string => {
    const authority = url.substring(url.indexOf("://") + 3).split(/[/?#]/)[0] ?? "";
    const hostAndPort = authority.split("@").slice(-1)[0] ?? "";

    return hostAndPort.split(":")[0] ?? "";
};

/** Absolute http(s) URL with a host — anything else (blank, relative path, other scheme) is no picture. */
export const isRemoteImageUrl = (value: string | null | undefined): boolean => {
    const url = value?.trim() ?? "";
    if (!url.startsWith("http://") && !url.startsWith("https://")) return false;

    const host = hostOf(url);
    return host.includes(".");
};

/** Google serves account photos from `googleusercontent.com`. */
export const isGoogleAvatarUrl = (value: string | null | undefined): boolean => {
    const host = hostOf(value?.trim() ?? "");
    return host === GOOGLE_AVATAR_HOST_SUFFIX || host.endsWith(`.${GOOGLE_AVATAR_HOST_SUFFIX}`);
};

/** DiceBear initials avatars are Synapse-generated placeholders, not a picture the user picked. */
export const isGeneratedAvatarUrl = (value: string | null | undefined): boolean =>
    hostOf(value?.trim() ?? "") === GENERATED_AVATAR_HOST;

/**
 * Picture a Google account shares with Supabase. GoTrue copies the provider profile into the user
 * metadata as `picture`, and mirrors it to `avatar_url` — the key an email sign-up also writes —
 * so both are read, in that order, and only Google-hosted images are accepted.
 */
export const googleAvatarUrl = (user: User): string | null => {
    const metadata = (user.user_metadata ?? {}) as Record<string, unknown>;

    for (const key of ["picture", "avatar_url"]) {
        const value = metadata[key];
        if (typeof value !== "string") continue;

        const url = value.trim();
        if (isRemoteImageUrl(url) && isGoogleAvatarUrl(url)) return url;
    }

    return null;
};

/**
 * Picture the profile should hold once [provided] is known, or null when nothing must change:
 * the profile may already hold it, or it may hold the user's own upload, which always wins — a
 * later Google sign-in must never overwrite a custom picture. Placeholders (a stale provider photo
 * or a generated initials avatar) are replaced instead.
 */
export const resolveStoredAvatar = (current: string | null, provided: string | null): string | null => {
    const incoming = provided?.trim() ?? "";
    if (!isRemoteImageUrl(incoming)) return null;

    const stored = current?.trim() ?? "";
    if (!stored) return incoming;
    if (stored === incoming) return null;

    return isGoogleAvatarUrl(stored) || isGeneratedAvatarUrl(stored) ? incoming : null;
};

// Mirrors ensureProfileExists in the Android client (SupabaseAuthRepository.kt:121), so web
// accounts appear in the same directory as native ones. A pre-existing row keeps its own data —
// only the Google account picture is refreshed, and only while the stored one is still a
// placeholder, so a picture the user uploaded is never replaced by a later sign-in.
export const ensureProfileRow = async (
    client: SupabaseClient,
    user: User,
): Promise<{ ok: true } | { ok: false; message: string }> => {
    try {
        const providerAvatar = googleAvatarUrl(user);

        const { data, error: selectError } = await client
            .from("users")
            .select("uid,avatar")
            .eq("uid", user.id)
            .maybeSingle();

        if (selectError) return { ok: false, message: selectError.message };

        if (data) {
            const currentAvatar = (data as { avatar: string | null }).avatar;
            const avatar = resolveStoredAvatar(currentAvatar, providerAvatar);

            if (avatar) {
                const { error: updateError } = await client
                    .from("users")
                    .update({ avatar })
                    .eq("uid", user.id);

                if (updateError) return { ok: false, message: updateError.message };
            }

            return { ok: true };
        }

        const { error: insertError } = await client
            .from("users")
            .insert({ uid: user.id, username: resolveUsername(user), ...(providerAvatar ? { avatar: providerAvatar } : {}) });

        if (insertError) return { ok: false, message: insertError.message };

        return { ok: true };
    } catch (error) {
        return { ok: false, message: error instanceof Error ? error.message : "Unknown error" };
    }
};
