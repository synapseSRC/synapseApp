import type { SupabaseClient } from "@supabase/supabase-js";

import { STORAGE_BUCKETS } from "@/lib/supabase/config";

export const MAX_PROFILE_IMAGE_BYTES = 5 * 1024 * 1024;

const EXTENSIONS: Record<string, string> = {
    "image/jpeg": "jpg",
    "image/png": "png",
    "image/webp": "webp",
    "image/gif": "gif",
};

export const validateProfileImage = (file: File): string | null => {
    if (!EXTENSIONS[file.type]) return "Choose a JPEG, PNG, WebP or GIF image.";
    if (file.size > MAX_PROFILE_IMAGE_BYTES) return "Images must be 5 MB or smaller.";

    return null;
};

export const uploadProfileImage = async (
    client: SupabaseClient,
    uid: string,
    kind: "avatar" | "cover",
    file: File,
): Promise<string> => {
    const path = `${uid}/${kind}-${Date.now()}.${EXTENSIONS[file.type] ?? "jpg"}`;
    const bucket = kind === "avatar" ? STORAGE_BUCKETS.avatars : STORAGE_BUCKETS.covers;

    const { error } = await client.storage
        .from(bucket)
        .upload(path, file, { contentType: file.type, cacheControl: "31536000", upsert: false });

    if (error) throw new Error(`Upload failed: ${error.message}`);

    if (kind === "cover") return `private-media://${STORAGE_BUCKETS.covers}/${path}`;

    return client.storage.from(bucket).getPublicUrl(path).data.publicUrl;
};
