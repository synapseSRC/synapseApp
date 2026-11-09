import { STORAGE_BUCKETS } from "@/lib/supabase/config";
import { createSupabaseServerClient } from "@/lib/supabase/server";

const PRIVATE_COVER_REFERENCE = /^private-media:\/\/covers\/(.+)$/i;
const SUPABASE_COVER_URL = /\/storage\/v1\/object\/(?:public|authenticated|sign)\/covers\/([^?#]+)/i;

const coverStoragePath = (reference: string): string | null => {
    const privateMatch = reference.match(PRIVATE_COVER_REFERENCE);
    if (privateMatch) return privateMatch[1];

    const urlMatch = reference.match(SUPABASE_COVER_URL);
    if (urlMatch) return decodeURIComponent(urlMatch[1]);

    return null;
};

export const resolveCoverUrl = async (reference: string | null | undefined): Promise<string | null> => {
    if (!reference) return null;

    const path = coverStoragePath(reference);

    if (!path) return /^https?:\/\//i.test(reference) ? reference : null;

    const { data, error } = await createSupabaseServerClient()
        .storage.from(STORAGE_BUCKETS.covers)
        .createSignedUrl(path, 60 * 60);

    return error ? null : data.signedUrl;
};
