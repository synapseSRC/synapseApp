import React, { useState } from 'react';
import { Settings, Calendar, Link as LinkIcon, Grid, Bookmark, Heart } from 'lucide-react';
import { UserProfile } from '@/types';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';

interface ProfileScreenProps {
  user: UserProfile;
  onOpenSettings?: () => void;
}

export const ProfileScreen: React.FC<ProfileScreenProps> = ({ user, onOpenSettings }) => {
  const [activeTab, setActiveTab] = useState<'posts' | 'saved' | 'liked'>('posts');

  const mockGridPosts = [
    'https://picsum.photos/seed/grid1/400/400',
    'https://picsum.photos/seed/grid2/400/400',
    'https://picsum.photos/seed/grid3/400/400',
    'https://picsum.photos/seed/grid4/400/400',
    'https://picsum.photos/seed/grid5/400/400',
    'https://picsum.photos/seed/grid6/400/400',
  ];

  return (
    <div className="space-y-6">
      {/* Cover Banner */}
      <div className="relative h-44 sm:h-56 rounded-2xl overflow-hidden bg-gradient-to-r from-primary-600 via-blue-500 to-indigo-600 shadow-md">
        {onOpenSettings && (
          <Button
            size="icon"
            variant="secondary"
            onClick={onOpenSettings}
            className="absolute top-3 right-3 rounded-full bg-black/40 backdrop-blur text-white border border-white/20 hover:bg-black/60"
            title="Settings"
          >
            <Settings className="w-5 h-5" />
          </Button>
        )}
      </div>

      {/* User Info Header */}
      <div className="relative px-4 sm:px-6 -mt-16 sm:-mt-20 space-y-4">
        <div className="flex justify-between items-end">
          <Avatar className="h-24 w-24 sm:h-32 sm:w-32 border-4 border-background shadow-xl">
            <AvatarImage src={user.avatarUrl} alt={user.fullName} />
            <AvatarFallback>{user.fullName.slice(0, 2)}</AvatarFallback>
          </Avatar>

          <div className="flex space-x-2">
            <Button variant="outline" className="rounded-full text-xs font-semibold px-4">
              Edit Profile
            </Button>
            {onOpenSettings && (
              <Button size="icon" variant="outline" onClick={onOpenSettings} className="rounded-full sm:hidden">
                <Settings className="w-4 h-4" />
              </Button>
            )}
          </div>
        </div>

        {/* User Bio Details */}
        <div className="space-y-2">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold flex items-center space-x-1.5">
              <span>{user.fullName}</span>
              {user.isVerified && <span className="text-primary-500 text-sm">✓</span>}
            </h1>
            <p className="text-xs sm:text-sm text-muted-foreground">@{user.username}</p>
          </div>

          <p className="text-sm leading-relaxed max-w-xl">{user.bio || 'Building the future of social apps on Synapse ✨'}</p>

          <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground pt-1">
            {user.website && (
              <a href={user.website} target="_blank" rel="noreferrer" className="flex items-center space-x-1 text-primary-500 hover:underline">
                <LinkIcon className="w-3.5 h-3.5" />
                <span>{user.website.replace('https://', '')}</span>
              </a>
            )}
            <div className="flex items-center space-x-1">
              <Calendar className="w-3.5 h-3.5" />
              <span>Joined January 2024</span>
            </div>
          </div>

          {/* Follower Stats */}
          <div className="flex items-center space-x-6 pt-2 text-sm">
            <div>
              <span className="font-bold text-foreground">{user.postsCount || 42}</span>{' '}
              <span className="text-muted-foreground text-xs">Posts</span>
            </div>
            <div>
              <span className="font-bold text-foreground">{user.followersCount || 1250}</span>{' '}
              <span className="text-muted-foreground text-xs">Followers</span>
            </div>
            <div>
              <span className="font-bold text-foreground">{user.followingCount || 380}</span>{' '}
              <span className="text-muted-foreground text-xs">Following</span>
            </div>
          </div>
        </div>
      </div>

      {/* Grid Tabs */}
      <div className="border-t pt-4">
        <div className="flex justify-around border-b pb-2">
          <button
            onClick={() => setActiveTab('posts')}
            className={`flex items-center space-x-2 text-xs font-semibold pb-2 border-b-2 cursor-pointer transition-colors ${
              activeTab === 'posts'
                ? 'border-primary-500 text-primary-500'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
          >
            <Grid className="w-4 h-4" />
            <span>POSTS</span>
          </button>
          <button
            onClick={() => setActiveTab('saved')}
            className={`flex items-center space-x-2 text-xs font-semibold pb-2 border-b-2 cursor-pointer transition-colors ${
              activeTab === 'saved'
                ? 'border-primary-500 text-primary-500'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
          >
            <Bookmark className="w-4 h-4" />
            <span>SAVED</span>
          </button>
          <button
            onClick={() => setActiveTab('liked')}
            className={`flex items-center space-x-2 text-xs font-semibold pb-2 border-b-2 cursor-pointer transition-colors ${
              activeTab === 'liked'
                ? 'border-primary-500 text-primary-500'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
          >
            <Heart className="w-4 h-4" />
            <span>LIKED</span>
          </button>
        </div>

        {/* Media Grid Gallery */}
        <div className="grid grid-cols-3 gap-2 mt-4">
          {mockGridPosts.map((url, idx) => (
            <Card key={idx} className="aspect-square rounded-xl overflow-hidden border cursor-pointer group">
              <img
                src={url}
                alt={`Grid post ${idx + 1}`}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
              />
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
};
