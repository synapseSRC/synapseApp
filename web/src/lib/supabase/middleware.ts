import { NextResponse, type NextRequest } from "next/server";
import { createServerClient } from "@supabase/ssr";

import { tryResolveSupabaseConfig } from "./config";

const PROTECTED_ROUTES = ["/home", "/inbox", "/notifications", "/search", "/settings", "/compose", "/profile"];
const AUTH_ROUTES = ["/login", "/signup", "/forgot-password", "/reset-password"];

const isUnder = (pathname: string, prefixes: readonly string[]) =>
    prefixes.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`));

const redirectTo = (request: NextRequest, pathname: string, search = "") => {
    const target = request.nextUrl.clone();
    target.pathname = pathname;
    target.search = search;
    return target;
};

export async function updateSession(request: NextRequest) {
    const config = tryResolveSupabaseConfig();

    // Marketing pages must keep serving when Supabase is unconfigured.
    if (!config) return NextResponse.next({ request });

    let response = NextResponse.next({ request });

    const supabase = createServerClient(config.url, config.key, {
        auth: { flowType: "pkce" },
        cookies: {
            getAll: () => request.cookies.getAll(),
            setAll: (cookiesToSet) => {
                for (const { name, value } of cookiesToSet) request.cookies.set(name, value);
                response = NextResponse.next({ request });
                for (const { name, value, options } of cookiesToSet) {
                    response.cookies.set(name, value, options);
                }
            },
        },
    });

    const { data } = await supabase.auth.getSession();
    const { pathname, search } = request.nextUrl;

    if (isUnder(pathname, AUTH_ROUTES)) {
        return data.session
            ? NextResponse.redirect(redirectTo(request, "/home"), { headers: response.headers })
            : response;
    }

    if (isUnder(pathname, PROTECTED_ROUTES) && !data.session) {
        return NextResponse.redirect(redirectTo(request, "/login", `?next=${encodeURIComponent(pathname + search)}`), {
            headers: response.headers,
        });
    }

    return response;
}
