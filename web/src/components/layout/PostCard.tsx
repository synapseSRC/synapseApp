import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Heart, MessageCircle, Share2, Bookmark, MoreHorizontal } from 'lucide-react';
import { Post } from '@/types';
import { formatDate, formatCompactNumber } from '@/lib/utils';
import { pulseHeart } from '@/lib/animations';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Card, CardHeader, CardContent, CardFooter } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';

interface PostCardProps {
  post: Post;
  onLike?: (postId: string) => void;
  onCommentClick?: (postId: string) => void;
  onShare?: (postId: string) => void;
  onBookmark?: (postId: string) => void;
}

export const PostCard: React.FC<PostCardProps> = ({
  post,
  onLike,
  onCommentClick,
  onShare,
  onBookmark,
}) => {
  const [isLiked, setIsLiked] = useState(post.isLiked || false);
  const [likesCount, setLikesCount] = useState(post.likesCount || 0);
  const [isBookmarked, setIsBookmarked] = useState(post.isBookmarked || false);

  const handleLike = () => {
    setIsLiked(!isLiked);
    setLikesCount(isLiked ? likesCount - 1 : likesCount + 1);
    if (onLike) onLike(post.id);
  };

  const handleBookmark = () => {
    setIsBookmarked(!isBookmarked);
    if (onBookmark) onBookmark(post.id);
  };

  return (
    <Card className="mb-4 overflow-hidden border border-border/80 shadow-sm hover:border-border transition-colors">
      {/* Header */}
      <CardHeader className="flex flex-row items-center justify-between p-4 pb-2">
        <div className="flex items-center space-x-3">
          <Avatar size="md">
            <AvatarImage src={post.user?.avatarUrl} alt={post.user?.fullName || 'User'} />
            <AvatarFallback>{post.user?.fullName?.slice(0, 2) || 'US'}</AvatarFallback>
          </Avatar>
          <div>
            <div className="flex items-center space-x-1">
              <span className="font-semibold text-sm leading-none hover:underline cursor-pointer">
                {post.user?.fullName || 'Anonymous'}
              </span>
              {post.user?.isVerified && (
                <span className="text-primary-500 text-xs" title="Verified">
                  ✓
                </span>
              )}
            </div>
            <div className="flex items-center space-x-2 text-xs text-muted-foreground mt-1">
              <span>@{post.user?.username || 'user'}</span>
              <span>•</span>
              <span>{formatDate(post.createdAt)}</span>
            </div>
          </div>
        </div>
        <Button size="icon" variant="ghost" className="h-8 w-8 text-muted-foreground">
          <MoreHorizontal className="w-4 h-4" />
        </Button>
      </CardHeader>

      {/* Content */}
      <CardContent className="p-4 pt-2">
        <p className="text-sm sm:text-base leading-relaxed whitespace-pre-line">{post.content}</p>

        {/* Media Attachments */}
        {post.mediaUrls && post.mediaUrls.length > 0 && (
          <div className="mt-3 rounded-xl overflow-hidden border bg-black/5">
            {post.mediaUrls.length === 1 ? (
              <img
                src={post.mediaUrls[0]}
                alt="Post content"
                className="w-full max-h-96 object-cover"
                loading="lazy"
              />
            ) : (
              <div className="grid grid-cols-2 gap-1">
                {post.mediaUrls.slice(0, 4).map((url, idx) => (
                  <img
                    key={idx}
                    src={url}
                    alt={`Post attachment ${idx + 1}`}
                    className="w-full h-48 object-cover"
                    loading="lazy"
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </CardContent>

      {/* Actions Footer */}
      <CardFooter className="flex items-center justify-between p-4 pt-0 border-t border-border/40">
        <div className="flex items-center space-x-4 pt-3">
          {/* Like Button */}
          <motion.button
            variants={pulseHeart}
            initial="initial"
            whileHover="hover"
            whileTap="tap"
            onClick={handleLike}
            className={`flex items-center space-x-1.5 text-xs font-medium cursor-pointer transition-colors ${
              isLiked ? 'text-red-500 font-semibold' : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            <Heart className={`w-5 h-5 ${isLiked ? 'fill-red-500 text-red-500' : ''}`} />
            <span>{formatCompactNumber(likesCount)}</span>
          </motion.button>

          {/* Comment Button */}
          <button
            onClick={() => onCommentClick && onCommentClick(post.id)}
            className="flex items-center space-x-1.5 text-xs font-medium text-muted-foreground hover:text-foreground cursor-pointer transition-colors"
          >
            <MessageCircle className="w-5 h-5" />
            <span>{formatCompactNumber(post.commentsCount || 0)}</span>
          </button>

          {/* Share Button */}
          <button
            onClick={() => onShare && onShare(post.id)}
            className="flex items-center space-x-1.5 text-xs font-medium text-muted-foreground hover:text-foreground cursor-pointer transition-colors"
          >
            <Share2 className="w-5 h-5" />
            <span>{formatCompactNumber(post.sharesCount || 0)}</span>
          </button>
        </div>

        {/* Bookmark Button */}
        <button
          onClick={handleBookmark}
          className={`text-muted-foreground hover:text-foreground cursor-pointer transition-colors pt-3 ${
            isBookmarked ? 'text-primary-500' : ''
          }`}
        >
          <Bookmark className={`w-5 h-5 ${isBookmarked ? 'fill-primary-500 text-primary-500' : ''}`} />
        </button>
      </CardFooter>
    </Card>
  );
};
