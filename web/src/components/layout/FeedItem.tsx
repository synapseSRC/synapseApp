import React from 'react';
import { PostCard } from './PostCard';
import { Post } from '@/types';

interface FeedItemProps {
  post: Post;
  onLike?: (postId: string) => void;
  onCommentClick?: (postId: string) => void;
  onShare?: (postId: string) => void;
  onBookmark?: (postId: string) => void;
}

export const FeedItem: React.FC<FeedItemProps> = (props) => {
  return <PostCard {...props} />;
};
