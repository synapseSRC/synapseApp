import React, { useState } from 'react';
import { Send } from 'lucide-react';
import { Post, Comment } from '@/types';
import { PostCard } from '@/components/layout/PostCard';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { formatDate } from '@/lib/utils';

interface PostDetailScreenProps {
  post: Post;
  comments?: Comment[];
  onAddComment?: (commentText: string) => void;
}

export const PostDetailScreen: React.FC<PostDetailScreenProps> = ({
  post,
  comments = [],
  onAddComment,
}) => {
  const [commentText, setCommentText] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim()) return;
    if (onAddComment) onAddComment(commentText.trim());
    setCommentText('');
  };

  return (
    <div className="space-y-6">
      <PostCard post={post} />

      {/* Add Comment Input Form */}
      <form onSubmit={handleSubmit} className="flex items-center space-x-2 p-3 border rounded-xl bg-card">
        <input
          type="text"
          value={commentText}
          onChange={(e) => setCommentText(e.target.value)}
          placeholder="Write a comment..."
          className="flex-1 bg-muted/50 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
        <Button size="sm" type="submit" disabled={!commentText.trim()} className="rounded-lg gap-1">
          <Send className="w-4 h-4" />
          <span>Reply</span>
        </Button>
      </form>

      {/* Comments List */}
      <div className="space-y-3">
        <h3 className="text-sm font-bold text-muted-foreground uppercase tracking-wider">
          Comments ({comments.length})
        </h3>

        {comments.length === 0 ? (
          <p className="text-xs text-muted-foreground italic py-4 text-center">No comments yet. Be the first to reply!</p>
        ) : (
          comments.map((comment) => (
            <div key={comment.id} className="flex space-x-3 p-3 border rounded-xl bg-card/60">
              <Avatar size="sm">
                <AvatarImage src={comment.user.avatarUrl} alt={comment.user.fullName} />
                <AvatarFallback>{comment.user.fullName.slice(0, 2)}</AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold">{comment.user.fullName}</span>
                  <span className="text-[10px] text-muted-foreground">{formatDate(comment.createdAt)}</span>
                </div>
                <p className="text-sm mt-1 text-foreground">{comment.content}</p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
