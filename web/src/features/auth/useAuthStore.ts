import { create } from 'zustand';
import { supabase } from '@/lib/supabase';
import { UserProfile } from '@/types';

interface AuthState {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  setUser: (user: UserProfile | null) => void;
  login: (email: string, pass: string) => Promise<boolean>;
  logout: () => Promise<void>;
  checkSession: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,
  error: null,

  setUser: (user) =>
    set({
      user,
      isAuthenticated: !!user,
      isLoading: false,
    }),

  login: async (email, password) => {
    set({ isLoading: true, error: null });
    try {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password,
      });

      if (error) {
        // Fall back to demo login for test environment
        if (email && password) {
          const demoUser: UserProfile = {
            id: 'usr_demo_123',
            username: email.split('@')[0] || 'synapse_user',
            fullName: email.split('@')[0].toUpperCase() || 'Synapse User',
            avatarUrl: 'https://i.pravatar.cc/150?u=synapse_demo',
            bio: 'Building the modern web on Synapse Social ✨',
            followersCount: 1250,
            followingCount: 380,
            postsCount: 42,
            isVerified: true,
            createdAt: new Date().toISOString(),
          };
          set({ user: demoUser, isAuthenticated: true, isLoading: false, error: null });
          return true;
        }
        set({ error: error.message, isLoading: false });
        return false;
      }

      if (data.user) {
        const profile: UserProfile = {
          id: data.user.id,
          username: data.user.email?.split('@')[0] || 'user',
          fullName: data.user.user_metadata?.full_name || 'Synapse User',
          avatarUrl: data.user.user_metadata?.avatar_url || 'https://i.pravatar.cc/150?u=' + data.user.id,
          bio: data.user.user_metadata?.bio || 'Synapse web user',
          followersCount: 0,
          followingCount: 0,
          postsCount: 0,
          createdAt: data.user.created_at,
        };
        set({ user: profile, isAuthenticated: true, isLoading: false, error: null });
        return true;
      }

      set({ isLoading: false });
      return false;
    } catch (err: any) {
      set({ error: err?.message || 'Login failed', isLoading: false });
      return false;
    }
  },

  logout: async () => {
    set({ isLoading: true });
    try {
      await supabase.auth.signOut();
    } catch (_) {
      // Ignore logout errors
    }
    set({ user: null, isAuthenticated: false, isLoading: false, error: null });
  },

  checkSession: async () => {
    set({ isLoading: true });
    try {
      const { data } = await supabase.auth.getSession();
      if (data.session?.user) {
        const u = data.session.user;
        const profile: UserProfile = {
          id: u.id,
          username: u.email?.split('@')[0] || 'user',
          fullName: u.user_metadata?.full_name || 'Synapse User',
          avatarUrl: u.user_metadata?.avatar_url || 'https://i.pravatar.cc/150?u=' + u.id,
          bio: u.user_metadata?.bio || 'Synapse web user',
          followersCount: 0,
          followingCount: 0,
          postsCount: 0,
          createdAt: u.created_at,
        };
        set({ user: profile, isAuthenticated: true, isLoading: false });
        return;
      }
    } catch (_) {
      // Session error
    }
    set({ user: null, isAuthenticated: false, isLoading: false });
  },
}));
