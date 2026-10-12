import { describe, expect, it } from 'vitest';
import { resolveSupabaseConfig } from './supabaseConfig';

describe('resolveSupabaseConfig', () => {
  it('prefers Vite-prefixed configuration when it is present', () => {
    expect(
      resolveSupabaseConfig({
        VITE_SUPABASE_URL: 'https://vite-project.supabase.co',
        VITE_SUPABASE_PUBLISHABLE_KEY: 'vite-publishable-key',
        NEXT_PUBLIC_SUPABASE_URL: 'https://next-project.supabase.co',
        NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY: 'next-publishable-key',
      })
    ).toEqual({ url: 'https://vite-project.supabase.co', key: 'vite-publishable-key' });
  });

  it('accepts NEXT_PUBLIC settings used by the production Vercel project', () => {
    expect(
      resolveSupabaseConfig({
        NEXT_PUBLIC_SUPABASE_URL: 'https://project.supabase.co',
        NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY: 'publishable-key',
      })
    ).toEqual({ url: 'https://project.supabase.co', key: 'publishable-key' });
  });

  it('supports the legacy anon-key name under either public prefix', () => {
    expect(
      resolveSupabaseConfig({
        NEXT_PUBLIC_SUPABASE_URL: 'https://project.supabase.co',
        NEXT_PUBLIC_SUPABASE_ANON_KEY: 'anon-key',
      })
    ).toEqual({ url: 'https://project.supabase.co', key: 'anon-key' });
  });

  it('fails clearly when public client configuration is missing', () => {
    expect(() => resolveSupabaseConfig({})).toThrow(/Supabase is not configured/);
  });
});
