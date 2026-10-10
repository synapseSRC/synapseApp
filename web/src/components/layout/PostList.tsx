import React from 'react';
import { motion } from 'framer-motion';
import { Post } from '@/types';
import { PostCard } from './PostCard';
import { staggerContainer, staggerItem } from '@/lib/animations';

interface PostListProps {
  posts: Post[];
  isLoading?: boolean;
  onLikePost?: (postId: string) => void;
  onCommentPost?: (postId: string) => void;
  onSharePost?: (postId: string) => void;
  onBookmarkPost?: (postId: string) => void;
}

export const PostList: React.FC<PostListProps> = ({
  posts,
  isLoading = false,
  onLikePost,
  onCommentPost,
  onSharePost,
  onBookmarkPost,
}) => {
  if (isLoading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3].map((i) => (
          <div key={i} className="rounded-xl border p-4 space-y-3 animate-pulse bg-card/40">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-full bg-muted" />
              <div className="space-y-1 flex-1">
                <div className="h-4 w-32 bg-muted rounded" />
                <div className="h-3 w-20 bg-muted rounded" />
              </div>
            </div>
            <div className="h-16 bg-muted rounded-lg w-full" />
            <div className="h-8 bg-muted/60 rounded-lg w-full" />
          </div>
        ))}
      </div>
    );
  }

  if (!posts || posts.length === 0) {
    return (
      <div className="text-center py-12 px-4 border rounded-2xl bg-card/30">
        <p className="text-base font-semibold text-foreground">No posts yet</p>
        <p className="text-xs text-muted-foreground mt-1">
          Follow creators or share your thoughts to get started!
        </p>
      </div>
    );
  }

  return (
    <motion.div
      variants={staggerContainer}
      initial="initial"
      animate="animate"
      className="space-y-4"
    >
      {posts.map((post) => (
        <motion.div key={post.id} variants={staggerItem}>
          <PostCard
            post={post}
            onLike={onLikePost}
            onCommentClick={onCommentPost}
            onShare={onSharePost}
            onBookmark={onBookmarkPost}
          />
        </motion.div>
      ))}
    </motion.div>
  );
};
