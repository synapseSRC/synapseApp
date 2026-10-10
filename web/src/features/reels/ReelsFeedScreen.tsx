import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Heart, MessageCircle, Share2, Music, Play, Pause } from 'lucide-react';
import { Reel } from '@/types';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';

const MOCK_REELS: Reel[] = [
  {
    id: 'reel_1',
    userId: 'usr_reel_1',
    user: {
      id: 'usr_reel_1',
      username: 'tech_vibes',
      fullName: 'Tech Vibes',
      avatarUrl: 'https://i.pravatar.cc/150?u=reel1',
      followersCount: 12400,
      followingCount: 150,
      postsCount: 88,
      createdAt: new Date().toISOString(),
    },
    videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    caption: 'Building smooth animations in React Web with Framer Motion ✨ #ui #react',
    likesCount: 1240,
    commentsCount: 86,
    sharesCount: 42,
    isLiked: false,
    createdAt: new Date().toISOString(),
  },
  {
    id: 'reel_2',
    userId: 'usr_reel_2',
    user: {
      id: 'usr_reel_2',
      username: 'code_flow',
      fullName: 'Code Flow',
      avatarUrl: 'https://i.pravatar.cc/150?u=reel2',
      followersCount: 45000,
      followingCount: 300,
      postsCount: 210,
      createdAt: new Date().toISOString(),
    },
    videoUrl: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    caption: 'Synapse Web responsive layout in action! 📱💻 #synapse #webdev',
    likesCount: 3510,
    commentsCount: 214,
    sharesCount: 110,
    isLiked: true,
    createdAt: new Date().toISOString(),
  },
];

export const ReelsFeedScreen: React.FC = () => {
  const [activeReelIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(true);
  const currentReel = MOCK_REELS[activeReelIndex];

  const togglePlay = () => {
    setIsPlaying(!isPlaying);
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-5rem)] p-2 sm:p-4">
      <div className="relative w-full max-w-sm h-[75vh] rounded-3xl overflow-hidden bg-black border border-border shadow-2xl flex flex-col justify-between">
        {/* HTML5 Video */}
        <video
          src={currentReel.videoUrl}
          className="absolute inset-0 w-full h-full object-cover cursor-pointer"
          loop
          autoPlay={isPlaying}
          muted
          onClick={togglePlay}
        />

        {/* Play/Pause Overlay Indicator */}
        {!isPlaying && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/30 pointer-events-none">
            <div className="w-16 h-16 rounded-full bg-white/20 backdrop-blur flex items-center justify-center text-white">
              <Play className="w-8 h-8 fill-white ml-1" />
            </div>
          </div>
        )}

        {/* Top Header Badge */}
        <div className="relative z-10 p-4 flex items-center justify-between bg-gradient-to-b from-black/60 to-transparent">
          <span className="text-white font-bold text-base tracking-wide flex items-center space-x-2">
            <span>Reels</span>
          </span>
          <Button size="icon" variant="ghost" className="text-white hover:bg-white/20" onClick={togglePlay}>
            {isPlaying ? <Pause className="w-5 h-5" /> : <Play className="w-5 h-5" />}
          </Button>
        </div>

        {/* Right Action Sidebar Overlay */}
        <div className="absolute right-3 bottom-16 z-20 flex flex-col items-center space-y-5">
          <motion.button
            whileTap={{ scale: 0.8 }}
            className="flex flex-col items-center space-y-1 text-white cursor-pointer"
          >
            <div className="p-3 rounded-full bg-black/40 backdrop-blur hover:bg-black/60 transition-colors">
              <Heart className={`w-6 h-6 ${currentReel.isLiked ? 'fill-red-500 text-red-500' : ''}`} />
            </div>
            <span className="text-xs font-semibold">{currentReel.likesCount}</span>
          </motion.button>

          <button className="flex flex-col items-center space-y-1 text-white cursor-pointer">
            <div className="p-3 rounded-full bg-black/40 backdrop-blur hover:bg-black/60 transition-colors">
              <MessageCircle className="w-6 h-6" />
            </div>
            <span className="text-xs font-semibold">{currentReel.commentsCount}</span>
          </button>

          <button className="flex flex-col items-center space-y-1 text-white cursor-pointer">
            <div className="p-3 rounded-full bg-black/40 backdrop-blur hover:bg-black/60 transition-colors">
              <Share2 className="w-6 h-6" />
            </div>
            <span className="text-xs font-semibold">{currentReel.sharesCount}</span>
          </button>
        </div>

        {/* Bottom User Profile & Caption Overlay */}
        <div className="relative z-10 p-4 pr-16 bg-gradient-to-t from-black/80 via-black/40 to-transparent space-y-3">
          <div className="flex items-center space-x-3">
            <Avatar size="sm">
              <AvatarImage src={currentReel.user.avatarUrl} alt={currentReel.user.fullName} />
              <AvatarFallback>{currentReel.user.fullName.slice(0, 2)}</AvatarFallback>
            </Avatar>
            <span className="text-white font-semibold text-sm">@{currentReel.user.username}</span>
            <Button size="sm" variant="outline" className="h-7 text-xs rounded-full bg-white/10 text-white border-white/30 hover:bg-white hover:text-black">
              Follow
            </Button>
          </div>

          <p className="text-white text-xs leading-relaxed line-clamp-2">{currentReel.caption}</p>

          <div className="flex items-center space-x-2 text-white/80 text-[11px]">
            <Music className="w-3.5 h-3.5 animate-spin" />
            <span className="truncate">Original Audio - {currentReel.user.fullName}</span>
          </div>
        </div>
      </div>
    </div>
  );
};
