/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SUPABASE_URL: string;
  readonly VITE_SUPABASE_PUBLISHABLE_KEY: string;
  readonly VITE_SUPABASE_ANON_KEY?: string;
  readonly NEXT_PUBLIC_SUPABASE_URL?: string;
  readonly NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY?: string;
  readonly NEXT_PUBLIC_SUPABASE_ANON_KEY?: string;
  readonly VITE_AVATARS_BUCKET?: string;
  readonly VITE_CHAT_MEDIA_BUCKET?: string;
  readonly VITE_POSTS_MEDIA_BUCKET?: string;
}

interface ImportMeta { readonly env: ImportMetaEnv; }
