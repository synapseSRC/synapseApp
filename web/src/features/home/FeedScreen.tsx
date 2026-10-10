import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Sparkles, Users, Plus } from 'lucide-react';
import { PostList } from '@/components/layout/PostList';
import { Button } from '@/components/ui/Button';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { supabase } from '@/lib/supabase';
import { Post } from '@/types';

const INITIAL_MOCK_POSTS: Post[] = [
  {
    id: 'post_1',
    userId: 'usr_2',
    user: {
      id: 'usr_2',
      username: 'sarah_tech',
      fullName: 'Sarah Jenkins',
      avatarUrl: 'https://i.pravatar.cc/150?u=sarah',
      bio: 'Frontend Architect | Design Systems & React',
      followersCount: 3420,
      followingCount: 512,
      postsCount: 128,
      isVerified: true,
      createdAt: '2024-01-15T10:00:00Z',
    },
    content: 'Just launched the new Synapse Web platform built with React 18, TypeScript, and Vite! 🚀 Responsive 3-column layout, dark theme, and ultra-fast real-time feed updates.',
    mediaUrls: ['https://picsum.photos/seed/synapse1/800/450'],
    likesCount: 142,
    commentsCount: 18,
    sharesCount: 9,
    isLiked: false,
    createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString(),
  },
  {
    id: 'post_2',
    userId: 'usr_3',
    user: {
      id: 'usr_3',
      username: 'marcus_code',
      fullName: 'Marcus Vance',
      avatarUrl: 'https://i.pravatar.cc/150?u=marcus',
      bio: 'Fullstack Dev & Open Source Contributor',
      followersCount: 890,
      followingCount: 230,
      postsCount: 54,
      isVerified: false,
      createdAt: '2024-02-01T12:00:00Z',
    },
    content: 'Kotlin Multiplatform + Web frontend working seamlessly together. Reusing domain models and business logic across Android, iOS, and Web is incredible for productivity.',
    likesCount: 88,
    commentsCount: 7,
    sharesCount: 3,
    isLiked: true,
    createdAt: new Date(Date.now() - 1000 * 60 * 180).toISOString(),
  },
];

interface FeedScreenProps {
  onOpenCreatePost?: () => void;
}

export const FeedScreen: React.FC<FeedScreenProps> = ({ onOpenCreatePost }) => {
  const [feedFilter, setFeedFilter] = useState<'forYou' | 'following'>('forYou');

  const { data: posts = INITIAL_MOCK_POSTS, isLoading } = useQuery<Post[]>({
    queryKey: ['feed', feedFilter],
    queryFn: async () => {
      try {
        const { data, error } = await supabase
          .from('posts')
          .select('*, user:users(*)')
          .order('created_at', { ascending: false })
          .limit(20);

        if (error || !data || data.length === 0) {
          return INITIAL_MOCK_POSTS;
        }

        return data.map((item: any) => ({
          id: item.id,
          userId: item.user_id,
          user: {
            id: item.user?.id || item.user_id,
            username: item.user?.username || 'user',
            fullName: item.user?.full_name || 'User',
            avatarUrl: item.user?.avatar_url,
            followersCount: item.user?.followers_count || 0,
            followingCount: item.user?.following_count || 0,
            postsCount: item.user?.posts_count || 0,
            createdAt: item.user?.created_at || item.created_at,
          },
          content: item.content || '',
          mediaUrls: item.media_urls || [],
          likesCount: item.likes_count || 0,
          commentsCount: item.comments_count || 0,
          sharesCount: item.shares_count || 0,
          createdAt: item.created_at,
        }));
      } catch (_) {
        return INITIAL_MOCK_POSTS;
      }
    },
  });

  const stories = [
    { name: 'Your Story', avatar: 'https://i.pravatar.cc/150?u=me', isUser: true },
    { name: 'Elena R.', avatar: 'https://i.pravatar.cc/150?u=elena' },
    { name: 'David C.', avatar: 'https://i.pravatar.cc/150?u=david' },
    { name: 'Aria S.', avatar: 'https://i.pravatar.cc/150?u=aria' },
    { name: 'Lucas K.', avatar: 'https://i.pravatar.cc/150?u=lucas' },
  ];

  return (
    <div className="space-y-6">
      {/* Feed Filters Header */}
      <div className="sticky top-0 z-20 bg-background/95 backdrop-blur border-b p-3 flex items-center justify-between">
        <div className="flex space-x-2">
          <button
            onClick={() => setFeedFilter('forYou')}
            className={`flex items-center space-x-1.5 px-4 py-2 rounded-full text-xs font-semibold cursor-pointer transition-all ${
              feedFilter === 'forYou'
                ? 'bg-primary-500 text-white shadow-sm'
                : 'text-muted-foreground hover:bg-muted'
            }`}
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>For You</span>
          </button>
          <button
            onClick={() => setFeedFilter('following')}
            className={`flex items-center space-x-1.5 px-4 py-2 rounded-full text-xs font-semibold cursor-pointer transition-all ${
              feedFilter === 'following'
                ? 'bg-primary-500 text-white shadow-sm'
                : 'text-muted-foreground hover:bg-muted'
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            <span>Following</span>
          </button>
        </div>

        {onOpenCreatePost && (
          <Button size="sm" onClick={onOpenCreatePost} className="rounded-full gap-1 text-xs px-3">
            <Plus className="w-4 h-4" />
            <span>Post</span>
          </Button>
        )}
      </div>

      {/* Stories Carousel */}
      <div className="flex space-x-4 overflow-x-auto pb-2 scrollbar-none px-1">
        {stories.map((story, i) => (
          <div key={i} className="flex flex-col items-center space-y-1 shrink-0 cursor-pointer group">
            <div
              className={`p-0.5 rounded-full ${
                story.isUser ? 'border-2 border-dashed border-primary-500' : 'bg-gradient-to-tr from-amber-500 via-primary-500 to-blue-600'
              }`}
            >
              <div className="bg-background p-0.5 rounded-full">
                <Avatar size="md" className="group-hover:scale-105 transition-transform">
                  <AvatarImage src={story.avatar} alt={story.name} />
                  <AvatarFallback>{story.name.slice(0, 2)}</AvatarFallback>
                </Avatar>
              </div>
            </div>
            <span className="text-[11px] font-medium text-muted-foreground truncate max-w-[64px]">
              {story.name}
            </span>
          </div>
        ))}
      </div>

      {/* Post Feed List */}
      <PostList posts={posts} isLoading={isLoading} />
    </div>
  );
};
