import React from 'react';
import { Home, Video, Search, MessageSquare, User, PlusSquare, Sun, Moon, Settings, LogOut } from 'lucide-react';
import { MainTab } from '@/types';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';

interface TabletShellProps {
  activeTab: MainTab;
  onTabChange: (tab: MainTab) => void;
  onOpenCreatePost?: () => void;
  onOpenSettings?: () => void;
  onLogout?: () => void;
  userAvatar?: string;
  userName?: string;
  isDarkMode: boolean;
  onToggleTheme: () => void;
  children: React.ReactNode;
}

export const TabletShell: React.FC<TabletShellProps> = ({
  activeTab,
  onTabChange,
  onOpenCreatePost,
  onOpenSettings,
  onLogout,
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
    <div className="hidden sm:flex lg:hidden min-h-screen bg-background">
      {/* Permanent Left Navigation Rail */}
      <aside className="w-20 border-r bg-card flex flex-col items-center justify-between py-6 sticky top-0 h-screen z-30 shrink-0">
        <div className="flex flex-col items-center space-y-8 w-full">
          {/* Logo */}
          <div className="w-10 h-10 rounded-2xl bg-primary-500 flex items-center justify-center text-white font-bold text-xl shadow-md">
            S
          </div>

          {/* Navigation Items */}
          <nav className="flex flex-col space-y-4 w-full px-2">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => onTabChange(item.id)}
                  title={item.label}
                  className={`flex flex-col items-center justify-center py-3 rounded-xl transition-all cursor-pointer ${
                    isActive
                      ? 'bg-primary-500/10 text-primary-500 font-medium'
                      : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                  }`}
                >
                  <Icon className={`w-6 h-6 ${isActive ? 'stroke-[2.5px]' : 'stroke-[1.75px]'}`} />
                  <span className="text-[10px] mt-1">{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        {/* Bottom Actions */}
        <div className="flex flex-col items-center space-y-4 w-full px-2">
          {onOpenCreatePost && (
            <Button
              size="icon"
              className="rounded-full bg-primary-500 hover:bg-primary-600 text-white shadow-md"
              onClick={onOpenCreatePost}
              title="Create Post"
            >
              <PlusSquare className="w-5 h-5" />
            </Button>
          )}

          <Button size="icon" variant="ghost" onClick={onToggleTheme} title="Toggle Theme">
            {isDarkMode ? <Sun className="w-5 h-5 text-amber-400" /> : <Moon className="w-5 h-5" />}
          </Button>

          {onOpenSettings && (
            <Button size="icon" variant="ghost" onClick={onOpenSettings} title="Settings">
              <Settings className="w-5 h-5" />
            </Button>
          )}

          {onLogout && (
            <Button size="icon" variant="ghost" onClick={onLogout} title="Logout" className="text-destructive">
              <LogOut className="w-5 h-5" />
            </Button>
          )}

          <div onClick={() => onTabChange('profile')} className="cursor-pointer pt-2">
            <Avatar size="md">
              <AvatarImage src={userAvatar} alt={userName || 'User'} />
              <AvatarFallback>{userName?.slice(0, 2) || 'US'}</AvatarFallback>
            </Avatar>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto">{children}</main>
    </div>
  );
};
