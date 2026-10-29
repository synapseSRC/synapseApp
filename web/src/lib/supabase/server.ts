import { cookies } from "next/headers";
import { createServerClient } from "@supabase/ssr";

import { resolveSupabaseConfig } from "./config";

export const createSupabaseServerClient = () => {
    const { url, key } = resolveSupabaseConfig();
    const cookieStore = cookies();

    return createServerClient(url, key, {
        auth: { flowType: "pkce" },
        cookies: {
            getAll: () => cookieStore.getAll(),
            setAll: (cookiesToSet) => {
                try {
                    for (const { name, value, options } of cookiesToSet) {
                        cookieStore.set(name, value, options);
                    }
                } catch {
                    // Called from a Server Component, which cannot write cookies.
                    // The middleware refreshes sessions instead.
                }
            },
        },
    });
};
