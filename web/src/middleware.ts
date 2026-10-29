import type { NextRequest } from "next/server";

import { updateSession } from "@/lib/supabase/middleware";

export default async function middleware(request: NextRequest) {
    return updateSession(request);
}

export const config = {
    matcher: ["/((?!_next/static|_next/image|favicon.ico|icon.svg|apple-icon.png|opengraph-image.png|twitter-image.png|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)"],
};
