export interface UserProfile {
  id: string;
  username: string;
  fullName: string;
  avatarUrl?: string;
  bio?: string;
  website?: string;
  followersCount: number;
  followingCount: number;
  postsCount: number;
  isFollowing?: boolean;
  isVerified?: boolean;
  createdAt: string;
}

export interface Post {
  id: string;
  userId: string;
  user: UserProfile;
  content: string;
  mediaUrls?: string[];
  likesCount: number;
  commentsCount: number;
  sharesCount: number;
  isLiked?: boolean;
  isBookmarked?: boolean;
  createdAt: string;
}

export interface Comment {
  id: string;
  postId: string;
  userId: string;
  user: UserProfile;
  content: string;
  likesCount: number;
  isLiked?: boolean;
  createdAt: string;
}

export interface Reel {
  id: string;
  userId: string;
  user: UserProfile;
  videoUrl: string;
  caption?: string;
  likesCount: number;
  commentsCount: number;
  sharesCount: number;
  isLiked?: boolean;
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  chatId: string;
  senderId: string;
  sender?: UserProfile;
  content: string;
  mediaUrl?: string;
  mediaType?: 'image' | 'video' | 'audio' | 'file';
  status: 'SENDING' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED';
  createdAt: string;
}

export interface ChatConversation {
  id: string;
  participant: UserProfile;
  lastMessage?: ChatMessage;
  unreadCount: number;
  updatedAt: string;
}

export interface NotificationItem {
  id: string;
  type: 'like' | 'comment' | 'follow' | 'mention';
  actor: UserProfile;
  post?: Post;
  read: boolean;
  createdAt: string;
}

export type ThemeMode = 'light' | 'dark' | 'system';
export type MainTab = 'feed' | 'reels' | 'search' | 'inbox' | 'profile';
