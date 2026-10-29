"use client";

import { createBrowserClient } from "@supabase/ssr";

import { resolveSupabaseConfig } from "./config";

let browserClient: ReturnType<typeof createBrowserClient> | null = null;

export const getSupabaseBrowserClient = () => {
    if (!browserClient) {
        const { url, key } = resolveSupabaseConfig();
        browserClient = createBrowserClient(url, key, {
            auth: { flowType: "pkce", detectSessionInUrl: false, autoRefreshToken: true, persistSession: true },
        });
    }

    return browserClient;
};
