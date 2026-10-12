export interface SupabaseEnvironment {
  VITE_SUPABASE_URL?: string;
  VITE_SUPABASE_PUBLISHABLE_KEY?: string;
  VITE_SUPABASE_ANON_KEY?: string;
  NEXT_PUBLIC_SUPABASE_URL?: string;
  NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY?: string;
  NEXT_PUBLIC_SUPABASE_ANON_KEY?: string;
}

export interface SupabaseConfig {
  url: string;
  key: string;
}

export function resolveSupabaseConfig(environment: SupabaseEnvironment): SupabaseConfig {
  const url = environment.VITE_SUPABASE_URL || environment.NEXT_PUBLIC_SUPABASE_URL;
  const key =
    environment.VITE_SUPABASE_PUBLISHABLE_KEY ||
    environment.VITE_SUPABASE_ANON_KEY ||
    environment.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY ||
    environment.NEXT_PUBLIC_SUPABASE_ANON_KEY;

  if (!url || !key) {
    throw new Error(
      'Supabase is not configured. Set VITE_SUPABASE_URL and VITE_SUPABASE_PUBLISHABLE_KEY, or their NEXT_PUBLIC_SUPABASE_* equivalents.'
    );
  }

  return { url, key };
}
