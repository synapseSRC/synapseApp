import { NextResponse, type NextRequest } from "next/server";

import { createSupabaseServerClient } from "@/lib/supabase/server";
import { ensureProfileRow, safeNextPath } from "@/lib/synapse/auth";

const fail = (origin: string, message: string) =>
    NextResponse.redirect(`${origin}/login?error=${encodeURIComponent(message)}`);

export async function GET(request: NextRequest) {
    const { searchParams, origin } = request.nextUrl;
    const code = searchParams.get("code");
    const failure = searchParams.get("error_description") ?? searchParams.get("error");

    if (failure) return fail(origin, failure);

    if (!code) return fail(origin, "That sign-in link is incomplete or has expired. Please try again.");

    const supabase = createSupabaseServerClient();
    const { data, error } = await supabase.auth.exchangeCodeForSession(code);

    if (error || !data.user) return fail(origin, error?.message ?? "Sign-in failed. Please try again.");

    await ensureProfileRow(supabase, data.user);

    const destination = searchParams.get("type") === "recovery" ? "/reset-password" : safeNextPath(searchParams.get("next") ?? undefined);

    return NextResponse.redirect(`${origin}${destination}`);
}
