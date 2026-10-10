import React, { useState } from 'react';
import { MainTab } from '@/types';
import { useAuthStore } from '@/features/auth/useAuthStore';
import { useThemeStore } from '@/app/useThemeStore';
import { LoginScreen } from '@/features/auth/LoginScreen';
import { Shell } from '@/components/layout/Shell';

// Lazy or direct feature imports
import { FeedScreen } from '@/features/home/FeedScreen';
import { ReelsFeedScreen } from '@/features/reels/ReelsFeedScreen';
import { SearchScreen } from '@/features/search/SearchScreen';
import { InboxScreen } from '@/features/inbox/InboxScreen';
import { ProfileScreen } from '@/features/profile/ProfileScreen';
import { SettingsScreen } from '@/features/settings/SettingsScreen';
import { CreatePostScreen } from '@/features/create-post/CreatePostScreen';

export const AppRouter: React.FC = () => {
  const { user, isAuthenticated, isLoading, logout } = useAuthStore();
  const { isDarkMode, toggleTheme } = useThemeStore();

  const [activeTab, setActiveTab] = useState<MainTab>('feed');
  const [isCreatePostOpen, setIsCreatePostOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center space-y-4">
          <div className="w-12 h-12 rounded-2xl bg-primary-500 flex items-center justify-center text-white font-bold text-2xl animate-bounce">
            S
          </div>
          <p className="text-sm text-muted-foreground font-medium">Loading Synapse...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <LoginScreen />;
  }

  const renderActiveScreen = () => {
    switch (activeTab) {
      case 'feed':
        return <FeedScreen onOpenCreatePost={() => setIsCreatePostOpen(true)} />;
      case 'reels':
        return <ReelsFeedScreen />;
      case 'search':
        return <SearchScreen />;
      case 'inbox':
        return <InboxScreen />;
      case 'profile':
        return <ProfileScreen user={user} onOpenSettings={() => setIsSettingsOpen(true)} />;
      default:
        return <FeedScreen onOpenCreatePost={() => setIsCreatePostOpen(true)} />;
    }
  };

  return (
    <Shell
      activeTab={activeTab}
      onTabChange={setActiveTab}
      onOpenCreatePost={() => setIsCreatePostOpen(true)}
      onOpenSettings={() => setIsSettingsOpen(true)}
      onLogout={logout}
      userAvatar={user.avatarUrl}
      userName={user.fullName}
      userHandle={`@${user.username}`}
      isDarkMode={isDarkMode}
      onToggleTheme={toggleTheme}
    >
      {renderActiveScreen()}

      {/* Modals */}
      {isCreatePostOpen && (
        <CreatePostScreen
          user={user}
          isOpen={isCreatePostOpen}
          onClose={() => setIsCreatePostOpen(false)}
        />
      )}

      {isSettingsOpen && (
        <SettingsScreen
          user={user}
          isOpen={isSettingsOpen}
          onClose={() => setIsSettingsOpen(false)}
          onLogout={logout}
        />
      )}
    </Shell>
  );
};
