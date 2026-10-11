import { create } from 'zustand';
import type { User } from '@supabase/supabase-js';
import { supabase } from '@/lib/supabase';
import type { UserProfile } from '@/types';

type AuthLoadingAction = 'session' | 'google' | 'login' | 'signup' | 'reset' | 'recovery' | 'logout' | null;

interface AuthState {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  loadingAction: AuthLoadingAction;
  error: string | null;
  notice: string | null;
  setUser: (user: UserProfile | null) => void;
  clearMessages: () => void;
  signInWithGoogle: () => Promise<boolean>;
  login: (email: string, pass: string) => Promise<boolean>;
  signUp: (email: string, pass: string, username: string) => Promise<boolean>;
  requestPasswordReset: (email: string) => Promise<boolean>;
  updatePassword: (password: string) => Promise<boolean>;
  logout: () => Promise<void>;
  checkSession: () => Promise<void>;
}

const toProfile = (user: User): UserProfile => {
  const metadata = user.user_metadata ?? {};
  const emailName = user.email?.split('@')[0] || 'member';
  const username = String(metadata.username || emailName);
  return {
    id: user.id,
    username,
    fullName: String(metadata.full_name || metadata.display_name || metadata.name || username),
    avatarUrl: metadata.avatar_url || metadata.avatar || metadata.picture || undefined,
    bio: metadata.bio || undefined,
    followersCount: 0,
    followingCount: 0,
    postsCount: 0,
    isVerified: false,
    createdAt: user.created_at,
  };
};

const errorMessage = (error: unknown) => {
  const message = error instanceof Error ? error.message : 'Something went wrong. Please try again.';
  const normalized = message.toLowerCase();
  if (normalized.includes('invalid login credentials') || normalized.includes('invalid email or password')) {
    return 'Email or password is incorrect. Please try again.';
  }
  if (normalized.includes('user already registered') || normalized.includes('already been registered')) {
    return 'An account with this email already exists. Try signing in instead.';
  }
  if (normalized.includes('database error saving new user') || normalized.includes('users_username_lower_key')) {
    return 'That username may already be taken. Try another username.';
  }
  if (normalized.includes('email not confirmed')) {
    return 'Please confirm your email using the link we sent before signing in.';
  }
  if (normalized.includes('too many requests') || normalized.includes('rate limit')) {
    return 'Too many attempts. Please wait a moment and try again.';
  }
  if (normalized.includes('timeout') || normalized.includes('timed out')) {
    return 'The request took too long. Check your connection and try again.';
  }
  if (normalized.includes('password') && (normalized.includes('weak') || normalized.includes('characters'))) {
    return 'Choose a stronger password that meets the security requirements.';
  }
  return message;
};

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,
  loadingAction: 'session',
  error: null,
  notice: null,
  setUser: (user) => set({ user, isAuthenticated: Boolean(user), isLoading: false, loadingAction: null }),
  clearMessages: () => set({ error: null, notice: null }),

  signInWithGoogle: async () => {
    set({ isLoading: true, loadingAction: 'google', error: null, notice: null });
    try {
      const { error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: { redirectTo: window.location.origin },
      });
      if (error) throw error;
      set({ isLoading: false, loadingAction: null, error: null, notice: null });
      return true;
    } catch (error) {
      set({ error: errorMessage(error), isLoading: false, loadingAction: null });
      return false;
    }
  },

  login: async (email, password) => {
    set({ isLoading: true, loadingAction: 'login', error: null, notice: null });
    try {
      const { data, error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
      if (error) throw error;
      if (!data.user) throw new Error('Supabase did not return an authenticated user.');
      set({ user: toProfile(data.user), isAuthenticated: true, isLoading: false, loadingAction: null, error: null, notice: null });
      return true;
    } catch (error) {
      set({ error: errorMessage(error), isLoading: false, loadingAction: null });
      return false;
    }
  },

  signUp: async (email, password, username) => {
    set({ isLoading: true, loadingAction: 'signup', error: null, notice: null });
    try {
      const { data, error } = await supabase.auth.signUp({
        email: email.trim(),
        password,
        options: { data: { username: username.trim().toLowerCase() }, emailRedirectTo: window.location.origin },
      });
      if (error) throw error;
      if (data.session && data.user) {
        set({ user: toProfile(data.user), isAuthenticated: true, isLoading: false, loadingAction: null, error: null, notice: null });
      } else {
        set({ user: null, isAuthenticated: false, isLoading: false, loadingAction: null, error: null, notice: 'Check your inbox for a confirmation link sent to ' + email.trim() + '.' });
      }
      return true;
    } catch (error) {
      set({ error: errorMessage(error), isLoading: false, loadingAction: null });
      return false;
    }
  },

  requestPasswordReset: async (email) => {
    set({ isLoading: true, loadingAction: 'reset', error: null, notice: null });
    try {
      const { error } = await supabase.auth.resetPasswordForEmail(email.trim(), { redirectTo: window.location.origin + '/?mode=recovery' });
      if (error) throw error;
      set({ isLoading: false, loadingAction: null, error: null, notice: 'If an account exists for that address, a password reset link is on its way.' });
      return true;
    } catch (error) {
      set({ error: errorMessage(error), isLoading: false, loadingAction: null });
      return false;
    }
  },

  updatePassword: async (password) => {
    set({ isLoading: true, loadingAction: 'recovery', error: null, notice: null });
    try {
      const { data, error } = await supabase.auth.updateUser({ password });
      if (error) throw error;
      if (!data.user) throw new Error('Supabase did not return the updated user.');
      window.history.replaceState({}, '', window.location.pathname);
      set({ user: toProfile(data.user), isAuthenticated: true, isLoading: false, loadingAction: null, error: null, notice: 'Your password has been updated.' });
      return true;
    } catch (error) {
      set({ error: errorMessage(error), isLoading: false, loadingAction: null });
      return false;
    }
  },

  logout: async () => {
    set({ isLoading: true, loadingAction: 'logout', error: null, notice: null });
    try {
      const { error } = await supabase.auth.signOut();
      if (error) throw error;
      set({ user: null, isAuthenticated: false, isLoading: false, loadingAction: null, error: null, notice: null });
    } catch (error) {
      set({ error: errorMessage(error), isLoading: false, loadingAction: null });
    }
  },

  checkSession: async () => {
    set({ isLoading: true, loadingAction: 'session' });
    try {
      const { data, error } = await supabase.auth.getSession();
      if (error) throw error;
      const user = data.session?.user ?? null;
      set({ user: user ? toProfile(user) : null, isAuthenticated: Boolean(user), isLoading: false, loadingAction: null, error: null });
    } catch (error) {
      set({ user: null, isAuthenticated: false, isLoading: false, loadingAction: null, error: errorMessage(error) });
    }
  },
}));
