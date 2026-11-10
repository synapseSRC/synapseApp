import { NextResponse, type NextRequest } from "next/server";

import { createSupabaseServerClient } from "@/lib/supabase/server";
import { ensureProfileRow, safeAuthNotice, safeNextPath } from "@/lib/synapse/auth";

const fail = (origin: string, message: string) =>
    NextResponse.redirect(`${origin}/login?error=${encodeURIComponent(message)}`);

export async function GET(request: NextRequest) {
    const { searchParams, origin } = request.nextUrl;
    const code = searchParams.get("code");
    // Both parameters are attacker-controlled when a link is crafted, so they are mapped through
    // the allow-list in auth.ts rather than reflected onto /login.
    const failure = searchParams.get("error") ?? searchParams.get("error_description");

    if (failure) return fail(origin, safeAuthNotice(failure) ?? "We could not complete that sign-in. Please try again.");

    if (!code) return fail(origin, "That sign-in link is incomplete or has expired. Please try again.");

    const supabase = createSupabaseServerClient();
    const { data, error } = await supabase.auth.exchangeCodeForSession(code);

    if (error || !data.user) return fail(origin, "We could not complete that sign-in. Please try again.");

    await ensureProfileRow(supabase, data.user);

    const destination = searchParams.get("type") === "recovery" ? "/reset-password" : safeNextPath(searchParams.get("next") ?? undefined);

    return NextResponse.redirect(`${origin}${destination}`);
}
