import { STORAGE_BUCKETS } from "@/lib/supabase/config";
import { createSupabaseServerClient } from "@/lib/supabase/server";

const PRIVATE_COVER_REFERENCE = /^private-media:\/\/covers\/(.+)$/i;
const SUPABASE_COVER_URL = /\/storage\/v1\/object\/(?:public|authenticated|sign)\/covers\/([^?#]+)/i;

const coverStoragePath = (reference: string): string | null => {
    const privateMatch = reference.match(PRIVATE_COVER_REFERENCE);
    const urlMatch = reference.match(SUPABASE_COVER_URL);

    const rawPath = privateMatch ? privateMatch[1] : urlMatch ? decodeURIComponent(urlMatch[1]) : null;
    if (!rawPath) return null;

    // A reference is user-writable data, so normalise before it reaches the storage API: drop the
    // leading separator and refuse any "." / ".." segment that would climb out of the object's
    // own folder once Supabase resolves it.
    const path = rawPath.replace(/^\/+/, "");
    if (!path || path.split("/").some((segment) => segment === "." || segment === "..")) return null;

    return path;
};

/**
 * Turns a stored cover reference into a URL that a browser can render immediately.
 *
 * Private covers need a short-lived signed URL, and this function mints one for whatever path it
 * is handed. Because `users.profile_cover_image` is written by the user, an unchecked call is a
 * signing oracle: any member could point their own column at another member's object and have the
 * server sign it, including on the public `/u/[username]` page. Uploads always write
 * "<uid>/<file>", so when the caller knows the owner, require that prefix.
 */
export const resolveCoverUrl = async (
    reference: string | null | undefined,
    ownerUid?: string | null,
): Promise<string | null> => {
    if (!reference) return null;

    let path = coverStoragePath(reference);

    if (path && ownerUid) {
        const owner = ownerUid.trim().toLowerCase();
        if (!owner || !path.toLowerCase().startsWith(`${owner}/`)) path = null;
    }

    if (!path) return /^https?:\/\//i.test(reference) ? reference : null;

    const { data, error } = await createSupabaseServerClient()
        .storage.from(STORAGE_BUCKETS.covers)
        .createSignedUrl(path, 60 * 60);

    return error ? null : data.signedUrl;
};
