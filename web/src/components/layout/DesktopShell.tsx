import React from 'react';
import { Home, Video, Search, MessageSquare, User, PlusSquare, Sun, Moon, Settings, LogOut, TrendingUp, Sparkles } from 'lucide-react';
import { MainTab } from '@/types';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';

interface DesktopShellProps {
  activeTab: MainTab;
  onTabChange: (tab: MainTab) => void;
  onOpenCreatePost?: () => void;
  onOpenSettings?: () => void;
  onLogout?: () => void;
  userAvatar?: string;
  userName?: string;
  userHandle?: string;
  isDarkMode: boolean;
  onToggleTheme: () => void;
  children: React.ReactNode;
}

export const DesktopShell: React.FC<DesktopShellProps> = ({
  activeTab,
  onTabChange,
  onOpenCreatePost,
  onOpenSettings,
  onLogout,
  userAvatar,
  userName,
  userHandle,
  isDarkMode,
  onToggleTheme,
  children,
}) => {
  const navItems = [
    { id: 'feed' as MainTab, label: 'Feed', icon: Home },
    { id: 'reels' as MainTab, label: 'Reels', icon: Video },
    { id: 'search' as MainTab, label: 'Explore & Search', icon: Search },
    { id: 'inbox' as MainTab, label: 'Messages', icon: MessageSquare },
    { id: 'profile' as MainTab, label: 'Profile', icon: User },
  ];

  const trendingTopics = [
    { tag: '#SynapseWeb', posts: '12.4k posts' },
    { tag: '#React18', posts: '8.1k posts' },
    { tag: '#TypeScript', posts: '15.9k posts' },
    { tag: '#TailwindCSS', posts: '6.3k posts' },
  ];

  const suggestedUsers = [
    { name: 'Alex Rivera', handle: '@arivera', avatar: 'https://i.pravatar.cc/150?u=1' },
    { name: 'Elena Rostova', handle: '@elena_r', avatar: 'https://i.pravatar.cc/150?u=2' },
    { name: 'David Chen', handle: '@dchen_dev', avatar: 'https://i.pravatar.cc/150?u=3' },
  ];

  return (
    <div className="hidden lg:flex min-h-screen bg-background">
      {/* 3-Column Layout Container */}
      <div className="flex w-full max-w-7xl mx-auto px-4 gap-6">
        {/* Left Column: Navigation Sidebar */}
        <aside className="w-64 border-r bg-card/50 flex flex-col justify-between py-6 sticky top-0 h-screen z-30 shrink-0 pr-4">
          <div className="flex flex-col space-y-6">
            {/* Synapse Brand */}
            <div className="flex items-center space-x-3 px-3">
              <div className="w-10 h-10 rounded-2xl bg-primary-500 flex items-center justify-center text-white font-bold text-xl shadow-md">
                S
              </div>
              <div>
                <h1 className="font-bold text-xl tracking-tight bg-gradient-to-r from-primary-500 to-blue-600 bg-clip-text text-transparent">
                  Synapse
                </h1>
                <p className="text-xs text-muted-foreground">Social Network</p>
              </div>
            </div>

            {/* Navigation List */}
            <nav className="flex flex-col space-y-2">
              {navItems.map((item) => {
                const Icon = item.icon;
                const isActive = activeTab === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => onTabChange(item.id)}
                    className={`flex items-center space-x-3 px-4 py-3 rounded-xl transition-all cursor-pointer font-medium text-sm ${
                      isActive
                        ? 'bg-primary-500 text-white shadow-sm'
                        : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                    }`}
                  >
                    <Icon className={`w-5 h-5 ${isActive ? 'stroke-[2.5px]' : 'stroke-[1.75px]'}`} />
                    <span>{item.label}</span>
                  </button>
                );
              })}
            </nav>

            {/* Create Post Button */}
            {onOpenCreatePost && (
              <Button
                onClick={onOpenCreatePost}
                className="w-full py-6 rounded-xl bg-primary-500 hover:bg-primary-600 text-white font-semibold text-base shadow-md flex items-center justify-center space-x-2"
              >
                <PlusSquare className="w-5 h-5" />
                <span>Create Post</span>
              </Button>
            )}
          </div>

          {/* User Profile & Footer Controls */}
          <div className="flex flex-col space-y-4 pt-4 border-t">
            <div
              onClick={() => onTabChange('profile')}
              className="flex items-center space-x-3 p-2 rounded-xl hover:bg-muted cursor-pointer transition-colors"
            >
              <Avatar size="md">
                <AvatarImage src={userAvatar} alt={userName || 'User'} />
                <AvatarFallback>{userName?.slice(0, 2) || 'US'}</AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold truncate">{userName || 'User'}</p>
                <p className="text-xs text-muted-foreground truncate">{userHandle || '@user'}</p>
              </div>
            </div>

            <div className="flex items-center justify-between px-2">
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
            </div>
          </div>
        </aside>

        {/* Center Column: Main Content */}
        <main className="flex-1 border-r min-w-0 py-6 pr-6 max-w-2xl">{children}</main>

        {/* Right Column: Trending & Suggestions Panel */}
        <aside className="w-80 sticky top-0 h-screen py-6 overflow-y-auto shrink-0 space-y-6">
          {/* Trending Panel */}
          <Card className="bg-card/60 backdrop-blur">
            <CardHeader className="pb-2 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold flex items-center space-x-2">
                <TrendingUp className="w-4 h-4 text-primary-500" />
                <span>Trending Topics</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 pt-2">
              {trendingTopics.map((topic, index) => (
                <div key={index} className="flex justify-between items-center group cursor-pointer">
                  <div>
                    <p className="text-sm font-semibold group-hover:text-primary-500 transition-colors">
                      {topic.tag}
                    </p>
                    <p className="text-xs text-muted-foreground">{topic.posts}</p>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          {/* Who to Follow Suggestions */}
          <Card className="bg-card/60 backdrop-blur">
            <CardHeader className="pb-2 flex flex-row items-center justify-between">
              <CardTitle className="text-base font-bold flex items-center space-x-2">
                <Sparkles className="w-4 h-4 text-primary-500" />
                <span>Suggested Creators</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 pt-2">
              {suggestedUsers.map((user, index) => (
                <div key={index} className="flex items-center justify-between">
                  <div className="flex items-center space-x-3 min-w-0">
                    <Avatar size="sm">
                      <AvatarImage src={user.avatar} alt={user.name} />
                      <AvatarFallback>{user.name.slice(0, 2)}</AvatarFallback>
                    </Avatar>
                    <div className="min-w-0">
                      <p className="text-sm font-semibold truncate">{user.name}</p>
                      <p className="text-xs text-muted-foreground truncate">{user.handle}</p>
                    </div>
                  </div>
                  <Button size="sm" variant="outline" className="rounded-full h-8 text-xs px-3">
                    Follow
                  </Button>
                </div>
              ))}
            </CardContent>
          </Card>
        </aside>
      </div>
    </div>
  );
};
