import { create } from 'zustand';
import { ThemeMode } from '@/types';

interface ThemeState {
  theme: ThemeMode;
  isDarkMode: boolean;
  setTheme: (theme: ThemeMode) => void;
  toggleTheme: () => void;
}

const getInitialTheme = (): ThemeMode => {
  if (typeof window === 'undefined') return 'system';
  const saved = localStorage.getItem('synapse_theme') as ThemeMode;
  if (saved && ['light', 'dark', 'system'].includes(saved)) {
    return saved;
  }
  return 'system';
};

const applyTheme = (mode: ThemeMode) => {
  if (typeof window === 'undefined') return false;
  const root = document.documentElement;
  const isDark =
    mode === 'dark' ||
    (mode === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);

  if (isDark) {
    root.classList.add('dark');
  } else {
    root.classList.remove('dark');
  }
  return isDark;
};

export const useThemeStore = create<ThemeState>((set, get) => {
  const initialTheme = getInitialTheme();
  const initialDark = applyTheme(initialTheme);

  return {
    theme: initialTheme,
    isDarkMode: initialDark,
    setTheme: (theme) => {
      localStorage.setItem('synapse_theme', theme);
      const isDark = applyTheme(theme);
      set({ theme, isDarkMode: isDark });
    },
    toggleTheme: () => {
      const current = get().theme;
      const nextTheme = current === 'dark' ? 'light' : 'dark';
      get().setTheme(nextTheme);
    },
  };
});
