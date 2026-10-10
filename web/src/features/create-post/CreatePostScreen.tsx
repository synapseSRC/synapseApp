import React, { useState } from 'react';
import { Image, Video, X, Sparkles } from 'lucide-react';
import { UserProfile } from '@/types';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/Dialog';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';

interface CreatePostScreenProps {
  user: UserProfile;
  isOpen: boolean;
  onClose: () => void;
  onPostCreated?: () => void;
}

export const CreatePostScreen: React.FC<CreatePostScreenProps> = ({
  user,
  isOpen,
  onClose,
  onPostCreated,
}) => {
  const [content, setContent] = useState('');
  const [mediaPreview, setMediaPreview] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const url = URL.createObjectURL(file);
      setMediaPreview(url);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim() && !mediaPreview) return;

    setIsSubmitting(true);
    setTimeout(() => {
      setIsSubmitting(false);
      setContent('');
      setMediaPreview(null);
      if (onPostCreated) onPostCreated();
      onClose();
    }, 600);
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-lg p-6">
        <DialogHeader>
          <DialogTitle className="text-lg font-bold flex items-center space-x-2">
            <Sparkles className="w-5 h-5 text-primary-500" />
            <span>Create New Post</span>
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4 pt-2">
          {/* User Header */}
          <div className="flex items-center space-x-3">
            <Avatar size="md">
              <AvatarImage src={user.avatarUrl} alt={user.fullName} />
              <AvatarFallback>{user.fullName.slice(0, 2)}</AvatarFallback>
            </Avatar>
            <div>
              <p className="text-sm font-semibold leading-none">{user.fullName}</p>
              <p className="text-xs text-muted-foreground mt-1">@{user.username}</p>
            </div>
          </div>

          {/* Post Content Input */}
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="What's on your mind? Share thoughts, links, or updates..."
            rows={4}
            className="w-full bg-muted/40 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none placeholder:text-muted-foreground border"
          />

          {/* Media Preview Box */}
          {mediaPreview && (
            <div className="relative rounded-xl overflow-hidden border max-h-60 bg-black/5">
              <img src={mediaPreview} alt="Preview" className="w-full h-full object-cover" />
              <button
                type="button"
                onClick={() => setMediaPreview(null)}
                className="absolute top-2 right-2 p-1.5 rounded-full bg-black/60 text-white hover:bg-black/80 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Media Attachment Actions */}
          <div className="flex items-center justify-between border-t pt-3">
            <div className="flex items-center space-x-2">
              <label className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg border text-xs font-medium cursor-pointer hover:bg-muted transition-colors">
                <Image className="w-4 h-4 text-primary-500" />
                <span>Photo</span>
                <input type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
              </label>

              <label className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg border text-xs font-medium cursor-pointer hover:bg-muted transition-colors">
                <Video className="w-4 h-4 text-blue-500" />
                <span>Video</span>
                <input type="file" accept="video/*" className="hidden" onChange={handleFileChange} />
              </label>
            </div>

            <Button
              type="submit"
              disabled={(!content.trim() && !mediaPreview) || isSubmitting}
              className="px-6 rounded-xl font-semibold shadow-md"
            >
              {isSubmitting ? 'Posting...' : 'Publish'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
