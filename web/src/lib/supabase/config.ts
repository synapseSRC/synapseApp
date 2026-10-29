export interface SupabaseEnv {
    [key: string]: string | undefined;
    NEXT_PUBLIC_SUPABASE_URL?: string;
    NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY?: string;
    NEXT_PUBLIC_SUPABASE_ANON_KEY?: string;
}

const CONFIGURATION_ERROR =
    "Supabase is not configured. Set NEXT_PUBLIC_SUPABASE_URL and NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY (or NEXT_PUBLIC_SUPABASE_ANON_KEY) in .env.local.";

// Next.js only inlines direct process.env.NEXT_PUBLIC_* references into client bundles.
const PUBLIC_ENV: SupabaseEnv = {
    NEXT_PUBLIC_SUPABASE_URL: process.env.NEXT_PUBLIC_SUPABASE_URL,
    NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY: process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY,
    NEXT_PUBLIC_SUPABASE_ANON_KEY: process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY,
};

export const resolveSupabaseConfig = (env: SupabaseEnv = PUBLIC_ENV) => {
    const config = tryResolveSupabaseConfig(env);

    if (!config) throw new Error(CONFIGURATION_ERROR);

    return config;
};

export const tryResolveSupabaseConfig = (env: SupabaseEnv = PUBLIC_ENV) => {
    const url = env.NEXT_PUBLIC_SUPABASE_URL?.trim();
    const key = (env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY ?? env.NEXT_PUBLIC_SUPABASE_ANON_KEY)?.trim();

    return url && key ? { url, key } : null;
};

export const STORAGE_BUCKETS = {
    avatars: "avatars",
    covers: "covers",
    posts: "posts",
    chatMedia: "web-chat-media",
} as const;
