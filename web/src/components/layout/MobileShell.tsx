import React from 'react';
import { Home, Video, Search, MessageSquare, User, PlusSquare, Sun, Moon } from 'lucide-react';
import { MainTab } from '@/types';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';

interface MobileShellProps {
  activeTab: MainTab;
  onTabChange: (tab: MainTab) => void;
  onOpenCreatePost?: () => void;
  userAvatar?: string;
  userName?: string;
  isDarkMode: boolean;
  onToggleTheme: () => void;
  children: React.ReactNode;
}

export const MobileShell: React.FC<MobileShellProps> = ({
  activeTab,
  onTabChange,
  onOpenCreatePost,
  userAvatar,
  userName,
  isDarkMode,
  onToggleTheme,
  children,
}) => {
  const navItems = [
    { id: 'feed' as MainTab, label: 'Feed', icon: Home },
    { id: 'reels' as MainTab, label: 'Reels', icon: Video },
    { id: 'search' as MainTab, label: 'Search', icon: Search },
    { id: 'inbox' as MainTab, label: 'Inbox', icon: MessageSquare },
    { id: 'profile' as MainTab, label: 'Profile', icon: User },
  ];

  return (
    <div className="flex flex-col min-h-screen bg-background sm:hidden">
      {/* Mobile Top Header */}
      <header className="sticky top-0 z-40 flex items-center justify-between h-14 px-4 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 rounded-full bg-primary-500 flex items-center justify-center text-white font-bold text-lg">
            S
          </div>
          <span className="font-bold text-lg tracking-tight bg-gradient-to-r from-primary-500 to-blue-600 bg-clip-text text-transparent">
            Synapse
          </span>
        </div>

        <div className="flex items-center space-x-2">
          {onOpenCreatePost && (
            <Button size="icon" variant="ghost" onClick={onOpenCreatePost} aria-label="Create Post">
              <PlusSquare className="w-5 h-5" />
            </Button>
          )}
          <Button size="icon" variant="ghost" onClick={onToggleTheme} aria-label="Toggle Theme">
            {isDarkMode ? <Sun className="w-5 h-5 text-amber-400" /> : <Moon className="w-5 h-5" />}
          </Button>
          <div onClick={() => onTabChange('profile')} className="cursor-pointer">
            <Avatar size="sm">
              <AvatarImage src={userAvatar} alt={userName || 'User'} />
              <AvatarFallback>{userName?.slice(0, 2) || 'US'}</AvatarFallback>
            </Avatar>
          </div>
        </div>
      </header>

      {/* Main Screen Content */}
      <main className="flex-1 pb-16 overflow-y-auto">{children}</main>

      {/* Mobile Bottom Navigation Bar */}
      <nav className="fixed bottom-0 left-0 right-0 z-40 h-16 border-t bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 flex items-center justify-around px-2">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onTabChange(item.id)}
              className={`flex flex-col items-center justify-center w-12 h-12 rounded-xl transition-all cursor-pointer ${
                isActive ? 'text-primary-500 font-semibold scale-105' : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              <Icon className={`w-5 h-5 ${isActive ? 'stroke-[2.5px]' : 'stroke-[1.75px]'}`} />
              <span className="text-[10px] mt-0.5">{item.label}</span>
            </button>
          );
        })}
      </nav>
    </div>
  );
};
