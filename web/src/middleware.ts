import type { NextRequest } from "next/server";

import { updateSession } from "@/lib/supabase/middleware";

export default async function middleware(request: NextRequest) {
    return updateSession(request);
}

export const config = {
    // /.well-known/ must be served untouched: Google's Digital Asset Links lookup for passkeys
    // fails on any redirect or non-JSON response, and updateSession answers an anonymous request
    // with a 307 to the sign-in page.
    matcher: ["/((?!_next/static|_next/image|favicon.ico|icon.svg|apple-icon.png|opengraph-image.png|twitter-image.png|\\.well-known/|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)"],
};
