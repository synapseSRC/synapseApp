import { redirect } from "next/navigation";
import type { User } from "@supabase/supabase-js";

import { createSupabaseServerClient } from "@/lib/supabase/server";

export const getCurrentUser = async (): Promise<User | null> => {
    const supabase = createSupabaseServerClient();
    const { data } = await supabase.auth.getUser();

    return data.user;
};

export const requireCurrentUser = async (next = "/home"): Promise<User> => {
    const user = await getCurrentUser();

    if (!user) {
        redirect(`/login?next=${encodeURIComponent(next)}`);
    }

    return user;
};
